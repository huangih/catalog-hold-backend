package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import tw.com.hyweb.cathold.backend.redis.service.VBookingService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemsService;
import tw.com.hyweb.cathold.backend.redis.service.VMarcCallVolumeService;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.BookingAvailation;
import tw.com.hyweb.cathold.model.BookingComment;
import tw.com.hyweb.cathold.model.BookingExpandDuedate;
import tw.com.hyweb.cathold.model.BookingHistory;
import tw.com.hyweb.cathold.model.Intransit;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.view.BookingHistoryView;
import tw.com.hyweb.cathold.model.view.BookingView;
import tw.com.hyweb.cathold.model.view.IntransitView;

@RequiredArgsConstructor
public class BookingViewServiceImpl implements BookingViewService {

	private static final List<Phase> AVAIL_PHASES = Arrays.asList(Phase.AVAILABLE, Phase.A01_ORDER);

	private static final String USER_ID = "userId";
	
	private static final String BOOKING_ID = "bookingId";
	
	private final VBookingService vBookingService;

	private final VHoldItemService vHoldItemService;

	private final VHoldItemsService vHoldItemsService;

	private final VMarcCallVolumeService vMarcCallVolumeService;

	private final ItemSiteDefService itemSiteDefService;

	private final R2dbcEntityOperations calVolTemplate;

	private Flux<BookingView> getReaderBookingViews(int readerId, int skip, int take, Boolean isAvailation,
			boolean containCopy) {
		return this.calVolTemplate.select(query(where(USER_ID).is(readerId)), Booking.class)
				.filter(bi -> this
						.filterBookingAvailOrCopy(isAvailation, containCopy, "C".equals(bi.getType()), bi.getPhase()))
				.sort(this::sortBooking).skip(skip).take(take).zipWith(Flux.range(1, take))
				.flatMap(tuple2 -> Mono.just(tuple2).publishOn(Schedulers.parallel())
						.flatMap(tup2 -> this.convert2BookingView(tup2.getT1()).map(bv -> Tuples.of(bv, tup2.getT2()))))
				.sort(Comparator.comparing(Tuple2::getT2)).map(Tuple2::getT1);
	}

	private boolean filterBookingAvailOrCopy(Boolean isAvailation, boolean containCopy, boolean isCopyType,
			Phase phase) {
		if (AVAIL_PHASES.contains(phase))
			return isAvailation == null || isAvailation;
		else {
			if (isCopyType)
				return containCopy;
			return isAvailation == null || !isAvailation;
		}
	}

	private int sortBooking(Booking b1, Booking b2) {
		List<Phase> sortPhases = new ArrayList<>();
		sortPhases.addAll(Arrays.asList(Phase.TRANSIT_B, Phase.WAIT_ANNEX));
		sortPhases.addAll(AVAIL_PHASES);
		Phase phase1 = b1.getPhase();
		Phase phase2 = b2.getPhase();
		if (AVAIL_PHASES.contains(phase1))
			return AVAIL_PHASES.contains(phase2) ? b1.getDueDate().compareTo(b2.getDueDate()) : -1;
		if (Phase.TRANSIT_B == phase1)
			return this.sortTransitBooking(b1, b2);
		if (Phase.WAIT_ANNEX == phase1) {
			if (phase1.equals(phase2))
				return b1.getAvailableDate().compareTo(b2.getAvailableDate());
			return AVAIL_PHASES.contains(phase2) || Phase.TRANSIT_B == phase2 ? 1 : -1;
		}
		return sortPhases.contains(phase2) ? 1 : b1.getPlaceDate().compareTo(b2.getPlaceDate());
	}

	private int sortTransitBooking(Booking b1, Booking b2) {
		Phase phase1 = b1.getPhase();
		Phase phase2 = b2.getPhase();
		if (phase1.equals(phase2)) {
			if (b1.getTransitDate() == null)
				return -1;
			if (b2.getTransitDate() == null)
				return 1;
			return b1.getTransitDate().compareTo(b2.getTransitDate());
		}
		return AVAIL_PHASES.contains(phase2) ? 1 : -1;
	}

