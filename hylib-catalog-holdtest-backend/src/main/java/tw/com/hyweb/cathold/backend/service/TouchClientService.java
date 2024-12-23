package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.TouchResult;

public interface TouchClientService {

	Mono<TouchResult> touchHoldItem(String barcode, String sessionId, int muserId);

	void preTouchCallback(String touchId, PreTouchResult preTouchResult);

}
