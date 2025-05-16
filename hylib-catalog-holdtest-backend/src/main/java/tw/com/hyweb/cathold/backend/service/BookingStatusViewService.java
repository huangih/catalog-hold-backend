package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Flux;
import tw.com.hyweb.cathold.model.view.BookingStatusView;

public interface BookingStatusViewService {

	Flux<BookingStatusView> getBookingStatuses(int rate, int regNum, int supNum, int waitDays2);

}
