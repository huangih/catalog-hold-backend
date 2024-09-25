package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;

public interface TransitOverdaysService {

	Mono<Boolean> existsNonTouchByHoldId(int holdId);

}
