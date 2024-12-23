package tw.com.hyweb.cathold.backend.configuration;

import java.util.Map;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AsyncAmqpTemplate;
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
import net.bytebuddy.utility.RandomString;
import tw.com.hyweb.cathold.backend.service.AmqpBackendService;
import tw.com.hyweb.cathold.backend.service.FuncNameHeaderListener;

@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:cathold-routekey.yml")
public class AmqpConfiguration {

	@Value("${cathold.exchange.name}")
	private String exchangeName;

	@Value("${cathold.exchange.fanout.name}")
	private String fanoutExchangeName;

	@Value("${cathold.exchange.stream.name}")
	private String streamExchangeName;

	@Value("${cathold.backend.routekey}")
	private String beRouteKey;

	@Bean
	@Primary
	DirectExchange directExchange() {
		return new DirectExchange(exchangeName);
	}

	@Bean
	FanoutExchange fanoutExchange() {
		return new FanoutExchange(fanoutExchangeName);
	}

	@Bean
	DirectExchange streamExchange() {
		return new DirectExchange(this.streamExchangeName, true, false, Map.of("x-super-stream", true));
	}

	@Bean
	@Primary
	Queue beQueue() {
		return QueueBuilder.durable(beRouteKey).quorum().build();
	}

	@Bean
	Binding beBinding(Queue queue, DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).withQueueName();
	}

	@Bean
	Queue tcQueue() {
		String name = "backend-" + RandomString.make();
		return QueueBuilder.durable(name).autoDelete().build();
	}

	@Bean
	Binding tcBinding(@Qualifier("tcQueue") Queue queue, DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).withQueueName();
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
		template.setReplyTimeout(20000);
		template.setUsePublisherConnection(true);
		template.setUseChannelForCorrelation(true);
		return template;
	}

	@Bean
	AsyncAmqpTemplate asyncTemplate(RabbitTemplate template) {
		return new AsyncRabbitTemplate(template);
	}

	@Bean
	MessageListenerContainer listenerContainer(ConnectionFactory factory, MessageConverter messageConverter,
			AmqpBackendService amqpBackendService, Queue queue, @Qualifier("tcQueue") Queue tcQueue) {
		var container = new DirectMessageListenerContainer(factory);
		var listener = new FuncNameHeaderListener(amqpBackendService, messageConverter);
		container.setConsumersPerQueue(5);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		container.addQueues(queue, tcQueue);
		container.setMessageListener(listener);
		return container;
	}

}
