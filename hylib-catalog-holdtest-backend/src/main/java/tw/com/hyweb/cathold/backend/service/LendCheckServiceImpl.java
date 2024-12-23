package tw.com.hyweb.cathold.backend.service;

import java.time.Duration;
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

	private static final List<String> ANNEX_TYPES = List.of("BA", "AA", "BDA", "HOT-BA");

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
	public Mono<LendCheck> checkRfidUidMap(LendCallback lendCallback, String barcode) {
		char type = 'F';
		LendCheck lendCheck = new LendCheck('0');
		if (barcode != null && barcode.length() > 0) {
			lendCheck.setType(type);
			return this.amqpBackendClient.checkUidByBarcode(barcode).filter(b -> b)
					.flatMap(b -> this.vHoldItemService.getVHoldItemByBarcode(barcode).map(VHoldItem::getSiteCode)
							.flatMap(this.amqpBackendClient::onMobileLendSite).filter(b1 -> b1).map(b1 -> "")
							.switchIfEmpty(this.messageMapService.resultPhaseConvert("RFIDCheck",
									ResultPhase.NOT_MOBILELEND_SITE)))
					.switchIfEmpty(this.messageMapService.resultPhaseConvert("RFIDCheck", ResultPhase.NOTON_UIDMAP))
					.map(s -> {
						if (s.length() > 0) {
							lendCheck.setCanLend(false);
							lendCheck.setReason(s);
						}
						return lendCheck;
					}).timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("checkRfidUidMap", type)));
		}
		return Mono.just(lendCheck);
	}

	@Override
	public Mono<LendCheck> lastItemMissing(LendCallback lendCallback) {
		char type = 'M';
		LendCheck lendCheck = new LendCheck('1');
		return this.userCheckService.checkReaderType(lendCallback.getReaderId(), LASTITEM_READERTYPE).filter(b -> b)
				.flatMap(b -> {
					lendCheck.setType(type);
					return this.vHoldItemService.getVHoldItemById(lendCallback.getHoldId()).map(VHoldItem::getCallVolId)
							.flatMap(this.vCallVolHoldSummaryService::findCallVolHoldSummaryByCallVolId)
							.filter(chs -> chs.getAllowBookingNum() <= WARN_RESTNUM && chs.getWaitBookingNum() > 0)
							.flatMap(chs -> this.messageMapService
									.resultPhaseConvert(LENDCHECK, ResultPhase.LASITEM_ALLOWBOOKING).map(s -> {
										lendCheck.setCanLend(false);
										lendCheck.setReason(s);
										return lendCheck;
									}));
				}).defaultIfEmpty(lendCheck)
				.timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("lastItemMissing", type)));

	}

	@Override
	public Mono<LendCheck> onAvailBooking(LendCallback lendCallback) {
		char type = 'B';
		LendCheck lendCheck = new LendCheck('2');
		return this.bookingCheckService.onAvailBooking(lendCallback.getHoldId()).flatMap(userId -> {
			lendCheck.setType(type);
			if (lendCallback.getReaderId() != userId)
				return this.messageMapService.resultPhaseConvert(LENDCHECK, ResultPhase.NOT_BOOKING_USER).map(s -> {
					lendCheck.setCanLend(false);
					lendCheck.setReason(s);
					return lendCheck;
				});
			return Mono.just(lendCheck);
		}).defaultIfEmpty(lendCheck).timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("onAvailBooking", type)));
	}

	@Override
	public Mono<LendCheck> onTransit(LendCallback lendCallback) {
		char type = 'T';
		LendCheck lendCheck = new LendCheck('3');
		List<Phase> phases = List.of(Phase.WAIT_TRANSITB, Phase.TRANSIT_B);
		return this.bookingCheckService.onTransitBooking(lendCallback.getHoldId())
				.zipWith(this.checkCMissingLend(lendCallback).map(tup2 -> tup2.getT1() && tup2.getT2()))
				.flatMap(tup2 -> {
					lendCheck.setType(type);
					VIntransitBooking vib = tup2.getT1();
					if (vib.getUserId() == null)
						log.warn("onTransit-vib: {}", vib);
					else if (phases.contains(vib.getPhase()) && lendCallback.getReaderId() != vib.getUserId()
							&& Boolean.FALSE.equals(tup2.getT2()))
						return this.messageMapService.resultPhaseConvert(LENDCHECK, ResultPhase.HOLD_ON_TRANSIT_B)
								.map(s -> {
									lendCheck.setCanLend(false);
									lendCheck.setReason(s);
									return lendCheck;
								});
					return Mono.just(lendCheck);
				}).defaultIfEmpty(lendCheck)
				.timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("onTransit", type)));
	}

	@Override // 預約待取撤架,若借閱者為原逾期未取者，取消待定記點，否則待定改為記點，可暫訂可借，依接的check
	public Mono<LendCheck> onBookingAvailRemove(LendCallback lendCallback) {
		char type = 'O';
		LendCheck lendCheck = new LendCheck('4');
		return this.bookingCheckService.onBookingAvailRemove(lendCallback.getHoldId()).filter(b -> b).map(b -> {
			lendCheck.setType(type);
			return lendCheck;
		}).defaultIfEmpty(lendCheck).timeout(Duration.ofMillis(3000),
				Mono.just(new LendCheck("onBookingAvailRemove", type)));
	}

	@Override
	public Mono<LendCheck> onUserBooking(LendCallback lendCallback) {
		char type = 'U';
		LendCheck lendCheck = new LendCheck('5');
		List<Phase> availPhases = List.of(Phase.A01_ORDER, Phase.AVAILABLE);
		int readerId = lendCallback.getReaderId();
		int hId = lendCallback.getHoldId();
		return this.bookingCheckService.correctUniqueBooking(readerId, hId, "C")
				.switchIfEmpty(this.vHoldItemService.getVHoldItemById(hId)
						.filter(vh -> !ANNEX_TYPES.contains(vh.getTypeCode())).map(VHoldItem::getCallVolId)
						.flatMap(cvId -> this.bookingCheckService.correctUniqueBooking(readerId, cvId, "T")))
				.map(bi -> {
					if (!availPhases.contains(bi.getPhase()))
						lendCheck.setType(type);
					return lendCheck;
				}).defaultIfEmpty(lendCheck)
				.timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("onUserBooking", type)));
	}

	@Override
	public Mono<LendCheck> onBookingDistribution(LendCallback lendCallback) {
		char type = 'D';
		LendCheck lendCheck = new LendCheck('6');
		return this.bookingCheckService.onBookingDistribution(lendCallback.getHoldId()).filter(b -> b).map(b -> {
			lendCheck.setType(type);
			return lendCheck;
		}).defaultIfEmpty(lendCheck).timeout(Duration.ofMillis(3000),
				Mono.just(new LendCheck("onBookingDistribution", type)));
	}

	@Override
	public Mono<LendCheck> onCMissingLend(LendCallback lendCallback) {
		// 無須callback
		LendCheck lendCheck = new LendCheck('7');
		return this.checkCMissingLend(lendCallback).filter(tuple2 -> tuple2.getT1() ^ tuple2.getT2()).flatMap(
				tup2 -> this.messageMapService.resultPhaseConvert(LENDCHECK, ResultPhase.NOMATCH_CMISSING).map(s -> {
					lendCheck.setCanLend(false);
					lendCheck.setReason(s);
					return lendCheck;
				})).defaultIfEmpty(lendCheck)
				.timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("onCMissingLend", '7')));
	}

	private Mono<Tuple2<Boolean, Boolean>> checkCMissingLend(LendCallback lendCallback) {
		return this.transitOverdaysService.existsNonTouchByHoldId(lendCallback.getHoldId())
				.zipWith(this.userCheckService.checkReaderType(lendCallback.getReaderId(), TRANSIT_OVERDAYS));
	}

	@Override
	public Mono<LendCheck> onUserLendCallVolIds(LendCallback lendCallback) {// 目前北市圖未check
		// 無須callback
		LendCheck lendCheck = new LendCheck('8');
		return Flux.fromIterable(this.sqlserverChargedRepository.findByReaderId(lendCallback.getReaderId()))
				.map(SqlserverCharged::getHoldId).flatMap(this.vHoldItemService::getVHoldItemById, 1)
				.map(VHoldItem::getCallVolId).distinct().collectList()
				.zipWith(this.vHoldItemService.getVHoldItemById(lendCallback.getHoldId()).map(VHoldItem::getCallVolId),
						Collection::contains)
				.filter(b -> b).flatMap(b -> this.messageMapService
						.resultPhaseConvert(LENDCHECK, ResultPhase.SAMECALLVOLID_ONLEND).map(s -> {
							lendCheck.setCanLend(false);
							lendCheck.setReason(s);
							return lendCheck;
						}))
				.defaultIfEmpty(lendCheck)
				.timeout(Duration.ofMillis(3000), Mono.just(new LendCheck("onUserLendCallVolIds", '9')));
	}

