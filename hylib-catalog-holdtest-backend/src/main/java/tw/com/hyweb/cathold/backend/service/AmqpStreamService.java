package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AmqpStreamService {

	Mono<String> createStreamQueue(String prefix);

	<T> void prduceAndSendStream(String queueName, Flux<T> sendFlux);

	Flux<Object> createListenerContainer(String queueName);

	void deleteStream(String queueName);

}
