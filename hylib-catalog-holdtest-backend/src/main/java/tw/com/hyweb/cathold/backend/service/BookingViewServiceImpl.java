package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import tw.com.hyweb.cathold.model.UserStopBooking;
import tw.com.hyweb.cathold.model.UserSuspendBooking;
import tw.com.hyweb.cathold.model.VBookingCabinetHold;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.view.BookingHistories;
import tw.com.hyweb.cathold.model.view.BookingHistoryView;
import tw.com.hyweb.cathold.model.view.BookingNclView;
import tw.com.hyweb.cathold.model.view.BookingView;
import tw.com.hyweb.cathold.model.view.BookingViewComparator;
import tw.com.hyweb.cathold.model.view.IntransitView;
import tw.com.hyweb.cathold.model.view.MarcVolume;
import tw.com.hyweb.cathold.model.view.ReaderBookingSummary;

@RequiredArgsConstructor
public class BookingViewServiceImpl implements BookingViewService {

	private static final List<Phase> AVAIL_PHASES = Arrays.asList(Phase.AVAILABLE, Phase.A01_ORDER, Phase.CAB_WAIT,
			Phase.OVERDUE_BOOKING_WAITING);

	private static final String USER_ID = "userId";

	private static final String BOOKING_ID = "bookingId";

	private static final String PHASE = "phase";

	private static final String UPDATE_TIME = "updateTime";

	private final BookingExpandDuedateService bookingExpandDuedateService;

	private final VBookingService vBookingService;

	private final VHoldItemService vHoldItemService;

	private final VHoldItemsService vHoldItemsService;

	private final VMarcCallVolumeService vMarcCallVolumeService;

