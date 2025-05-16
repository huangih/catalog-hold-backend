package tw.com.hyweb.cathold.backend.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import tw.com.hyweb.cathold.backend.redis.service.VCallVolHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.backend.redis.service.VMarcHoldSummaryService;
import tw.com.hyweb.cathold.backend.service.AmqpBackendClient;
import tw.com.hyweb.cathold.backend.service.BookingExpandDuedateService;
import tw.com.hyweb.cathold.backend.service.BookingResultViewService;
import tw.com.hyweb.cathold.backend.service.BookingStatusViewService;
import tw.com.hyweb.cathold.backend.service.BookingViewService;
import tw.com.hyweb.cathold.backend.service.ItemSiteDefService;
import tw.com.hyweb.cathold.backend.service.LendCheckService;
import tw.com.hyweb.cathold.backend.service.TouchService;
import tw.com.hyweb.cathold.backend.service.UserStopBookingService;
import tw.com.hyweb.cathold.backend.service.UserSuspendBookingService;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.BookingCloseDate;
import tw.com.hyweb.cathold.model.BookingDistribution;
import tw.com.hyweb.cathold.model.BookingDueDate;
import tw.com.hyweb.cathold.model.BookingHistory;
import tw.com.hyweb.cathold.model.BookingResult;
import tw.com.hyweb.cathold.model.DistriFoundStatic;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.Intransit;
import tw.com.hyweb.cathold.model.LendCheck;
import tw.com.hyweb.cathold.model.NoticeResult;
import tw.com.hyweb.cathold.model.NoticeSmsRule;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.ResultPhase;
import tw.com.hyweb.cathold.model.TransitOverdays;
import tw.com.hyweb.cathold.model.TransitOverdaysStatic;
import tw.com.hyweb.cathold.model.UserSuspendBooking;
import tw.com.hyweb.cathold.model.VBookingAvailRemove;
import tw.com.hyweb.cathold.model.VBookingAvailation;
import tw.com.hyweb.cathold.model.VBookingAvailationHistory;
import tw.com.hyweb.cathold.model.VBookingDistributionNum;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.client.TouchResult;
import tw.com.hyweb.cathold.model.view.BookingAvailWaitings;
import tw.com.hyweb.cathold.model.view.BookingAvailationView;
import tw.com.hyweb.cathold.model.view.BookingHistories;
import tw.com.hyweb.cathold.model.view.BookingHistoryView;
import tw.com.hyweb.cathold.model.view.BookingResultView;
import tw.com.hyweb.cathold.model.view.BookingStatusView;
import tw.com.hyweb.cathold.model.view.BookingView;
import tw.com.hyweb.cathold.model.view.CallVolHoldSummary;
import tw.com.hyweb.cathold.model.view.ExpandDuedateView;
import tw.com.hyweb.cathold.model.view.ReaderBookingSummary;
import tw.com.hyweb.cathold.model.view.ReaderStopBookingInfo;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;
import tw.com.hyweb.cathold.model.view.TransitOverdaysStaticView;
import tw.com.hyweb.cathold.model.view.TransitOverdaysView;
import tw.com.hyweb.cathold.model.view.UserBookingResultView;

@RequiredArgsConstructor
@Slf4j
public class CatvolBookingServiceImpl implements CatvolBookingService {

	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

	private static final String SEQNUM = "seqNum";

	private static final String SITE_ID = "siteId";

	private static final String SITE_CODE = "siteCode";

	private static final String HOLD_ID = "holdId";

	private static final String READER_ID = "readerId";

	private static final String USER_ID = "userId";

	private static final String MUSER_ID = "muserId";

	private static final String BOOKING_IDS = "bookingIds";

	private static final String PICKUP_SITEID = "pickupSiteId";

	private static final String COMMENT = "comment";

	private static final String BEG_DATE = "begDate";

	private static final String END_DATE = "endDate";

	private static final String AVAILABLE_DATE = "availableDate";

	private static final String DUE_DATE = "dueDate";

	private static final String RULE_ID = "ruleId";

	private static final String TRANSIT_DATE = "transitDate";

	private static final String PHASE = "phase";

	private static final String SKIP = "skip";

	private static final String TAKE = "take";

	private static final String FROM_PSITECODE = "fromPSiteCode";

	private static final String TOMOBILE_MSG = "toMobileMsg";

	private final BookingViewService bookingViewService;

	private final BookingResultViewService bookingResultViewService;

	private final BookingExpandDuedateService bookingExpandDuedateService;

	private final BookingStatusViewService bookingStatusViewService;

	private final ItemSiteDefService itemSiteDefService;

	private final UserStopBookingService userStopBookingService;

	private final UserSuspendBookingService userSuspendBookingService;

	private final LendCheckService lendCheckService;

	private final VMarcHoldSummaryService vMarcHoldSummaryService;

	private final VCallVolHoldSummaryService vCallVolHoldSummaryService;

	private final VHoldItemService vHoldItemService;

	private final VHoldClientService vHoldClientService;

	private final TouchService touchService;

	private final R2dbcEntityOperations calVolTemplate;

	private final AmqpBackendClient amqpBackendClient;

