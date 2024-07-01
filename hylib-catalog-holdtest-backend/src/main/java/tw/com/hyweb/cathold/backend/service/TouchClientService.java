package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.TouchResult;

public interface TouchClientService {

	Mono<TouchResult> touchHoldItem(Mono<Tuple3<String, String, Integer>> args);

	void preTouchCallback(String touchId, PreTouchResult preTouchResult);

}
