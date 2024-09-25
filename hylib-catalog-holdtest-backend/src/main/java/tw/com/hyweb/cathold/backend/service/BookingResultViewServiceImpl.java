package tw.com.hyweb.cathold.backend.service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;
import tw.com.hyweb.cathold.model.view.ReaderStopBookingInfo;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;

@RequiredArgsConstructor
public class BookingResultViewServiceImpl implements BookingResultViewService {

	private final UserStopBookingService userStopBookingService;

	private final MessageMapService messageMapService;

	@Override
	public Mono<TradeoffStopBookingResultView> conver2TradeoffStopBookingResultView(
			TradeoffStopBookingResult tradeoffStopBookingResult, int reqTradeoffDays) {
		return this.userStopBookingService.getReaderStopBookingInfo(tradeoffStopBookingResult.getUserId())
				.defaultIfEmpty(new ReaderStopBookingInfo()).zipWith(this.messageMapService.resultPhaseConvert(
						"TradeoffStopBooking", tradeoffStopBookingResult.getResultPhase()), (rsbi, s) -> {
							TradeoffStopBookingResultView tsbrv = new TradeoffStopBookingResultView(
									tradeoffStopBookingResult, reqTradeoffDays, rsbi);
							tsbrv.setResultCode(s);
							return tsbrv;
						});
	}

}
