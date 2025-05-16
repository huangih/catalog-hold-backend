package tw.com.hyweb.cathold.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import tw.com.hyweb.cathold.backend.redis.service.VCallVolHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.backend.redis.service.VLendCallBackService;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendCheck;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.ResultPhase;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.VIntransitBooking;
import tw.com.hyweb.cathold.sqlserver.model.SqlserverCharged;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverChargedRepository;

@Slf4j
@RequiredArgsConstructor
public class LendCheckServiceImpl implements LendCheckService {

	private static final String LASTITEM_READERTYPE = "checkLastItem";

	private static final String TRANSIT_OVERDAYS = "transitOverdays";

	private static final String LENDCALLBACK_NOFLOAT = "noFloatLend";

	private static final List<String> ANNEX_TYPES = List.of("BA", "AA", "BDA", "HOT-BA");

	private static final List<Character> TYPES = Arrays.asList('F', 'M', 'B', 'T', 'O', 'U', 'D', '7', '9');

	private static final String LENDCHECK = "LendCheck";

	private static final int WARN_RESTNUM = 1;

	private final BookingCheckService bookingCheckService;

	private final UserCheckService userCheckService;

	private final TransitOverdaysService transitOverdaysService;

	private final LendLog2Service lendLog2Service;

	private final MessageMapService messageMapService;

	private final SqlserverChargedRepository sqlserverChargedRepository;

	private final VHoldItemService vHoldItemService;

	private final VCallVolHoldSummaryService vCallVolHoldSummaryService;

	private final VLendCallBackService vLendCallBackService;

	private final AmqpBackendClient amqpBackendClient;

	@Override
	public Mono<LendCheck> readerCanLendHold(int readerId, int holdId, int muserId, String barcode) {
		return this.lendLog2Service.newLendLog(readerId, holdId, muserId, LocalDateTime.now()).map(LendCallback::new)
				.flatMap(lc -> this.checkRfidUidMap(lc, barcode)).flatMap(this::lastItemMissing)
				.flatMap(this::onAvailBooking).flatMap(this::onTransit).flatMap(this::onBookingAvailRemove)
				.flatMap(this::onUserBooking).flatMap(this::onBookingDistribution).flatMap(this::onCMissingLend)
				.flatMap(this.vLendCallBackService::prepareLendCallback);
	}

	// 查檢RFID UID對照表
	private Mono<LendCallback> checkRfidUidMap(LendCallback lendCallback, String barcode) {
		char type = 'F';
		if (barcode == null || barcode.isEmpty())
			return Mono.just(lendCallback);
		lendCallback.addCallbackType(type);
		return this.amqpBackendClient.checkUidByBarcode(barcode).filter(b -> b)
				.flatMap(b -> this.vHoldItemService.getVHoldItemByBarcode(barcode).map(VHoldItem::getSiteCode)
						.flatMap(this.amqpBackendClient::onMobileLendSite).filter(b1 -> b1).map(b1 -> "")
						.switchIfEmpty(this.messageMapService.resultPhaseConvert("RFIDCheck",
								ResultPhase.NOT_MOBILELEND_SITE)))
				.switchIfEmpty(this.messageMapService.resultPhaseConvert("RFIDCheck", ResultPhase.NOTON_UIDMAP))
				.map(s -> {
					if (!s.isEmpty()) {
						lendCallback.setCanotLendType(type);
						lendCallback.setReason(s);
					}
					return lendCallback;
				}).defaultIfEmpty(lendCallback)
				.timeout(Duration.ofMillis(2000), Mono.defer(() -> lendCallback.timeout("checkRfidUidMap", type)));
	}

