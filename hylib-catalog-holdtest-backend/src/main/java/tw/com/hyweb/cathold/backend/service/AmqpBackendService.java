package tw.com.hyweb.cathold.backend.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import tw.com.hyweb.cathold.backend.redis.service.VCallVolHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VMarcHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchControlService;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendCheck;
import tw.com.hyweb.cathold.model.VHotBookingDate;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.VHoldClient;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmqpBackendService {

	private final LendCheckService lendCheckService;

	private final BookingResultViewService bookingResultViewService;

	private final VTouchControlService vTouchControlService;

	private final VHoldClientService vHoldClientService;

	private final VMarcHoldSummaryService vMarcHoldSummaryService;

	private final VCallVolHoldSummaryService vCallVolHoldSummaryService;

	private final AmqpBackendClient amqpBackendClient;

	public Mono<Void> preTouchCallback(Object[] args) {
		String touchId = (String) args[0];
		PreTouchResult preTouchResult = (PreTouchResult) args[1];
		log.info("preTouchCallback-{}: {}", touchId, preTouchResult);
		return this.vTouchControlService.preTouchCallback(touchId, preTouchResult);
	}

	public Mono<VHoldClient> getVHoldClientById(int holdClientId) {
		return this.vHoldClientService.getVHoldClientById(holdClientId);
	}

	public Mono<Void> refreshMarcHoldSummary(int marcId) {
		this.vMarcHoldSummaryService.deleteMarcHoldSummary(marcId);
		return Mono.empty();
	}

	public Mono<Void> refreshCallVolHoldSummary(int callVolId) {
		this.vCallVolHoldSummaryService.deleteCallVolHoldSummary(callVolId);
		return Mono.empty();
	}

	public Mono<Void> refrshCallVolHoldSummaryByHotBookingDate(VHotBookingDate vHotBookingDate) {
		this.vCallVolHoldSummaryService.refrshByHotBookingDate(vHotBookingDate);
		return Mono.empty();
	}

	public Mono<LendCheck> readerCanLendHold(LendCallback lendCallback) {
		Mono<LendCallback> m0 = this.lendCheckService.prepareLendCheck(lendCallback);
		Mono<Boolean> m1 = this.lendCheckService.checkRfidUidMap(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m2 = this.lendCheckService.lastItemMissing(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m3 = this.lendCheckService.onAvailBooking(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m4 = this.lendCheckService.onTransit(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m5 = this.lendCheckService.onBookingAvailRemove(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m6 = this.lendCheckService.onUserBooking(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m7 = this.lendCheckService.onBookingDistribution(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> m8 = this.lendCheckService.onCMissingLend(lendCallback).map(lendCallback::lendCheck);
		Mono<Boolean> checkMono = Mono.zip(m1, m2, m3, m4, m5, m6, m7, m8).map(tup8 -> tup8.getT1() && tup8.getT2()
				&& tup8.getT3() && tup8.getT4() && tup8.getT5() && tup8.getT6() && tup8.getT7() && tup8.getT8());
		return Mono.zip(m0, checkMono).filter(Tuple2::getT2)
				.flatMap(tup2 -> this.lendCheckService.putLendCallback(tup2.getT1()));
	}

	public Mono<Void> lendCallback(String callbackId) {
		log.info("lendCallback: {}", callbackId);
		this.lendCheckService.lendCallback(callbackId);
		return Mono.empty();
	}

	public Mono<TradeoffStopBookingResultView> tradeoffStopBookingDays(Object[] args) {
		int userId = (int) args[0];
		int tradeoffDays = (int) args[1];
		int muserId = (int) args[2];
		return this.amqpBackendClient.tradeoffStopBookingDays(userId, tradeoffDays, muserId)
				.doOnNext(tsbr -> log.info("{}", tsbr))
				.flatMap(tsbr -> this.bookingResultViewService.conver2TradeoffStopBookingResultView(tsbr, tradeoffDays))
				.doOnNext(tsbrv -> log.info("{}", tsbrv));
	}

}