	@Override
	public Flux<BookingView> getAllBookingViewsByReaderId(int readerId) {
		return this.getReaderBookingViews(readerId, 0, 999, null, false);
	}

	@Override
	/* 查詢讀者預約，若到館可借則包含copy預約 */
	public Flux<BookingView> getBookingViewsByReaderId(int readerId, boolean isAvailation) {
		return this.getReaderBookingViews(readerId, 0, 999, isAvailation, false);
	}

	@Override
	public Mono<BookingView> convert2BookingView(Booking booking) {
		BookingView bookingView = new BookingView(booking);
		return Mono.justOrEmpty(booking.getType()).filter(type -> !type.equals("T"))
				.flatMap(type -> this.vHoldItemService.getVHoldItemById(booking.getItemId())
						.map(VHoldItem::getCallVolId))
				.defaultIfEmpty(booking.getItemId())
				.flatMap(cvId -> this.vMarcCallVolumeService.getMarcVolumeByCallVolId(cvId)
						.zipWith(this.vMarcCallVolumeService.getHotTypeByCallVolId(cvId), (mcv, b) -> {
							bookingView.setMarcVolume(mcv);
							bookingView.setHotCallvol(b);
							return booking;
						}))
				.flatMap(bi -> this.calVolTemplate.selectOne(query(where("id").is(bi.getId())), BookingComment.class)
						.map(bc -> {
							bookingView.setComment(bc.getComment());
							return bookingView;
						}).defaultIfEmpty(bookingView))
				.flatMap(bv -> this.convBookingViewPhase(bv, booking));
	}

	@Override
	public Mono<BookingHistoryView> convert2BookingView(BookingHistory bookingHistory) {
		var bookingHistoryView = new BookingHistoryView(bookingHistory);
		return Mono.just("T".equals(bookingHistory.getType())).filter(b -> !b).flatMap(b -> {
			bookingHistoryView.setHoldId(bookingHistory.getItemId());
			return this.vHoldItemService.getVHoldItemById(bookingHistory.getItemId()).map(VHoldItem::getCallVolId);
		}).switchIfEmpty(Mono.just(bookingHistory.getAssociateId()).filter(holdId -> holdId > 0)
				.switchIfEmpty(this.vHoldItemsService.getOneHoldIdByCallVolId(bookingHistory.getItemId())).map(hId -> {
					bookingHistoryView.setHoldId(hId);
					return bookingHistory.getItemId();
				})).flatMap(this.vMarcCallVolumeService::getMarcVolumeByCallVolId).flatMap(mcv -> {
					bookingHistoryView.setMarcVolume(mcv);
					return this.calVolTemplate
							.selectOne(query(where("id").is(bookingHistory.getId())), BookingComment.class).map(bc -> {
								bookingHistoryView.setComment(bc.getComment());
								return bookingHistoryView;
							}).defaultIfEmpty(bookingHistoryView);
				}).flatMap(bhv -> this.calVolTemplate
						.selectOne(query(where(BOOKING_ID).is(bookingHistory.getId())), BookingAvailation.class).map(ba -> {
							bhv.setNoticeId(ba.getNoticeId());
							return bhv;
						}).defaultIfEmpty(bhv));
	}

	@Override
	public Flux<BookingHistory> findBookingHistoriesByUserId(int readerId, boolean onlyOverdue, boolean overNotAvail) {
		List<Phase> phases = new ArrayList<>();
		if (onlyOverdue) {
			Stream.of(Phase.OVERDUE_BOOKING, Phase.OVERDUE_BOOKING_WAITING, Phase.ON_STOP_BOOKING).forEach(phases::add);
			if (overNotAvail)
				Stream.of(Phase.OVERDUE_CANCEL, Phase.END_STOP_BOOKING, Phase.OVERDUE_OVER_AVAIL).forEach(phases::add);
			return this.calVolTemplate
					.select(query(where(USER_ID).is(readerId).and("phase").in(phases)), BookingHistory.class)
					.sort(Comparator.comparing(BookingHistory::getUpdateTime).reversed());
		}
		return this.calVolTemplate.select(query(where(USER_ID).is(readerId)), BookingHistory.class)
				.sort(Comparator.comparing(BookingHistory::getUpdateTime).reversed());
	}

