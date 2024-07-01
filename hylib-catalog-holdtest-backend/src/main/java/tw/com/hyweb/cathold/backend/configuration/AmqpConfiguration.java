package tw.com.hyweb.cathold.backend.configuration;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

import tw.com.hyweb.cathold.backend.service.AmqpBackendService;
import tw.com.hyweb.cathold.backend.service.FuncNameHeaderListener;

@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:cathold-routekey.yml")
public class AmqpConfiguration {

	@Value("${cathold.exchange.name}")
	private String exchangeName;

	@Value("${cathold.exchange.fanout.name}")
	private String fanoutExchangeName;

	@Value("${cathold.bookingTransit.routekey}")
	private String biRouteKey;

	@Value("${cathold.bookingTransit.touch.queue-name}")
	private String tbiQueueName;

	@Bean
	DirectExchange directExchange() {
		return new DirectExchange(exchangeName);
	}

	@Bean
	@Primary
	Queue biQueue() {
		return QueueBuilder.durable(biRouteKey).quorum().build();
	}

	@Bean
	Binding biBinding(Queue queue, DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).withQueueName();
	}

	@Bean
	FanoutExchange fanoutExchange() {
		return new FanoutExchange(fanoutExchangeName);
	}

	@Bean
	Queue tbiQueue() {
		return QueueBuilder.durable(tbiQueueName).quorum().build();
	}

	@Bean
	Binding tbiBinding(@Qualifier("tbiQueue") Queue queue, FanoutExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange);
	}

	@Bean
	MessageConverter messageConverter(ObjectMapper objectMapper) {
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	@Bean
	RabbitTemplate template(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		var template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		template.setExchange(exchangeName);
		template.setReplyTimeout(2000000);
		template.setUsePublisherConnection(true);
		template.setUseChannelForCorrelation(true);
		return template;
	}

	@Bean
	AsyncRabbitTemplate asyncTemplate(RabbitTemplate template) {
		var asyncTemplate = new AsyncRabbitTemplate(template);
		asyncTemplate.setEnableConfirms(true);
		return asyncTemplate;
	}

	@Bean
	MessageListenerContainer listenerContainer(ConnectionFactory factory, MessageConverter messageConverter,
			AmqpBackendService amqpBookingService, Queue queue, @Qualifier("tbiQueue") Queue tbiQueue) {
		var container = new DirectMessageListenerContainer(factory);
		var listener = new FuncNameHeaderListener(amqpBookingService, messageConverter);
		container.setConsumersPerQueue(50);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		container.addQueues(queue, tbiQueue);
		container.setMessageListener(listener);
		return container;
	}

}
