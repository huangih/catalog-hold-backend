package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.VHoldItem;

public interface UserCheckService {

	Mono<VHoldItem> setDependUserRuleStatus(int readerId, VHoldItem vHoldItem);
	
	Mono<Boolean> checkReaderType(int readerId, String ruleName);

}
