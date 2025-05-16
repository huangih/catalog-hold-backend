package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.UserSuspendBooking;

public interface UserSuspendBookingService {

	Mono<UserSuspendBooking> getReaderSuspendBooking(int readerId);

}
