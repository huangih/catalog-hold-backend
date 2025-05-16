package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.BookingResult;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;
import tw.com.hyweb.cathold.model.view.BookingResultView;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;
import tw.com.hyweb.cathold.model.view.UserBookingResultView;

public interface BookingResultViewService {

	Mono<BookingResultView> convert2BookingResultView(BookingResult bookingResult);

	Mono<UserBookingResultView> convert2UserBookingResultView(BookingResult bookingResult);

	Mono<TradeoffStopBookingResultView> conver2TradeoffStopBookingResultView(
			TradeoffStopBookingResult tradeoffStopBookingResult, int reqTradeoffDays);

}
