package tw.com.hyweb.cathold.backend.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VCallVolHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VMarcHoldSummaryService;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.LendCheck;
import tw.com.hyweb.cathold.model.VHotBookingDate;
import tw.com.hyweb.cathold.model.client.TouchResult;
import tw.com.hyweb.cathold.model.client.VHoldClient;
import tw.com.hyweb.cathold.model.view.CallVolHoldSummary;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmqpBackendService {

	private final LendCheckService lendCheckService;

	private final BookingResultViewService bookingResultViewService;

	private final TouchService touchService;

	private final VHoldClientService vHoldClientService;

	private final VMarcHoldSummaryService vMarcHoldSummaryService;

	private final VCallVolHoldSummaryService vCallVolHoldSummaryService;

	private final AmqpBackendClient amqpBackendClient;

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

	public Mono<LendCheck> readerCanLendHold(Object[] args) {
		int readerId = (int) args[0];
		int holdId = (int) args[1];
		int muserId = (int) args[2];
		String barcode = (String) args[3];
		return this.lendCheckService.readerCanLendHold(readerId, holdId, muserId, barcode);
	}

	public Mono<Void> lendCallback(String callbackId) {
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

	public Mono<CallVolHoldSummary> findCallVolHoldSummaryByCallVolId(Integer[] args) {
		return this.vCallVolHoldSummaryService.findCallVolHoldSummaryByCallVolId(args[0], args[1])
				.timeout(Duration.ofSeconds(20), Mono.just(new CallVolHoldSummary())
						.doOnNext(li -> log.warn("findCallVolHoldSummaryByCallVolId: {}-{}", args[0], args[1])));
	}

	public Mono<HoldClient> addHoldClient(HoldClient holdClient) {
		return this.vHoldClientService.addHoldClient(holdClient);
	}

	public Mono<HoldClient> updateHoldClient(Object... args) {
		HoldClient holdClient = (HoldClient) args[0];
		Integer seqNum = (Integer) args[1];
		return this.vHoldClientService.updateHoldClient(holdClient, seqNum);
	}

	public Mono<List<HoldClient>> getHoldClientsBySiteCode(String siteCode) {
		return this.vHoldClientService.getHoldClientsBySiteCode(siteCode).collectList();
	}

	public Mono<TouchResult> touchHoldItem(Object... args) {
		String barcode = (String) args[0];
		String sessionId = (String) args[1];
		int muserId = (int) args[2];
		return this.touchService.touchHoldItem(barcode, sessionId, muserId);
	}

}