	private final ItemSiteDefService itemSiteDefService;

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Flux<BookingView> getReaderBookingViews(int readerId, int skip, int take, Boolean isAvailation,
			boolean containCopy) {
		return this.calVolTemplate.select(query(where(USER_ID).is(readerId)), Booking.class)
				.filter(bi -> this.filterBookingAvailOrCopy(isAvailation, containCopy, "C".equals(bi.getType()),
						bi.getPhase()))
				.sort(BookingViewComparator.INSTANCE).skip(skip).take(take)
				.flatMapSequential(this::convert2BookingView);
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
				})
				.flatMap(bhv -> this.calVolTemplate
						.selectOne(query(where(BOOKING_ID).is(bookingHistory.getId())), BookingAvailation.class)
						.map(ba -> {
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
			return this.calVolTemplate.select(
					query(where(USER_ID).is(readerId).and(PHASE).in(phases)).sort(Sort.by(Direction.DESC, UPDATE_TIME)),
					BookingHistory.class);
		}
		return this.calVolTemplate.select(query(where(USER_ID).is(readerId)).sort(Sort.by(Direction.DESC, UPDATE_TIME)),
				BookingHistory.class);
	}

	private Mono<Long> countBookingHistoriesByUserId(int readerId, boolean onlyOverdue, boolean overNotAvail) {
		List<Phase> phases = new ArrayList<>();
		if (onlyOverdue) {
			Stream.of(Phase.OVERDUE_BOOKING, Phase.OVERDUE_BOOKING_WAITING, Phase.ON_STOP_BOOKING).forEach(phases::add);
			if (overNotAvail)
				Stream.of(Phase.OVERDUE_CANCEL, Phase.END_STOP_BOOKING, Phase.OVERDUE_OVER_AVAIL).forEach(phases::add);
			return this.calVolTemplate.count(query(where(USER_ID).is(readerId).and(PHASE).in(phases)),
					BookingHistory.class);
		}
		return this.calVolTemplate.count(query(where(USER_ID).is(readerId)), BookingHistory.class);

	}

	private Mono<BookingView> convBookingViewPhase(BookingView bookingView, Booking booking) {
		Mono<BookingView> mono;
		Phase phase = bookingView.getPhase();
		long bId = booking.getId();
		switch (phase) {
		case AVAILABLE, WAIT_ANNEX, A01_ORDER, CAB_WAIT ->
			mono = this.calVolTemplate.selectOne(query(where(BOOKING_ID).is(bId)), BookingAvailation.class)
					.flatMap(ba -> this.convertAvailSeqNum(ba).map(s -> {
						bookingView.setAvailSeqNum(s);
						bookingView.setExpDuedateType(ba.isExpDuedateMark());
						bookingView.setNoticeId(ba.getNoticeId());
						return bookingView;
					})).defaultIfEmpty(bookingView).flatMap(bv -> {
						LocalDateTime availDate = booking.getAvailableDate();
						if (availDate != null) {
							bv.setAvailableDateTime(availDate);
							bv.setAvailableDate(availDate.toLocalDate());
						}
						return this.calVolTemplate.exists(query(where(BOOKING_ID).is(bId)), BookingExpandDuedate.class)
								.map(b -> {
									bv.setHadExpDueDate(b);
									return b;
								}).flatMap(b -> this.itemSiteDefService.allowExpandDueDateBySiteIdAndAvailDate(
										booking.getPickupSiteId(), availDate))
								.map(b1 -> {
									bv.setExpDuedateSite(b1);
									if (booking.getDueDate() != null)
										bv.setDuePickupDate(booking.getDueDate());
									bv.setHoldId(booking.getAssociateId());
									return bv;
								});
					});

		case TRANSIT_B ->
			mono = this.calVolTemplate.selectOne(query(where("holdId").is(booking.getAssociateId())), Intransit.class)
					.defaultIfEmpty(new Intransit(booking.getTransitDate())).map(it -> {
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

	private Mono<String> convertAvailSeqNum(BookingAvailation bookingAvailation) {
		if (bookingAvailation.getSeqNum() == -2)
			return this.calVolTemplate
					.selectOne(query(where(BOOKING_ID).is(bookingAvailation.getBookingId())), VBookingCabinetHold.class)
					.map(VBookingCabinetHold::getCabinetCode).defaultIfEmpty(bookingAvailation.getAvailSeqNum());
		return Mono.just(bookingAvailation.getAvailSeqNum());
	}

	@Override
	public Flux<BookingHistoryView> getReaderOnStopBookingHistories(int readerId, Phase onStopBooking) {
		return this.calVolTemplate
				.select(query(where(USER_ID).is(readerId).and(PHASE).is(onStopBooking)).sort(Sort.by("inActiveDate")),
						BookingHistory.class)
				.flatMap(this::convert2BookingView);
	}

	@Override
	public Mono<BookingHistoryView> convert2ExpandBookingView(long bookingId) {
		return this.calVolTemplate
				.selectOne(query(where("id").is(bookingId)), BookingHistory.class).switchIfEmpty(this.calVolTemplate
						.selectOne(query(where("id").is(bookingId)), Booking.class).map(BookingHistory::new))
				.flatMap(this::convert2BookingView);
	}

	@Override
	public Mono<BookingNclView> convert2BookingNclView(Booking booking) {
		return this.vMarcCallVolumeService.getMarcCallVolumeByCallVolId(booking.getItemId()).map(mcv -> {
			BookingNclView bnv = new BookingNclView(booking);
			bnv.setMarcVolume(new MarcVolume(mcv));
			return bnv;
		});
	}

	@Override
	public Mono<ReaderBookingSummary> getReaderBookingSummary(int readerId) {
		List<Phase> odPhases = List.of(Phase.OVERDUE_BOOKING, Phase.ON_STOP_BOOKING, Phase.OVERDUE_BOOKING_WAITING);
		Mono<Integer> biNumMono = this.calVolTemplate.count(query(where(USER_ID).is(readerId)), Booking.class)
				.map(Long::intValue);
		Mono<List<BookingView>> abvsMono = this.getBookingViewsByReaderId(readerId, true).collectList();
		Mono<List<BookingHistoryView>> odbvsMono = this.calVolTemplate
				.select(query(where(USER_ID).is(readerId).and(PHASE).in(odPhases)).sort(Sort.by(UPDATE_TIME)),
						BookingHistory.class)
				.flatMapSequential(this::convert2BookingView).collectList();
		Mono<Integer> bedNumMono = this.bookingExpandDuedateService.getExpandDuedatesOnMonthNum(readerId);
		Mono<UserSuspendBooking> usbMono = this.calVolTemplate
				.select(query(where(USER_ID).is(readerId)).sort(Sort.by(Direction.DESC, "id")),
						UserSuspendBooking.class)
				.next().defaultIfEmpty(new UserSuspendBooking());
		return Mono.zip(biNumMono, abvsMono, odbvsMono, bedNumMono, usbMono).map(ReaderBookingSummary::new)
				.flatMap(rbs -> {
					rbs.setUserId(readerId);
					return this.calVolTemplate.selectOne(query(where(USER_ID).is(readerId).and("available").isTrue()),
							UserStopBooking.class).map(usb -> {
								rbs.setStopBegDate(usb.getBegDate());
								rbs.setStopEndDate(usb.getEndDate());
								return rbs;
							}).defaultIfEmpty(rbs);
				});
	}

	@Override
	public Mono<BookingHistories> getReaderBookingHistories(int readerId, Boolean onlyOverdue, Boolean availOver,
			int skip, int take) {
		return this.findBookingHistoriesByUserId(readerId, onlyOverdue, availOver).skip(skip).take(take)
				.flatMap(this::convert2BookingView).collectList()
				.flatMap(li -> this.countBookingHistoriesByUserId(readerId, onlyOverdue, availOver).map(Long::intValue)
						.map(n -> new BookingHistories(n, li)))
				.defaultIfEmpty(new BookingHistories(0, Collections.emptyList()));
	}

}
