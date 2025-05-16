package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.client.TouchResult;

public interface TouchService {

	Mono<TouchResult> touchHoldItem(String barcode, String sessionId, int muserId);

}
