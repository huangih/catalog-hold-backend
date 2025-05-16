package tw.com.hyweb.cathold.backend.service;

import java.util.List;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.BookingExpandDuedate;

public interface BookingExpandDuedateService {

	Mono<List<BookingExpandDuedate>> getExpandDuedatesOnMonth(int readerId);

	Mono<Integer> getExpandDuedatesOnMonthNum(int readerId);

}
