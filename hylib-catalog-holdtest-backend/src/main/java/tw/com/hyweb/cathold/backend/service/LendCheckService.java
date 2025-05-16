package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendCheck;

public interface LendCheckService {

	Mono<LendCheck> readerCanLendHold(int readerId, int holdId, int muserId, String barcode);

	Mono<LendCallback> onUserLendCallVolIds(LendCallback lendCallback); // 是否為借閱中同一callvolId的書

	void lendCallback(String callbackId);

}
