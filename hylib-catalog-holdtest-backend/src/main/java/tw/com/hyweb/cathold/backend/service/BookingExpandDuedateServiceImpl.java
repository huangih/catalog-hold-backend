package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.ReactiveRedisUtils;
import tw.com.hyweb.cathold.model.BookingExpandDuedate;

@RequiredArgsConstructor
public class BookingExpandDuedateServiceImpl implements BookingExpandDuedateService {

	private static final String BOOKINGEXPANDDUEDATE_RWLOCK = "/cathold/booking/expandDuedate/";

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<List<BookingExpandDuedate>> getExpandDuedatesOnMonth(int readerId) {
		var today = LocalDate.now();
		LocalDateTime begMonth = LocalDate.of(today.getYear(), today.getMonth(), 1).atStartOfDay();
		return this.redisUtils.getMonoFromReadLock(BOOKINGEXPANDDUEDATE_RWLOCK + readerId,
				() -> this.calVolTemplate
						.select(query(where("readerId").is(readerId).and("createTime").greaterThan(begMonth)),
								BookingExpandDuedate.class)
						.collectList());
	}

	@Override
	public Mono<Integer> getExpandDuedatesOnMonthNum(int readerId) {
		var today = LocalDate.now();
		LocalDateTime begMonth = LocalDate.of(today.getYear(), today.getMonth(), 1).atStartOfDay();
		return this.redisUtils.getMonoFromReadLock(BOOKINGEXPANDDUEDATE_RWLOCK + readerId,
				() -> this.calVolTemplate
						.count(query(where("readerId").is(readerId).and("createTime").greaterThan(begMonth)),
								BookingExpandDuedate.class)
						.map(Long::intValue));
	}

}
