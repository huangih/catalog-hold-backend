package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.view.BookingStatusView;
import tw.com.hyweb.cathold.model.view.TodayBookingInfo;

public interface BookingStatusViewService {

	Flux<BookingStatusView> getBookingStatuses(int rate, int regNum, int supNum, int waitDays2);

	Mono<TodayBookingInfo> getTodayBookingInfo();

}
