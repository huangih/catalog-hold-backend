package tw.com.hyweb.cathold.backend.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Slf4j
public class AmqpStreamServiceImpl implements AmqpStreamService {

	private final String appId = RandomString.make();

	private final MessageConverter messageConverter;

	private final Environment environment;

	private final Map<String, StreamListenerContainer> containerMap = new HashMap<>();

	@Override
	public Mono<String> createStreamQueue(String prefix) {
		String queueName = "getbookingViewsForNcl.stream-" + RandomString.make();
		this.environment.streamCreator().name(queueName).maxAge(Duration.ofMinutes(60)).create();
		return Mono.just(queueName);
	}

	@Override
	public <T> void prduceAndSendStream(String queueName, Flux<T> sendFlux) {
		try (RabbitStreamTemplate streamTemplate = new RabbitStreamTemplate(this.environment, queueName)) {
			streamTemplate.setMessageConverter(this.messageConverter);
			sendFlux.publishOn(Schedulers.immediate(), 1).map(streamTemplate::convertAndSend).count()
					.subscribe(size -> {
						CompletableFuture<Boolean> future = streamTemplate.convertAndSend(size);
						log.info("prduceAndSendStream complete: {}-{}", size, future.join());
						streamTemplate.close();
					});
		}
	}

	@Override
	public Flux<Object> createListenerContainer(String queueName) {
		StreamListenerContainer container = new StreamListenerContainer(this.environment);
		container.setQueueNames(queueName);
		container.setListenerId("backend-" + appId);
		container.setConsumerCustomizer((id, builder) -> builder.offset(OffsetSpecification.first()));
		containerMap.put(queueName, container);
		return Flux.create(sink -> {
			container.setupMessageListener(msg -> {
				Object obj = messageConverter.fromMessage(msg);
				if (obj instanceof Long n) {
					log.info("completed: {}", n);
					sink.complete();
				} else
					sink.next(obj);
			});
			container.start();
		});
	}

	@Override
	public void deleteStream(String queueName) {
		StreamListenerContainer container = this.containerMap.remove(queueName);
		if (container != null)
			container.stop();
		this.environment.deleteStream(queueName);
	}

}
