package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.UserSuspendBooking;

@RequiredArgsConstructor
public class UserSuspendBookingServiceImpl implements UserSuspendBookingService {

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<UserSuspendBooking> getReaderSuspendBooking(int readerId) {
	return	this.calVolTemplate.select(query(where("userId").is(readerId)).sort(Sort.by(Direction.DESC, "id")),
				UserSuspendBooking.class).next();
	}

}
