package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.view.ReaderStopBookingInfo;

public interface UserStopBookingService {

	Mono<ReaderStopBookingInfo> getReaderStopBookingInfo(int readerId);

}