	// 是否為有預約者的最後一件去向不明
	private Mono<LendCallback> lastItemMissing(LendCallback lendCallback) {
		char type = 'M';
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.userCheckService.checkReaderType(lendCallback.getReaderId(), LASTITEM_READERTYPE).filter(b -> b)
				.flatMap(b -> {
					lendCallback.addCallbackType(type);
					if (lendCallback.getCanotLendType() == 0)
						return this.vHoldItemService.getVHoldItemById(lendCallback.getHoldId())
								.map(VHoldItem::getCallVolId)
								.flatMap(this.vCallVolHoldSummaryService::findCallVolHoldSummaryByCallVolId)
								.filter(chs -> chs.getAllowBookingNum() <= WARN_RESTNUM).filterWhen(chs -> { // 是否為此callVol最後一本預約館藏
									int holdId = lendCallback.getHoldId();
									return Mono.just(chs.getWaitBookingNum() > 0).filter(b1 -> b1) // 若此callVol有預約,則true
											.switchIfEmpty(this.bookingCheckService.onAvailBooking(holdId)
													.map(bi -> true).filter(b2 -> b2)) // 或 此item為預約待取
											.switchIfEmpty(this.bookingCheckService.onTransitBooking(holdId)
													.map(vt -> true).filter(b3 -> b3)) // 或 此item為預約調撥
											.defaultIfEmpty(false);
								}).flatMap(chs -> this.messageMapService
										.resultPhaseConvert(LENDCHECK, ResultPhase.LASITEM_ALLOWBOOKING).map(s -> {
											lendCallback.setCanotLendType(type);
											lendCallback.setReason(s);
											return lendCallback;
										}));
					return Mono.just(lendCallback);
				}).defaultIfEmpty(lendCallback)
				.timeout(Duration.ofMillis(300000), Mono.defer(() -> lendCallback.timeout("lastItemMissing", type)));
	}

	// 是否為預約到館借閱
	private Mono<LendCallback> onAvailBooking(LendCallback lendCallback) {
		char type = 'B';
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.bookingCheckService.onAvailBooking(lendCallback.getHoldId()).flatMap(userId -> {
			lendCallback.addCallbackType(type);
			if (lendCallback.getCanotLendType() == 0 && lendCallback.getReaderId() != userId)
				return this.messageMapService.resultPhaseConvert(LENDCHECK, ResultPhase.NOT_BOOKING_USER).map(s -> {
					lendCallback.setCanotLendType(type);
					lendCallback.setReason(s);
					return lendCallback;
				});
			return Mono.just(lendCallback);
		}).defaultIfEmpty(lendCallback).timeout(Duration.ofMillis(3000),
				Mono.defer(() -> lendCallback.timeout("onAvailBooking", type)));
	}

	// 書是否處於調撥
	private Mono<LendCallback> onTransit(LendCallback lendCallback) {
		char type = 'T';
		List<Phase> phases = List.of(Phase.WAIT_TRANSITB, Phase.TRANSIT_B);
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.bookingCheckService.onTransitBooking(lendCallback.getHoldId())
				.zipWith(this.checkCMissingLend(lendCallback).map(tup2 -> tup2.getT1() && tup2.getT2()))
				.flatMap(tup2 -> {
					lendCallback.addCallbackType(type);
					VIntransitBooking vib = tup2.getT1();
					if (lendCallback.getCanotLendType() == 0) {
						if (vib.getUserId() == null)
							log.warn("onTransit-vib: {}", vib);
						else if (phases.contains(vib.getPhase()) && lendCallback.getReaderId() != vib.getUserId()
								&& Boolean.FALSE.equals(tup2.getT2()))
							return this.messageMapService.resultPhaseConvert(LENDCHECK, ResultPhase.HOLD_ON_TRANSIT_B)
									.map(s -> {
										lendCallback.setCanotLendType(type);
										lendCallback.setReason(s);
										return lendCallback;
									});
					}
					return Mono.just(lendCallback);
				}).defaultIfEmpty(lendCallback)
				.timeout(Duration.ofMillis(300000), Mono.defer(() -> lendCallback.timeout("onTransit", type)));
	}

	// 是否為待預約撤架
	// 預約待取撤架,若借閱者為原逾期未取者，取消待定記點，否則待定改為記點，可暫訂可借，依接的check
	private Mono<LendCallback> onBookingAvailRemove(LendCallback lendCallback) {
		char type = 'O';
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.bookingCheckService.onBookingAvailRemove(lendCallback.getHoldId()).filter(b -> b).map(b -> {
			lendCallback.addCallbackType(type);
			return lendCallback;
		}).defaultIfEmpty(lendCallback).timeout(Duration.ofMillis(3000),
				Mono.defer(() -> lendCallback.timeout("onBookingAvailRemove", type)));
	}

	// 借閱者是否已預約此書
	private Mono<LendCallback> onUserBooking(LendCallback lendCallback) {
		char type = 'U';
		List<Phase> availPhases = List.of(Phase.A01_ORDER, Phase.AVAILABLE);
		int readerId = lendCallback.getReaderId();
		int hId = lendCallback.getHoldId();
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.bookingCheckService.correctUniqueBooking(readerId, hId, "C")
				.switchIfEmpty(this.vHoldItemService.getVHoldItemById(hId)
						.filter(vh -> !ANNEX_TYPES.contains(vh.getTypeCode())).map(VHoldItem::getCallVolId)
						.flatMap(cvId -> this.bookingCheckService.correctUniqueBooking(readerId, cvId, "T")))
				.map(bi -> {
					if (!availPhases.contains(bi.getPhase()))
						lendCallback.addCallbackType(type);
					return lendCallback;
				}).defaultIfEmpty(lendCallback)
				.timeout(Duration.ofMillis(3000), Mono.defer(() -> lendCallback.timeout("onUserBooking", type)));
	}

