package tw.com.hyweb.cathold.backend.service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.BookingHistory;
import tw.com.hyweb.cathold.model.BookingResult;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;
import tw.com.hyweb.cathold.model.view.BookingResultView;
import tw.com.hyweb.cathold.model.view.ReaderStopBookingInfo;
import tw.com.hyweb.cathold.model.view.TradeoffStopBookingResultView;
import tw.com.hyweb.cathold.model.view.UserBookingResultView;

@RequiredArgsConstructor
public class BookingResultViewServiceImpl implements BookingResultViewService {

	private final BookingViewService bookingViewService;

	private final UserStopBookingService userStopBookingService;

	private final MessageMapService messageMapService;

	@Override
	public Mono<BookingResultView> convert2BookingResultView(BookingResult bookingResult) {
		return this.messageMapService.resultPhaseConvert("BookingResult", bookingResult.getResultPhase()).flatMap(s -> {
			BookingResultView bookingResultView = new BookingResultView(bookingResult.getId(), s);
			Class<?> clazz = bookingResult.getResultClass();
			Object result = bookingResult.getResult();
			if (clazz != null && clazz.isInstance(result)) {
				if (result instanceof Booking bi)
					this.bookingViewService.convert2BookingView(bi).map(bv -> {
						bookingResultView.setBooking(bv);
						return bookingResultView;
					});
				if (result instanceof BookingHistory bh)
					this.bookingViewService.convert2BookingView(bh).map(bhv -> {
						bookingResultView.setBookingHistory(bhv);
						return bookingResultView;
					});
			}
			return Mono.just(bookingResultView);
		});

	}

	@Override
	public Mono<UserBookingResultView> convert2UserBookingResultView(BookingResult bookingResult) {
		// TODO Auto-generated method stub
		return null;
	}

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
