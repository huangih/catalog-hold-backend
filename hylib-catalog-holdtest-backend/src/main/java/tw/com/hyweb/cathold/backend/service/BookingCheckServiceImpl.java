package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;

import java.util.Comparator;

import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.BookingDistribution;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.VBookingAvailRemove;
import tw.com.hyweb.cathold.model.VBookingAvailation;
import tw.com.hyweb.cathold.model.VBookingAvailationHistory;
import tw.com.hyweb.cathold.model.VIntransitBooking;

@RequiredArgsConstructor
public class BookingCheckServiceImpl implements BookingCheckService {

	private static final String HOLD_ID = "holdId";

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<Integer> onAvailBooking(int holdId) {
		return this.calVolTemplate.selectOne(query(where(HOLD_ID).is(holdId)), VBookingAvailation.class)
				.map(VBookingAvailation::getUserId);
	}

	@Override
	public Mono<VIntransitBooking> onTransitBooking(int holdId) {
		return this.calVolTemplate.selectOne(query(where(HOLD_ID).is(holdId)), VIntransitBooking.class);
	}

	@Override
	public Mono<Boolean> onBookingAvailRemove(int holdId) {
		return this.calVolTemplate
				.exists(query(where(HOLD_ID).is(holdId).and("phase").is(Phase.OVERDUE_BOOKING_WAITING)),
						VBookingAvailationHistory.class)
				.zipWith(this.calVolTemplate.exists(query(where(HOLD_ID).is(holdId).and("removeDate").isNull()),
						VBookingAvailRemove.class), (b1, b2) -> b1 || b2);
	}

	@Override
	public Mono<Booking> correctUniqueBooking(int readerId, int itemId, String type) {
		return this.calVolTemplate
				.select(query(where("userId").is(readerId).and("itemId").is(itemId).and("type").is(type)),
						Booking.class)
				.sort(Comparator.comparing(Booking::getPlaceDate)).collectList().filter(li -> !li.isEmpty()).map(li -> {
					Booking booking = li.removeFirst();
					Flux.fromIterable(li).subscribe(bi -> this.calVolTemplate.delete(bi).subscribe());
					return booking;
				});
	}

	@Override
	public Mono<Boolean> onBookingDistribution(int holdId) {
		return this.calVolTemplate.exists(query(where(HOLD_ID).is(holdId)), BookingDistribution.class);
	}

}
