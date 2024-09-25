package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;

public interface BookingResultViewService {

	Mono<TradeoffStopBookingResultView> conver2TradeoffStopBookingResultView(
			TradeoffStopBookingResult tradeoffStopBookingResult, int reqTradeoffDays);

}