	private Mono<BookingView> convBookingViewPhase(BookingView bookingView, Booking booking) {
		Mono<BookingView> mono;
		Phase phase = bookingView.getPhase();
		long bId = booking.getId();
		switch (phase) {
		case AVAILABLE, WAIT_ANNEX, A01_ORDER ->
			mono = this.calVolTemplate.selectOne(query(where(BOOKING_ID).is(bId)), BookingAvailation.class).map(ba -> {
				bookingView.setAvailSeqNum(ba.getAvailSeqNum());
				bookingView.setExpDuedateType(ba.isExpDuedateMark());
				bookingView.setNoticeId(ba.getNoticeId());
				return bookingView;
			}).defaultIfEmpty(bookingView).flatMap(bv -> {
				bv.setAvailableDateTime(booking.getAvailableDate());
				bv.setAvailableDate(booking.getAvailableDate().toLocalDate());
				return this.calVolTemplate.exists(query(where(BOOKING_ID).is(bId)), BookingExpandDuedate.class)
						.map(b -> {
							bv.setHadExpDueDate(b);
							return b;
						}).flatMap(b -> this.itemSiteDefService.allowExpandDueDateBySiteIdAndAvailDate(
								booking.getPickupSiteId(), booking.getAvailableDate()))
						.map(b1 -> {
							bv.setExpDuedateSite(b1);
							if (booking.getDueDate() != null)
								bv.setDuePickupDate(booking.getDueDate());
							bv.setHoldId(booking.getAssociateId());
							return bv;
						});
			});

		case TRANSIT_B -> mono = this.calVolTemplate
				.selectOne(query(where("holdId").is(booking.getAssociateId())), Intransit.class).map(it -> {
					bookingView.setIntransit(new IntransitView(it));
					return bookingView;
				});

		case PLACE, SUSPENSION, DISTRIBUTION ->
			mono = Mono.just("T".equals(booking.getType())).filter(b -> b).map(b -> booking.getItemId())
					.flatMap(cvId -> this.vBookingService.findBookingIdsByItemId(cvId).flatMap(li -> {
						int pos = li.indexOf(String.valueOf(booking.getId())) + 1;
						bookingView.setPosition(pos);
						return this.vHoldItemsService.getOneHoldIdByCallVolId(cvId);
					})).defaultIfEmpty(booking.getItemId()).map(hId -> {
						bookingView.setHoldId(hId);
						bookingView.setCanModify(true);
						bookingView.setCanCanceled(true);
						LocalDateTime distributeDate = booking.getDistributeDate();
						if (distributeDate != null)
							bookingView.setDistributeDate(distributeDate.toLocalDate());
						return bookingView;
					});

		default -> throw new IllegalArgumentException("Unexpected value: " + phase);
		}
		return mono;
	}

	@Override
	public Flux<BookingHistoryView> getReaderOnStopBookingHistories(int readerId, Phase onStopBooking) {
		return this.calVolTemplate
				.select(query(where(USER_ID).is(readerId).and("phase").is(onStopBooking)), BookingHistory.class)
				.sort(Comparator.comparing(BookingHistory::getInActiveDate)).flatMap(this::convert2BookingView);
	}

	@Override
	public Mono<BookingHistoryView> convert2ExpandBookingView(long bookingId) {
		return this.calVolTemplate
				.selectOne(query(where("id").is(bookingId)), BookingHistory.class).switchIfEmpty(this.calVolTemplate
						.selectOne(query(where("id").is(bookingId)), Booking.class).map(BookingHistory::new))
				.flatMap(this::convert2BookingView);
	}

}
