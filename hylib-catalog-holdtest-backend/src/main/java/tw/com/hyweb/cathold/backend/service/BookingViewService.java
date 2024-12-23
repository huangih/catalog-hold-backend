package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.BookingHistory;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.view.BookingHistoryView;
import tw.com.hyweb.cathold.model.view.BookingNclView;
import tw.com.hyweb.cathold.model.view.BookingView;

public interface BookingViewService {

	Flux<BookingView> getAllBookingViewsByReaderId(int readerId);

	Flux<BookingView> getBookingViewsByReaderId(int readerId, boolean isAvailation);

	Flux<BookingHistory> findBookingHistoriesByUserId(int readerId, boolean onlyOverdue, boolean overNotAvail);

	Mono<BookingView> convert2BookingView(Booking booking);

	Mono<BookingHistoryView> convert2BookingView(BookingHistory bookingHistory);

	Flux<BookingHistoryView> getReaderOnStopBookingHistories(int readerId, Phase onStopBooking);

	Mono<BookingHistoryView> convert2ExpandBookingView(long bookingId);

	Mono<BookingNclView> convert2BookingNclView(Booking booking);

}