//	@Override
//	public Mono<LendCheck> putLendCallback(LendCallback lendCallback) {
//		return this.vLendCallBackService.prepareLendCallback(lendCallback)
//				.doOnNext(lc -> this.lendLog2Service.saveLendLog2PreCheck(lendCallback));
//	}
//
	@Override
	public void lendCallback(String callbackId) {
		this.vLendCallBackService.getLendCallback(callbackId)
				.flatMap(lc -> this.lendLog2Service.saveLendLog2Callback(lc.getLogId()).map(ll2 -> lc))
				.switchIfEmpty(
						Mono.just(new LendCallback()).doOnNext(lc0 -> log.warn("nolendCallback: {}", callbackId)))
				.subscribe(lendCallback -> Flux.fromIterable(lendCallback.getCallbackTypes()).subscribe(type -> {
					switch (type) {
					case 'F' -> this.amqpBackendClient.addWhiteUid(lendCallback.getHoldId());
					case 'M' -> this.amqpBackendClient.postMissingLend(lendCallback);
					case 'B' -> this.amqpBackendClient.postAvailBookingLend(lendCallback);
					case 'O' -> this.amqpBackendClient.postBookingAvailRemoveLend(lendCallback);
					case 'T' -> this.amqpBackendClient.onTransitLend(lendCallback);
					case 'U' -> this.amqpBackendClient.lendBeforeBookingAvail(lendCallback);
					case 'D' -> this.amqpBackendClient.onBookingDistributionLend(lendCallback);
					default -> throw new IllegalArgumentException("Unexpected value: " + type);
					}
				}));
	}

}