	// 借閱之資料正分配架上找書中(排除借予missing)
	private Mono<LendCallback> onBookingDistribution(LendCallback lendCallback) {
		char type = 'D';
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.bookingCheckService.onBookingDistribution(lendCallback.getHoldId()).filter(b -> b).map(b -> {
			if (!lendCallback.getCallbackTypes().contains('M'))
				lendCallback.addCallbackType(type);
			return lendCallback;
		}).defaultIfEmpty(lendCallback).timeout(Duration.ofMillis(3000),
				Mono.defer(() -> lendCallback.timeout("onBookingDistribution", type)));
	}

	// 容許且僅可借閱"調撥異常"資料借閱者類型之借閱
	private Mono<LendCallback> onCMissingLend(LendCallback lendCallback) {
		// 無須callback
		char type = '7';
		if (lendCallback.isTimeout())
			return Mono.just(lendCallback);
		return this.checkCMissingLend(lendCallback).filter(tuple2 -> tuple2.getT1() ^ tuple2.getT2()).flatMap(
				tup2 -> this.messageMapService.resultPhaseConvert(LENDCHECK, ResultPhase.NOMATCH_CMISSING).map(s -> {
					lendCallback.setCanotLendType(type);
					lendCallback.setReason(s);
					return lendCallback;
				})).defaultIfEmpty(lendCallback)
				.timeout(Duration.ofMillis(3000), Mono.defer(() -> lendCallback.timeout("onCMissingLend", '7')));
	}

	private Mono<Tuple2<Boolean, Boolean>> checkCMissingLend(LendCallback lendCallback) {
		return this.transitOverdaysService.existsNonTouchByHoldId(lendCallback.getHoldId())
				.zipWith(this.userCheckService.checkReaderType(lendCallback.getReaderId(), TRANSIT_OVERDAYS));
	}

	public Mono<LendCallback> onUserLendCallVolIds(LendCallback lendCallback) {// 目前北市圖未check
		// 無須callback
		char type = '8';
		return Flux.fromIterable(this.sqlserverChargedRepository.findByReaderId(lendCallback.getReaderId()))
				.map(SqlserverCharged::getHoldId).flatMap(this.vHoldItemService::getVHoldItemById, 1)
				.map(VHoldItem::getCallVolId).distinct().collectList()
				.zipWith(this.vHoldItemService.getVHoldItemById(lendCallback.getHoldId()).map(VHoldItem::getCallVolId),
						Collection::contains)
				.filter(b -> b).flatMap(b -> this.messageMapService
						.resultPhaseConvert(LENDCHECK, ResultPhase.SAMECALLVOLID_ONLEND).map(s -> {
							lendCallback.setCanotLendType(type);
							lendCallback.setReason(s);
							return lendCallback;
						}))
				.defaultIfEmpty(lendCallback)
				.timeout(Duration.ofMillis(2000), lendCallback.timeout("onUserLendCallVolIds", '9'));
	}

	@Override
	public void lendCallback(String callbackId) {
		this.vLendCallBackService.getLendCallback(callbackId)
				.flatMap(lc -> this.lendLog2Service.saveLendLog2Callback(lc.getLogId()).map(ll2 -> lc))
				.switchIfEmpty(
						Mono.just(new LendCallback()).doOnNext(lc0 -> log.warn("nolendCallback: {}", callbackId)))
				.subscribe(lendCallback -> {
					this.userCheckService.checkReaderType(lendCallback.getReaderId(), LENDCALLBACK_NOFLOAT)
							.filter(b -> b)
							.subscribe(b -> this.amqpBackendClient.setHoldItemTempStatus(lendCallback.getHoldId(), 1));
					List<Character> callbackTypes = lendCallback.getCallbackTypes();
					if (callbackTypes.contains('F')) {
						this.amqpBackendClient.addWhiteUid(lendCallback.getHoldId());
						callbackTypes.remove(Character.valueOf('F'));
					}
					Flux.fromIterable(callbackTypes).filter(TYPES::contains).next()
							.subscribe(type -> this.amqpBackendClient.postBookingLendCallback(lendCallback));
				});
	}

}
