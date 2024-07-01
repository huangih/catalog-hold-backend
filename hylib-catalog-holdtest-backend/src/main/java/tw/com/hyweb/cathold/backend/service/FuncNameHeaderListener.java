package tw.com.hyweb.cathold.backend.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FuncNameHeaderListener extends MessageListenerAdapter {

	public FuncNameHeaderListener(Object delegate, MessageConverter messageConverter) {
		super(delegate, messageConverter);
	}

	@Override
	protected String getListenerMethodName(Message originalMessage, Object extractedMessage) {
		return (String) originalMessage.getMessageProperties().getHeader("funcName");
	}

}
