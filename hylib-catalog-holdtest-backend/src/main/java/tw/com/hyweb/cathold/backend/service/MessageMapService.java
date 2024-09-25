package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.ResultPhase;

public interface MessageMapService {

	Mono<String> resultPhaseConvert(String type, ResultPhase resultPhase);

}
