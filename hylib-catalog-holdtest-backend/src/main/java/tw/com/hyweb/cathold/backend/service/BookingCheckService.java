package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.VIntransitBooking;

public interface BookingCheckService {

	Mono<Integer> onAvailBooking(int holdId);

	Mono<VIntransitBooking> onTransitBooking(int holdId);

	Mono<Boolean> onBookingAvailRemove(int holdId);

	Mono<Booking> correctUniqueBooking(int readerId, int itemId, String type);

	Mono<Boolean> onBookingDistribution(int holdId);

}