	@Override
	public Mono<ServerResponse> getReaderBookingViews(ServerRequest request) {
		Mono<Integer> ridMono = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt);
		Mono<Integer> skipMono = Mono.justOrEmpty(request.queryParam(SKIP)).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> takeMono = Mono.justOrEmpty(request.queryParam(TAKE)).map(Integer::parseInt).defaultIfEmpty(999);
		Flux<BookingView> flux = ridMono.flatMapMany(rId -> skipMono.flatMapMany(skip -> takeMono
				.flatMapMany(take -> this.bookingViewService.getReaderBookingViews(rId, skip, take, null, true))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReadersBookingViews(ServerRequest request) {
		Flux<Integer> readerFlux = Mono.justOrEmpty(request.queryParams().get("readerIds"))
				.flatMapMany(Flux::fromIterable).map(Integer::parseInt);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(readerFlux.flatMap(this.bookingViewService::getAllBookingViewsByReaderId), BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReaderBookingSummary(ServerRequest request) {
		Mono<Integer> param0 = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(param0.flatMap(this.bookingViewService::getReaderBookingSummary), ReaderBookingSummary.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReaderBookingHistories(ServerRequest request) {
		Mono<Integer> ridMono = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt)
				.filter(rId -> rId > 0);
		Mono<Boolean> oovMono = Mono.justOrEmpty(request.queryParam("onlyOverdue")).map(Boolean::parseBoolean)
				.defaultIfEmpty(true);
		Mono<Boolean> aovMono = Mono.justOrEmpty(request.queryParam("availOver")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<Integer> skipMono = Mono.justOrEmpty(request.queryParam(SKIP)).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> takeMono = Mono.justOrEmpty(request.queryParam(TAKE)).map(Integer::parseInt).defaultIfEmpty(999);
		Mono<BookingHistories> mono = ridMono
				.flatMap(rId -> oovMono.flatMap(oov -> aovMono.flatMap(aov -> skipMono.flatMap(skip -> takeMono.flatMap(
						take -> this.bookingViewService.getReaderBookingHistories(rId, oov, aov, skip, take))))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingHistories.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReaderStopBookingInfo(ServerRequest request) {
		Mono<Integer> param0 = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(param0.flatMap(this.userStopBookingService::getReaderStopBookingInfo),
						ReaderStopBookingInfo.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReaderSuspendBooking(ServerRequest request) {
		Mono<Integer> param0 = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(param0.flatMap(this.userSuspendBookingService::getReaderSuspendBooking), UserSuspendBooking.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> readerCanLendHold(ServerRequest request) {
		Mono<Integer> ridMono = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt)
				.filter(rId -> rId > 0);
		Mono<Integer> hidMono = Mono.justOrEmpty(request.queryParam(HOLD_ID)).map(Integer::parseInt)
				.filter(hId -> hId > 0);
		Mono<Integer> midMono = Mono.justOrEmpty(request.queryParam(MUSER_ID)).map(Integer::parseInt)
				.defaultIfEmpty(500);
		Mono<String> bcMono = Mono.justOrEmpty(request.queryParam("barcode"))
				.map(code -> code.length() > 30 ? code.substring(0, 30) : code).filter(code -> code.length() > 2)
				.defaultIfEmpty("");
		Mono<LendCheck> mono = ridMono.flatMap(rId -> hidMono.flatMap(hId -> midMono.flatMap(
				mId -> bcMono.flatMap(barcode -> this.lendCheckService.readerCanLendHold(rId, hId, mId, barcode)))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, LendCheck.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> lendCheckCallback(ServerRequest request) {
		Mono<String> cbIdMono = request.formData().map(map -> map.getFirst("callbackId"));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(cbIdMono.doOnNext(this.lendCheckService::lendCallback), String.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> canRenewLends(ServerRequest request) {
		Mono<Map<Integer, Boolean>> mapMono = Flux.fromIterable(request.queryParams().get("holdIdes"))
				.map(Integer::parseInt)
				.flatMap(hId -> this.vCallVolHoldSummaryService.checkRenewableLend(hId).map(b -> Tuples.of(hId, b)))
				.collectMap(Tuple2::getT1, Tuple2::getT2);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mapMono, Map.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> placeBooking(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(READER_ID)
				&& fm.containsKey("holdIds") && fm.containsKey("type") && fm.containsKey("pickSiteId"));
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Flux<Integer> hidFlux = formMap.map(m -> m.get("holdIds")).flatMapMany(Flux::fromIterable)
				.map(Integer::parseInt).filter(hId -> hId > 0);
		Mono<Character> typeMono = formMap.map(m -> m.getFirst("type")).map(s -> s.charAt(0))
				.filter(type -> type == 'T' || type == 'C');
		Mono<Integer> pickMono = formMap.map(m -> m.getFirst("pickSiteId")).map(Integer::parseInt)
				.filter(pickId -> pickId > 0);
		Mono<String> commentMono = formMap.map(m -> m.getFirst(COMMENT)).defaultIfEmpty("");
		Mono<Integer> muserMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500)
				.filter(mId -> mId > 0);
		Flux<BookingResultView> flux = ridMono.flatMapMany(rId -> typeMono.flatMapMany(
				type -> pickMono.flatMapMany(pick -> commentMono.flatMapMany(comment -> muserMono.flatMapMany(
						mId -> hidFlux.flatMap(hId -> this.vHoldItemService.getVHoldItemById(hId).flatMap(vh -> {
							int cvId = vh.getCallVolId();
							if ('T' == type)
								return this.vCallVolHoldSummaryService.findCallVolHoldSummaryByCallVolId(cvId, rId)
										.filter(chs -> chs.getAllowBookingNum() > 0)
										.flatMap(chs -> this.amqpBackendClient.placeBooking(rId, cvId, pick, comment,
												mId))
										.defaultIfEmpty(new BookingResult(ResultPhase.ZERO_ALLOWBOOKINGNUM)).map(br -> {
											br.setId(cvId);
											return br;
										});
							return this.amqpBackendClient.placeCopyBooking(rId, vh.getHoldId(), pick, comment, mId);
						}), 1)).flatMap(this.bookingResultViewService::convert2BookingResultView)))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> suspendBooking(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(READER_ID) && fm.containsKey(BEG_DATE) && fm.containsKey(END_DATE));
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Mono<LocalDate> bdMono = formMap.map(m -> m.getFirst(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = formMap.map(m -> m.getFirst(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<Integer> muserMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500)
				.filter(mId -> mId > 0);
		Mono<UserBookingResultView> mono = ridMono.flatMap(rId -> bdMono.flatMap(begDate -> edMono.flatMap(
				endDate -> muserMono.flatMap(mId -> this.amqpBackendClient.suspendBooking(rId, begDate, endDate, mId)
						.flatMap(this.bookingResultViewService::convert2UserBookingResultView)))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, UserBookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> cancelBooking(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(READER_ID) && fm.containsKey(BOOKING_IDS));
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Flux<Long> bidFlux = formMap.map(m -> m.get(BOOKING_IDS)).flatMapMany(Flux::fromIterable).map(Long::parseLong)
				.filter(bId -> bId > 0);
		Mono<String> commentMono = formMap.map(m -> m.getFirst(COMMENT)).defaultIfEmpty("");
		Mono<Boolean> ovMono = formMap.map(m -> m.getFirst("override")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<Integer> muserMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500)
				.filter(mId -> mId > 0);
		Flux<BookingResultView> flux = ridMono.flatMapMany(rId -> commentMono
				.flatMapMany(comment -> ovMono.flatMapMany(override -> muserMono.flatMapMany(mId -> bidFlux
						.flatMap(bId -> this.amqpBackendClient.cancelBooking(rId, bId, comment, override, mId), 1)
						.flatMap(this.bookingResultViewService::convert2BookingResultView)))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> updateBookingSiteDueDate(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(READER_ID) && fm.containsKey(BOOKING_IDS));
		Flux<Long> bidFlux = formMap.map(m -> m.get(BOOKING_IDS)).flatMapMany(Flux::fromIterable).map(Long::parseLong)
				.filter(bId -> bId > 0);
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Mono<Integer> sidMono = formMap.map(m -> m.getFirst(SITE_ID)).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<LocalDate> ddMono = formMap.map(m -> m.getFirst(DUE_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
				.defaultIfEmpty(LocalDate.now().minusDays(1));
		Mono<String> commentMono = formMap.map(m -> m.getFirst(COMMENT)).defaultIfEmpty("");
		Mono<Integer> muserMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500)
				.filter(mId -> mId > 0);
		Flux<BookingResultView> flux = ridMono.flatMapMany(rId -> sidMono.flatMapMany(siteId -> ddMono
				.flatMapMany(dueDate -> commentMono.flatMapMany(comment -> muserMono.flatMapMany(mId -> bidFlux
						.flatMap(bId -> this.amqpBackendClient.updateBookingSiteDueDate(bId, rId, siteId, dueDate,
								comment, mId), 1)
						.flatMap(this.bookingResultViewService::convert2BookingResultView))))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> cancelSuspendBooking(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(READER_ID));
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Mono<Integer> muserMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500)
				.filter(mId -> mId > 0);
		Mono<UserBookingResultView> mono = ridMono
				.flatMap(rId -> muserMono.flatMap(mId -> this.amqpBackendClient.cancelSuspendBooking(rId, mId)))
				.flatMap(this.bookingResultViewService::convert2UserBookingResultView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, UserBookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getCallVolHoldSummariesByMarcId(ServerRequest request) {
		Mono<Integer> midMono = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt)
				.filter(rId -> rId > 0);
		Mono<Boolean> ascMono = Mono.justOrEmpty(request.queryParam("isAsc")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<Boolean> pmMono = Mono.justOrEmpty(request.queryParam("isPm")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<Integer> skipMono = Mono.justOrEmpty(request.queryParam(SKIP)).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> takeMono = Mono.justOrEmpty(request.queryParam(TAKE)).map(Integer::parseInt).defaultIfEmpty(999);
		Flux<CallVolHoldSummary> flux = midMono.flatMap(this.vMarcHoldSummaryService::findMarcHoldSummaryByMarcId)
				.flatMap(mhs -> pmMono.filter(b -> b).map(b -> mhs.getPmCallVolIds())
						.defaultIfEmpty(mhs.getCallVolIds()))
				.flatMap(cvIds -> ascMono.filter(b -> b).map(b -> cvIds.reversed()).defaultIfEmpty(cvIds))
				.flatMapMany(li -> skipMono
						.flatMapMany(skip -> takeMono.flatMapMany(take -> Flux.fromIterable(li).skip(skip).take(take))))
				.flatMap(this.vCallVolHoldSummaryService::findCallVolHoldSummaryByCallVolId);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, CallVolHoldSummary.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> findCallVolHoldSummaryByCallVol(ServerRequest request) {
		int callVolId = 0;
		int readerId = 0;
		try {
			callVolId = Integer.parseInt(request.pathVariable("callVolId"));
			readerId = Integer.parseInt(request.pathVariable(READER_ID));
		} catch (Exception e) {
			// nothing
		}
		Mono<CallVolHoldSummary> mono;
		if (callVolId > 0 && readerId > 0)
			mono = this.vCallVolHoldSummaryService.findCallVolHoldSummaryByCallVolId(callVolId, readerId);
		else
			mono = Mono.empty();
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, CallVolHoldSummary.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> touchHoldItem(ServerRequest request) {
		Mono<String> bcMono = Mono.justOrEmpty(request.queryParam("barcode"))
				.map(code -> code.length() > 30 ? code.substring(0, 30) : code).filter(code -> code.length() > 2);
		Mono<String> seMono = Mono.justOrEmpty(request.queryParam("sessionId"));
		Mono<Integer> midMono = Mono.justOrEmpty(request.queryParam(MUSER_ID)).map(Integer::parseInt)
				.defaultIfEmpty(500);
		Mono<TouchResult> mono = bcMono.flatMap(barcode -> seMono
				.flatMap(seId -> midMono.flatMap(mId -> this.touchService.touchHoldItem(barcode, seId, mId))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, TouchResult.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getHoldClientsBySiteCode(ServerRequest request) {
		Flux<HoldClient> flux = Mono.justOrEmpty(request.queryParam(SITE_CODE))
				.flatMapMany(this.vHoldClientService::getHoldClientsBySiteCode);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, HoldClient.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getHoldAvailBookings(ServerRequest request) {
		Mono<Integer> hidMono = Mono.justOrEmpty(request.queryParam(HOLD_ID)).map(Integer::parseInt)
				.filter(hId -> hId > 0);
		Mono<BookingAvailationView> mono = hidMono
				.flatMap(hId -> this.calVolTemplate.selectOne(query(where(HOLD_ID).is(hId)), VBookingAvailation.class))
				.map(BookingAvailationView::new);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingAvailationView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReaderAvailBookings(ServerRequest request) {
		Flux<BookingAvailationView> flux = Flux.fromIterable(request.queryParams().get("readerIds"))
				.map(Integer::parseInt)
				.flatMap(rId -> this.calVolTemplate
						.select(query(where(USER_ID).is(rId)).sort(Sort.by(PICKUP_SITEID, SEQNUM)),
								VBookingAvailation.class)
						.map(BookingAvailationView::new), 1);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingAvailationView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getSiteOvarAvailBookingWaitings(ServerRequest request) {
		Mono<Integer> sidMono = Mono.justOrEmpty(request.queryParam(SITE_ID)).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		Mono<Integer> skipMono = Mono.justOrEmpty(request.queryParam(SKIP)).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> takeMono = Mono.justOrEmpty(request.queryParam(TAKE)).map(Integer::parseInt).defaultIfEmpty(999);
		Mono<BookingAvailWaitings> mono = sidMono
				.flatMap(siteId -> this.calVolTemplate
						.select(query(where(SITE_ID).is(siteId)).sort(Sort.by(SEQNUM)), VBookingAvailRemove.class)
						.map(BookingAvailationView::new).collectList())
				.filter(li -> !li.isEmpty()).flatMap(li -> skipMono.flatMap(skip -> takeMono.map(take -> {
					int len = li.size();
					int fSkip = skip >= len ? len - 1 : skip;
					int fTake = fSkip + take > len ? len - skip : take;
					return new BookingAvailWaitings(len, li.subList(fSkip, fSkip + fTake));
				}))).defaultIfEmpty(new BookingAvailWaitings(0, Collections.emptyList()));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingAvailWaitings.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getSiteOvarAvailBookingSeqNum(ServerRequest request) {
		List<Phase> overdueBookings = Arrays.asList(Phase.OVERDUE_BOOKING_WAITING, Phase.ANNEX_MISSING);
		Mono<Integer> sidMono = Mono.justOrEmpty(request.queryParam(SITE_ID)).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		Flux<String> flux = sidMono
				.flatMapMany(siteId -> this.calVolTemplate
						.select(query(where(SITE_ID).is(siteId).and(PHASE).in(overdueBookings)).sort(Sort.by(SEQNUM)),
								VBookingAvailationHistory.class)
						.map(VBookingAvailation::new).map(BookingAvailationView::new))
				.map(BookingAvailationView::getAvailSeqNum);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, String.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getSiteBookingDistributions(ServerRequest request) {
		Mono<Integer> sidMono = Mono.justOrEmpty(request.queryParam(SITE_ID)).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		Mono<List<String>> typesMono = Mono.justOrEmpty(request.queryParams().get("typeCodes"))
				.filter(li -> !li.isEmpty());
		Mono<Boolean> containsMono = Mono.justOrEmpty(request.queryParam("contains")).map(Boolean::parseBoolean);
		Flux<BookingDistribution> flux = sidMono
				.flatMapMany(
						siteId -> this.calVolTemplate
								.select(query(where("dueSite").is(siteId)),
										BookingDistribution.class)
								.filterWhen(
										bd -> typesMono
												.flatMap(types -> containsMono
														.flatMap(contains -> this.vHoldItemService
																.getVHoldItemById(bd.getHoldId())
																.map(vh -> !(types.contains(vh.getTypeCode())
																		^ contains))
																.defaultIfEmpty(false))
														.defaultIfEmpty(true))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingDistribution.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getReaderExpandDuesOnMonth(ServerRequest request) {
		Mono<Integer> ridMono = Mono.justOrEmpty(request.queryParam(READER_ID)).map(Integer::parseInt)
				.filter(rId -> rId > 0);
		Flux<ExpandDuedateView> flux = ridMono.flatMap(this.bookingExpandDuedateService::getExpandDuedatesOnMonth)
				.filter(li -> !li.isEmpty()).flatMapMany(Flux::fromIterable)
				.flatMap(bed -> this.bookingViewService.convert2ExpandBookingView(bed.getBookingId()).map(bhv -> {
					ExpandDuedateView edv = new ExpandDuedateView(bed);
					edv.setBookingHistoryView(bhv);
					return edv;
				}));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, ExpandDuedateView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> expandAvailDueDate(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(READER_ID) && fm.containsKey(BOOKING_IDS));
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Flux<Long> bidFlux = formMap.map(m -> m.get(BOOKING_IDS)).flatMapMany(Flux::fromIterable).map(Long::parseLong)
				.filter(bId -> bId > 0);
		Mono<Integer> midMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500);
		Flux<BookingResultView> flux = ridMono.flatMapMany(rId -> midMono
				.flatMapMany(mId -> bidFlux.flatMap(bId -> this.amqpBackendClient.expandAvailDueDate(rId, bId, mId))
						.flatMap(this.bookingResultViewService::convert2BookingResultView)));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> addDueDateRule(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(BEG_DATE));
		Mono<LocalDate> bdMono = formMap.map(m -> m.getFirst(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = formMap.map(m -> m.getFirst(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> duMono = formMap.map(m -> m.getFirst(DUE_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<String> siteMono = formMap.map(m -> m.getFirst(SITE_CODE)).defaultIfEmpty("ALL");
		Mono<Boolean> epMono = formMap.map(m -> m.getFirst("expand")).map(Boolean::parseBoolean);
		Mono<Boolean> ovjMono = formMap.map(m -> m.getFirst("overJustify")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<List<BookingDueDate>> mono = bdMono.flatMap(begDate -> edMono.defaultIfEmpty(begDate)
				.flatMap(endDate -> epMono.map(expand -> Boolean.TRUE.equals(expand) ? 1 : 0).defaultIfEmpty(2)
						.flatMap(expOp -> duMono.defaultIfEmpty(endDate.plusDays(1)).flatMap(
								dueDate -> siteMono.flatMap(site -> ovjMono.flatMap(ovJustify -> this.amqpBackendClient
										.addDueDateRule(begDate, endDate, dueDate, site, expOp, ovJustify)))))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(mono.flatMapMany(Flux::fromIterable), BookingDueDate.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> addBookingCloseDate(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(BEG_DATE));
		Mono<LocalDate> bdMono = formMap.map(m -> m.getFirst(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = formMap.map(m -> m.getFirst(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<String> siteMono = formMap.map(m -> m.getFirst(SITE_CODE)).defaultIfEmpty("ALL");
		Mono<BookingCloseDate> mono = bdMono
				.flatMap(begDate -> edMono.defaultIfEmpty(begDate).flatMap(endDate -> siteMono
						.flatMap(site -> this.calVolTemplate.insert(new BookingCloseDate(begDate, endDate, site)))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingCloseDate.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> addNoticeSmsRule(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(BEG_DATE)
				&& fm.containsKey(END_DATE) && fm.containsKey(FROM_PSITECODE) && fm.containsKey(TOMOBILE_MSG));
		Mono<LocalDate> bdMono = formMap.map(m -> m.getFirst(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = formMap.map(m -> m.getFirst(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<String> fsiteMono = formMap.map(m -> m.getFirst(FROM_PSITECODE));
		Mono<String> dsiteMono = formMap.map(m -> m.getFirst("descPSite")).defaultIfEmpty("");
		Mono<String> tmMono = formMap.map(m -> m.getFirst(TOMOBILE_MSG));
		Mono<String> tsiteMono = formMap.map(m -> m.getFirst("toPSiteCode")).defaultIfEmpty("");
		Flux<NoticeSmsRule> flux = bdMono
				.flatMap(begDate -> edMono.flatMap(endDate -> fsiteMono.flatMap(
						fromPSiteCode -> tmMono.flatMap(toMobileMsg -> dsiteMono.flatMap(descPSite -> tsiteMono.flatMap(
								toPSiteCode -> this.amqpBackendClient.addNoticeSmsRule(new NoticeSmsRule(begDate,
										endDate, fromPSiteCode, descPSite, toMobileMsg, toPSiteCode))))))))
				.flatMapMany(Flux::fromIterable);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, NoticeSmsRule.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingDueDateRules(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
				.defaultIfEmpty(LocalDate.now());
		Flux<BookingDueDate> flux = bdMono.flatMapMany(begDate -> this.calVolTemplate
				.select(query(where(BEG_DATE).greaterThanOrEquals(begDate)), BookingDueDate.class));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingDueDate.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingCloseDates(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
				.defaultIfEmpty(LocalDate.now());
		Flux<BookingCloseDate> flux = bdMono.flatMapMany(begDate -> this.calVolTemplate
				.select(query(where(BEG_DATE).greaterThanOrEquals(begDate)), BookingCloseDate.class));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingCloseDate.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getNoticeSmsRules(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
				.defaultIfEmpty(LocalDate.now());
		Flux<NoticeSmsRule> flux = bdMono.flatMapMany(begDate -> this.calVolTemplate
				.select(query(where(BEG_DATE).greaterThanOrEquals(begDate)), NoticeSmsRule.class));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, NoticeSmsRule.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> rollbackBookingDueDateRule(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(RULE_ID));
		Mono<Integer> ruidMono = formMap.map(m -> m.getFirst(RULE_ID)).map(Integer::parseInt);
		Mono<BookingDueDate> mono = ruidMono
				.flatMap(ruleId -> this.calVolTemplate.selectOne(query(where("id").is(ruleId)), BookingDueDate.class)
						.doOnNext(this.amqpBackendClient::rollbackBookingDueDateRule));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingDueDate.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> delBookingCloseDate(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey("id"));
		Mono<String> mono = formMap.map(m -> m.getFirst("id")).map(Integer::parseInt).filter(id -> id > 0)
				.flatMap(id -> this.calVolTemplate.selectOne(query(where("id").is(id)), BookingCloseDate.class))
				.flatMap(this.calVolTemplate::delete).map(bcd -> "bookingCloseDate deleted: " + bcd.getId());
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, String.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> delNoticeSmsRule(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(RULE_ID));
		Flux<NoticeSmsRule> flux = formMap.map(m -> m.getFirst(RULE_ID)).map(Integer::parseInt)
				.flatMap(this.amqpBackendClient::delNoticeSmsRule).flatMapMany(Flux::fromIterable);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, NoticeSmsRule.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> updBookingCloseDate(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey("id"));
		Mono<LocalDate> bdMono = formMap.map(m -> m.getFirst(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = formMap.map(m -> m.getFirst(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<String> sdMono = formMap.map(m -> m.getFirst(SITE_CODE));
		Mono<BookingCloseDate> mono = formMap.map(m -> m.getFirst("id")).map(Integer::parseInt).filter(id -> id > 0)
				.flatMap(id -> this.calVolTemplate.selectOne(query(where("id").is(id)), BookingCloseDate.class))
				.flatMap(bcd -> bdMono.map(begDate -> {
					bcd.setBegDate(begDate);
					return bcd;
				}).defaultIfEmpty(bcd)).flatMap(bcd1 -> edMono.map(endDate -> {
					bcd1.setEndDate(endDate);
					return bcd1;
				}).defaultIfEmpty(bcd1)).flatMap(bcd2 -> sdMono.map(siteCode -> {
					bcd2.setSiteCodes(siteCode);
					return bcd2;
				}).defaultIfEmpty(bcd2)).flatMap(this.calVolTemplate::update);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingCloseDate.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> updateNoticeSmsRule(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey(RULE_ID));
		Mono<LocalDate> bdMono = formMap.map(m -> m.getFirst(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = formMap.map(m -> m.getFirst(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<String> fsiteMono = formMap.map(m -> m.getFirst(FROM_PSITECODE));
		Mono<String> dsiteMono = formMap.map(m -> m.getFirst("descPSite"));
		Mono<String> tmMono = formMap.map(m -> m.getFirst(TOMOBILE_MSG));
		Mono<String> tsiteMono = formMap.map(m -> m.getFirst("toPSiteCode"));
		Flux<NoticeSmsRule> flux = Mono.justOrEmpty(request.queryParam(RULE_ID)).map(Integer::parseInt)
				.map(NoticeSmsRule::new).flatMap(nsr -> bdMono.map(begDate -> {
					nsr.setBegDate(begDate);
					return nsr;
				}).defaultIfEmpty(nsr)).flatMap(nsr -> edMono.map(endDate -> {
					nsr.setEndDate(endDate);
					return nsr;
				}).defaultIfEmpty(nsr)).flatMap(nsr -> fsiteMono.map(fromPSiteCode -> {
					nsr.setFromPSiteCode(fromPSiteCode);
					return nsr;
				}).defaultIfEmpty(nsr)).flatMap(nsr -> dsiteMono.map(descPSite -> {
					nsr.setDescPSite(descPSite);
					return nsr;
				}).defaultIfEmpty(nsr)).flatMap(nsr -> tmMono.map(toMobileMsg -> {
					nsr.setToMobileMsg(toMobileMsg);
					return nsr;
				}).defaultIfEmpty(nsr)).flatMap(nsr -> tsiteMono.map(toPSiteCode -> {
					nsr.setToPSiteCode(toPSiteCode);
					return nsr;
				}).defaultIfEmpty(nsr)).flatMap(this.amqpBackendClient::updateNoticeSmsRule)
				.flatMapMany(Flux::fromIterable);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, NoticeSmsRule.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> addHoldClient(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(SITE_CODE) && fm.containsKey("name"));
		Mono<String> scmono = formMap.map(m -> m.getFirst(SITE_CODE)).filter(s -> !s.isEmpty());
		Mono<String> nameMono = formMap.map(m -> m.getFirst("name")).filter(s -> !s.isEmpty());
		Mono<String> nrMono = formMap.map(m -> m.getFirst("noIntransitSites")).defaultIfEmpty("");
		Mono<String> gsMono = formMap.map(m -> m.getFirst("giveSeqProp"));
		Mono<String> srMono = formMap.map(m -> m.getFirst("seqRange")).defaultIfEmpty("1-600#0");
		Mono<String> ntMono = formMap.map(m -> m.getFirst("noticeTypes")).defaultIfEmpty("HOT-BOOK#2,HOT-BA#3");
		Mono<String> nsMono = formMap.map(m -> m.getFirst("noticeSites")).defaultIfEmpty("");
		Mono<String> nlMono = formMap.map(m -> m.getFirst("noticeLocations")).defaultIfEmpty("18Y#1,BSP#2");
		Mono<Boolean> tdMono = formMap.map(m -> m.getFirst("transitDouble")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<Boolean> tfMono = formMap.map(m -> m.getFirst("toFloatLoc")).map(Boolean::parseBoolean)
				.defaultIfEmpty(true);
		Mono<HoldClient> mono = scmono
				.flatMap(code -> nameMono.map(name -> HoldClient.builder().siteCode(code).name(name))
						.flatMap(hcb -> gsMono.map(hcb::giveSeqProp).defaultIfEmpty(hcb.giveSeqProp(code))))
				.flatMap(hcb -> nrMono.map(hcb::noIntransitSites).defaultIfEmpty(hcb))
				.flatMap(hcb -> srMono.map(hcb::seqRange).defaultIfEmpty(hcb))
				.flatMap(hcb -> ntMono.map(hcb::noticeTypes).defaultIfEmpty(hcb))
				.flatMap(hcb -> nsMono.map(hcb::noticeSites).defaultIfEmpty(hcb))
				.flatMap(hcb -> nlMono.map(hcb::noticeLocations).defaultIfEmpty(hcb))
				.flatMap(hcb -> tdMono.map(hcb::transitDouble).defaultIfEmpty(hcb))
				.flatMap(hcb -> tfMono.map(hcb::toFloatLoc).defaultIfEmpty(hcb)).map(hcb -> hcb.build())
				.flatMap(this.vHoldClientService::addHoldClient);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, HoldClient.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> updateHoldClient(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData().filter(fm -> fm.containsKey("clientId"));
		Mono<Integer> cIdmono = formMap.map(m -> m.getFirst("clientId")).map(Integer::parseInt).filter(cId -> cId > 0);
		Mono<String> nameMono = formMap.map(m -> m.getFirst("name"));
		Mono<String> nrMono = formMap.map(m -> m.getFirst("noIntransitSites"));
		Mono<String> gsMono = formMap.map(m -> m.getFirst("giveSeqProp"));
		Mono<String> srMono = formMap.map(m -> m.getFirst("seqRange"));
		Mono<String> ntMono = formMap.map(m -> m.getFirst("noticeTypes"));
		Mono<String> nsMono = formMap.map(m -> m.getFirst("noticeSites"));
		Mono<String> nlMono = formMap.map(m -> m.getFirst("noticeLocations"));
		Mono<Integer> seqMono = formMap.map(m -> m.getFirst(SEQNUM)).map(Integer::parseInt).filter(seq -> seq > 0)
				.defaultIfEmpty(-1);
		Mono<Boolean> tdMono = formMap.map(m -> m.getFirst("transitDouble")).map(Boolean::parseBoolean)
				.defaultIfEmpty(false);
		Mono<Boolean> tfMono = formMap.map(m -> m.getFirst("toFloatLoc")).map(Boolean::parseBoolean)
				.defaultIfEmpty(true);
		Mono<HoldClient> mono = cIdmono.map(cId -> HoldClient.builder().id(cId))
				.flatMap(hcb -> nameMono.map(hcb::name).defaultIfEmpty(hcb))
				.flatMap(hcb -> nrMono.map(hcb::noIntransitSites).defaultIfEmpty(hcb))
				.flatMap(hcb -> gsMono.map(hcb::giveSeqProp).defaultIfEmpty(hcb))
				.flatMap(hcb -> srMono.map(hcb::seqRange).defaultIfEmpty(hcb))
				.flatMap(hcb -> ntMono.map(hcb::noticeTypes).defaultIfEmpty(hcb))
				.flatMap(hcb -> nsMono.map(hcb::noticeSites).defaultIfEmpty(hcb))
				.flatMap(hcb -> nlMono.map(hcb::noticeLocations).defaultIfEmpty(hcb))
				.flatMap(hcb -> tdMono.map(hcb::transitDouble).defaultIfEmpty(hcb))
				.flatMap(hcb -> tfMono.map(hcb::toFloatLoc).defaultIfEmpty(hcb)).map(hcb -> hcb.build())
				.flatMap(hc -> seqMono.flatMap(seq -> this.vHoldClientService.updateHoldClient(hc, seq)));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, HoldClient.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingsBySeqNum(ServerRequest request) {
		Mono<String> siteMono = Mono.justOrEmpty(request.queryParam(SITE_CODE));
		Mono<Integer> seqMono = Mono.justOrEmpty(request.queryParam(SEQNUM)).map(Integer::parseInt)
				.filter(seqNum -> seqNum > 0);
		Mono<Integer> typeMono = Mono.justOrEmpty(request.queryParam("type")).filter(s -> !s.isEmpty())
				.map(Integer::parseInt).defaultIfEmpty(-1);
		Flux<VBookingAvailation> flux = siteMono
				.flatMap(siteCode -> seqMono.flatMap(seqNum -> typeMono
						.flatMap(type -> this.amqpBackendClient.getBookingsBySeqNum(siteCode, seqNum, type))))
				.flatMapMany(Flux::fromIterable);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, VBookingAvailation.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> cancelOverdueBooking(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(READER_ID) && fm.containsKey(BOOKING_IDS));
		Flux<Long> bidFlux = formMap.map(m -> m.get(BOOKING_IDS)).flatMapMany(Flux::fromIterable).map(Long::parseLong)
				.filter(bId -> bId > 0);
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Mono<String> commentMono = formMap.map(m -> m.getFirst(COMMENT)).defaultIfEmpty("");
		Mono<Integer> midMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500);
		Flux<BookingHistory> flux = ridMono.flatMapMany(
				readerId -> commentMono.flatMapMany(comment -> midMono.flatMapMany(muserId -> bidFlux.flatMap(
						bookingId -> this.amqpBackendClient.cancelOverdueBooking(bookingId, readerId, comment, muserId),
						1))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingHistory.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getNoSeqBookingAvailation(ServerRequest request) {
		Flux<Integer> sidFlux = Flux.fromIterable(request.queryParams().get("siteIds")).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		Flux<BookingView> flux = sidFlux
				.flatMap(siteId -> this.calVolTemplate
						.select(query(where(SEQNUM).is(0).and(PICKUP_SITEID).is(siteId)), VBookingAvailation.class)
						.flatMap(vba -> this.calVolTemplate.selectOne(query(where("id").is(vba.getBookingId())),
								Booking.class)))
				.sort(Comparator.comparing(Booking::getAvailableDate).reversed())
				.flatMap(this.bookingViewService::convert2BookingView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getWaitComfirmTransits(ServerRequest request) {
		Mono<Integer> sidMono = Mono.justOrEmpty(request.queryParam(SITE_ID)).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		List<Phase> waitPhases = Arrays.asList(Phase.WAIT_TRANSITB, Phase.WAIT_TRANSITR);
		Flux<Intransit> flux = sidMono.flatMapMany(
				siteId -> this.calVolTemplate.select(query(where(PHASE).in(waitPhases).and("fromSiteId").is(siteId))
						.sort(Sort.by(Direction.DESC, TRANSIT_DATE)), Intransit.class));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, Intransit.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingViewsByHoldId(ServerRequest request) {
		Mono<Integer> hidMono = Mono.justOrEmpty(request.queryParam(HOLD_ID)).map(Integer::parseInt)
				.filter(hId -> hId > 0);
		Mono<Integer> skipMono = Mono.justOrEmpty(request.queryParam(SKIP)).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> takeMono = Mono.justOrEmpty(request.queryParam(TAKE)).map(Integer::parseInt).defaultIfEmpty(10);
		Flux<BookingView> flux = hidMono
				.flatMapMany(holdId -> skipMono.flatMapMany(skip -> takeMono.flatMapMany(take -> {
					Flux<Booking> flux1 = this.calVolTemplate
							.select(query(where("associateId").is(holdId))
									.sort(Sort.by(Direction.DESC, "type").and(Sort.by("placeDate"))), Booking.class)
							.concatWith(this.calVolTemplate.select(
									query(where("itemId").is(holdId).and("type").is("C")).sort(Sort.by("placeDate")),
									Booking.class));
					Flux<Booking> flux2 = this.vHoldItemService.getVHoldItemById(holdId).map(VHoldItem::getCallVolId)
							.flatMap(cvId -> this.amqpBackendClient.getBookingsByItemId(cvId, skip, take))
							.flatMapMany(Flux::fromIterable);
					return skip == 0 ? flux1.concatWith(flux2) : flux2;
				}))).flatMap(this.bookingViewService::convert2BookingView, 1);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getIntransitByHoldId(ServerRequest request) {
		Mono<Integer> hidMono = Mono.justOrEmpty(request.queryParam(HOLD_ID)).map(Integer::parseInt)
				.filter(hId -> hId > 0);
		Mono<Intransit> mono = hidMono.flatMap(holdId -> this.calVolTemplate
				.selectOne(query(where(HOLD_ID).is(holdId)), Intransit.class).defaultIfEmpty(new Intransit()));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, Intransit.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getOverdaysTransitesBySiteId(ServerRequest request) {
		Mono<Integer> sidMono = Mono.justOrEmpty(request.queryParam(SITE_ID)).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		Mono<Integer> weeksMono = Mono.justOrEmpty(request.queryParam("weeks")).map(Integer::parseInt)
				.defaultIfEmpty(0);
		Mono<List<String>> typecodesMono = Mono.justOrEmpty(request.queryParams().get("typeCodes"));
		Mono<Boolean> containsMono = Mono.justOrEmpty(request.queryParam("contains")).map(Boolean::parseBoolean);
		Flux<TransitOverdaysView> flux = sidMono
				.flatMapMany(
						siteId -> this.calVolTemplate
								.select(query(where("dueSiteId").is(siteId).and("touchTime").isNull())
										.sort(Sort.by("weeks", TRANSIT_DATE)), TransitOverdays.class))
				.filterWhen(
						tod -> weeksMono.map(weeks -> tod.getWeeks() == weeks)
								.switchIfEmpty(
										typecodesMono
												.flatMap(typeCodes -> containsMono
														.flatMap(contains -> this.vHoldItemService
																.getVHoldItemById(tod.getHoldId())
																.map(vh -> !(typeCodes.contains(vh.getTypeCode())
																		^ contains))
																.defaultIfEmpty(false)))
												.defaultIfEmpty(true)))
				.flatMap(tod -> this.itemSiteDefService.getCodeById(tod.getFromSiteId())
						.flatMap(fsCode -> this.itemSiteDefService.getCodeById(tod.getToSiteId())
								.flatMap(tsCode -> this.itemSiteDefService.getCodeById(tod.getSiteId())
										.flatMap(sidCode -> this.itemSiteDefService.getCodeById(tod.getDueSiteId())
												.map(duCode -> {
													TransitOverdaysView tov = new TransitOverdaysView(tod);
													tov.setFromSiteCode(fsCode);
													tov.setToSiteCode(tsCode);
													tov.setSiteCode(sidCode);
													tov.setDueSiteCode(duCode);
													return tov;
												})))));

		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, TransitOverdaysView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingsDueDateAfter(ServerRequest request) {
		Mono<Integer> daysMono = Mono.justOrEmpty(request.queryParam("days")).map(Integer::parseInt)
				.filter(days -> days >= 0);
		Flux<BookingView> flux = daysMono
				.flatMapMany(days -> this.calVolTemplate
						.select(query(where(DUE_DATE).is(LocalDate.now().plusDays(days)).and(PHASE).is(Phase.AVAILABLE))
								.sort(Sort.by(USER_ID)), Booking.class))
				.flatMap(this.bookingViewService::convert2BookingView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingsAvailDateBetween(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = Mono.justOrEmpty(request.queryParam(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<String> typeMono = Mono.justOrEmpty(request.queryParam("type"));
		Flux<BookingView> flux = bdMono
				.flatMapMany(
						begDate -> edMono
								.flatMapMany(
										endDate -> this.calVolTemplate.select(
												query(where(AVAILABLE_DATE)
														.between(begDate.atStartOfDay(),
																endDate.plusDays(1).atStartOfDay())
														.and(PHASE).is(Phase.AVAILABLE)).sort(Sort.by(USER_ID)),
												Booking.class)))
				.filterWhen(bi -> typeMono.flatMap(type -> this.calVolTemplate
						.selectOne(query(where("id").is(bi.getId())), VBookingAvailation.class).map(vba -> {
							if ("l".equals(type))
								return vba.getLinepush();
							return Objects.equals(vba.getNtype(), type);
						}).defaultIfEmpty(false)).defaultIfEmpty(true))
				.flatMap(this.bookingViewService::convert2BookingView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getSiteAvailableDateBookings(ServerRequest request) {
		Mono<LocalDate> avdMono = Mono.justOrEmpty(request.queryParam("availDate"))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<Integer> sidMono = Mono.justOrEmpty(request.queryParam(SITE_ID)).map(Integer::parseInt)
				.filter(siteId -> siteId > 0);
		Flux<BookingAvailationView> flux = avdMono
				.flatMapMany(
						availDate -> sidMono
								.flatMapMany(
										siteId -> this.calVolTemplate.select(
												query(where(AVAILABLE_DATE)
														.between(availDate.atStartOfDay(),
																availDate.plusDays(1).atStartOfDay())
														.and(PHASE).is(Phase.AVAILABLE).and(PICKUP_SITEID).is(siteId))
														.sort(Sort.by(Direction.DESC, SEQNUM, AVAILABLE_DATE)),
												VBookingAvailation.class)))
				.map(BookingAvailationView::new);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingAvailationView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingsOverDueDateBetween(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = Mono.justOrEmpty(request.queryParam(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Flux<BookingHistoryView> flux = bdMono
				.flatMapMany(
						begDate -> edMono
								.flatMapMany(
										endDate -> this.calVolTemplate.select(
												query(where("inActiveDate")
														.between(begDate.atStartOfDay(),
																endDate.plusDays(1).atStartOfDay())
														.and(PHASE)
														.in(List.of(Phase.OVERDUE_BOOKING,
																Phase.OVERDUE_BOOKING_WAITING)))
														.sort(Sort.by(USER_ID)),
												BookingHistory.class)))
				.flatMap(this.bookingViewService::convert2BookingView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingNoticeResult(ServerRequest request) {
		Mono<Integer> nidMono = Mono.justOrEmpty(request.queryParam("noticeId")).map(Integer::parseInt)
				.filter(nid -> nid > 0);
		Flux<NoticeResult> flux = nidMono.flatMapMany(
				noticeId -> this.calVolTemplate.select(query(where("noticeId").is(noticeId)), NoticeResult.class));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, NoticeResult.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> rollbackFillBooking(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(MUSER_ID) && fm.containsKey("bookingId"));
		Mono<Long> bidMono = formMap.map(m -> m.getFirst("bookingId")).map(Long::parseLong).filter(bid -> bid > 0);
		Mono<Integer> midMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).defaultIfEmpty(500);
		Mono<BookingResultView> mono = bidMono
				.flatMap(bId -> midMono.flatMap(mId -> this.amqpBackendClient.rollbackFillBooking(bId, mId)))
				.flatMap(this.bookingResultViewService::convert2BookingResultView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingStatuses(ServerRequest request) {
		Mono<Integer> rateMono = Mono.justOrEmpty(request.queryParam("rate")).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> snMono = Mono.justOrEmpty(request.queryParam("supportNum")).map(Integer::parseInt)
				.defaultIfEmpty(0);
		Mono<Integer> rqMono = Mono.justOrEmpty(request.queryParam("reqNum")).map(Integer::parseInt).defaultIfEmpty(0);
		Mono<Integer> wdMono = Mono.justOrEmpty(request.queryParam("waitDays")).map(Integer::parseInt)
				.defaultIfEmpty(0);
		Flux<BookingStatusView> flux = rateMono.flatMapMany(rate -> snMono.flatMapMany(
				supportNum -> rqMono.flatMapMany(reqNum -> wdMono.flatMapMany(waitDays -> this.bookingStatusViewService
						.getBookingStatuses(rate, reqNum, supportNum, waitDays)))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingStatusView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getOverdaysTransitHoldIds(ServerRequest request) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(this.amqpBackendClient.getOverdaysTransitHoldIds().flatMapMany(Flux::fromIterable), Integer.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getTransitOverdaysStatic(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = Mono.justOrEmpty(request.queryParam(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Flux<TransitOverdaysStatic> flux = bdMono
				.flatMapMany(begDate -> edMono.flatMapMany(endDate -> this.calVolTemplate
						.select(query(where("createDate").between(bdMono, edMono)), TransitOverdaysStatic.class)));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, TransitOverdaysStatic.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getTransitOverdaysStaticView(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = Mono.justOrEmpty(request.queryParam(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Flux<TransitOverdaysStaticView> flux = bdMono
				.flatMapMany(begDate -> edMono.flatMapMany(endDate -> this.calVolTemplate
						.select(query(where("createDate").between(bdMono, edMono)), TransitOverdaysStatic.class)))
				.map(TransitOverdaysStaticView::new);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, TransitOverdaysStatic.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getDistriFoundStatics(ServerRequest request) {
		Mono<LocalDate> bdMono = Mono.justOrEmpty(request.queryParam(BEG_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Mono<LocalDate> edMono = Mono.justOrEmpty(request.queryParam(END_DATE))
				.map(s -> LocalDate.parse(s, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
		Flux<DistriFoundStatic> flux = bdMono.flatMapMany(begDate -> edMono.flatMapMany(endDate -> this.calVolTemplate
				.select(query(where("bookingDistriNumId.dueDate").between(begDate, endDate)),
						VBookingDistributionNum.class)
				.collectMultimap(VBookingDistributionNum::getSiteCode).flatMapIterable(Map::entrySet)
				.publishOn(Schedulers.boundedElastic()).flatMap(entry -> Flux.fromIterable(entry.getValue())
						.reduce(new DistriFoundStatic(entry.getKey()), (dfs, vbdn) -> {
							dfs.setDistriNum(dfs.getDistriNum() + vbdn.getDistriNum());
							dfs.setTotalDays(dfs.getTotalDays() + vbdn.getDays());
							Phase phase = vbdn.getBookingDistriNumId().getPhase();
							if (phase != Phase.ITEMMISSING && phase != Phase.NONE_DISTRIBUTE)
								dfs.setFoundNum(dfs.getFoundNum() + vbdn.getDistriNum());
							return dfs;
						}).flatMap(
								dfs -> this.itemSiteDefService.getIdByCode(dfs.getSiteCode())
										.flatMap(siteId -> this.calVolTemplate
												.count(query(where("dueSite").is(siteId).and("beginDate")
														.between(begDate.atStartOfDay(), endDate.atTime(23, 59, 59))),
														BookingDistribution.class)
												.map(Long::intValue).map(dfs::computeRate))))))
				.sort(Comparator.comparing(DistriFoundStatic::getSiteCode));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, DistriFoundStatic.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> editReaderBookingCallVol(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(READER_ID) && fm.containsKey("sourceBarcode")
						&& fm.containsKey("destBarcode") && fm.containsKey("action") && fm.containsKey(MUSER_ID));
		Mono<Integer> ridMono = formMap.map(m -> m.getFirst(READER_ID)).map(Integer::parseInt).filter(rId -> rId > 0);
		Mono<String> sbcMono = formMap.map(m -> m.getFirst("sourceBarcode"))
				.map(code -> code.length() > 30 ? code.substring(0, 30) : code).filter(code -> code.length() > 2);
		Mono<String> dbcMono = formMap.map(m -> m.getFirst("destBarcode"))
				.map(code -> code.length() > 30 ? code.substring(0, 30) : code).filter(code -> code.length() > 2);
		Mono<String> atMono = formMap.map(m -> m.getFirst("action"));
		Mono<Integer> midMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt);
		Mono<BookingResultView> mono = sbcMono.flatMap(this.vHoldItemService::getVHoldItemByBarcode)
				.flatMap(svh -> dbcMono.flatMap(this.vHoldItemService::getVHoldItemByBarcode).flatMap(dvh -> {
					if (svh.getMarcId() != dvh.getMarcId())
						return Mono.just(new BookingResult(ResultPhase.NOSAME_MARCID));
					return ridMono
							.flatMap(
									readerId -> atMono
											.flatMap(action -> midMono.flatMap(muserId -> this.calVolTemplate
													.select(query(where(READER_ID).is(readerId).and("itemId")
															.is(svh.getCallVolId()).and("type").is("T")), Booking.class)
													.next()
													.flatMap(bi -> this.amqpBackendClient.editReaderBookingCallVol(
															bi.getId(), dvh.getCallVolId(), action, muserId)))))
							.defaultIfEmpty(new BookingResult(ResultPhase.NOSUCH_BOOKING));
				}).defaultIfEmpty(new BookingResult(ResultPhase.HOLD_NOTEXISTS)))
				.flatMap(this.bookingResultViewService::convert2BookingResultView);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, BookingResultView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> tradeoffStopBookingDays(ServerRequest request) {
		Mono<MultiValueMap<String, String>> formMap = request.formData()
				.filter(fm -> fm.containsKey(USER_ID) && fm.containsKey("tradeoffDays") && fm.containsKey(MUSER_ID));
		Mono<Integer> uidMono = formMap.map(m -> m.getFirst(USER_ID)).map(Integer::parseInt).filter(uid -> uid > 0);
		Mono<Integer> tdMono = formMap.map(m -> m.getFirst("tradeoffDays")).map(Integer::parseInt);
		Mono<Integer> midMono = formMap.map(m -> m.getFirst(MUSER_ID)).map(Integer::parseInt).filter(mid -> mid > 0);
		Mono<TradeoffStopBookingResultView> mono = uidMono.flatMap(userId -> tdMono.flatMap(tradeoffDays -> midMono
				.flatMap(muserId -> this.amqpBackendClient.tradeoffStopBookingDays(userId, tradeoffDays, muserId))
				.doOnNext(tsbr -> log.info("{}", tsbr))
				.flatMap(tsbr -> this.bookingResultViewService.conver2TradeoffStopBookingResultView(tsbr, tradeoffDays))
				.doOnNext(tsbrv -> log.info("{}", tsbrv))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(mono, TradeoffStopBookingResultView.class).switchIfEmpty(ServerResponse.notFound().build());
	}

}
