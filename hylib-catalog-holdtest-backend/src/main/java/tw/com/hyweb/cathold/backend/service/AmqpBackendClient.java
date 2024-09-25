package tw.com.hyweb.cathold.backend.service;

import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;
import tw.com.hyweb.cathold.model.client.TouchResult;

@Component
@Slf4j
public class AmqpBackendClient {

	private static final String FUNC_NAME = "funcName";

	@Value("${cathold.exchange.fanout.name}")
	private String fanoutExchangeName;

	@Value("${cathold.rfidctl.routekey}")
	private String rdRouteKey;

	@Value("${cathold.bookingTransit.routekey}")
	private String btRouteKey;

	private final RabbitTemplate template;

	private final AsyncAmqpTemplate asyncAmqpTemplate;

	private final String callbackQName;

	private final VHoldClientService vHoldClientService;

	public AmqpBackendClient(RabbitTemplate template, AsyncAmqpTemplate asyncAmqpTemplate,
			@Qualifier("tcQueue") Queue tcQueue, VHoldClientService vHoldClientService) {
		this.template = template;
		this.asyncAmqpTemplate = asyncAmqpTemplate;
		this.callbackQName = tcQueue.getName() + "_";
		this.vHoldClientService = vHoldClientService;
	}

	private Message setFuncNameHeader(Message message, String funcName) {
		message.getMessageProperties().getHeaders().put(FUNC_NAME, funcName);
		return message;
	}

	public Mono<TouchResult> touchHoldItemPre(final String barcode, char ctrlChar, String sessionId, String tcId) {
		this.template.convertAndSend(this.rdRouteKey, barcode, m -> this.setFuncNameHeader(m, "subWhiteUid"));
		Object[] args = new Object[] { barcode, ctrlChar, sessionId, this.callbackQName + tcId };
		this.template.convertAndSend(this.fanoutExchangeName, "", args,
				m -> this.setFuncNameHeader(m, "touchHoldItemPre"));
		return Mono.empty();
	}

	public Mono<TouchResult> touchPostProcess(String paramId) {
		return this.vHoldClientService.getTouchCallbackById(paramId).filter(ss -> ss.length == 3).map(ss -> {
			log.info("touchPostProcess: {}-{}-{}", ss[0], ss[1], ss[2]);
			return this.template.convertSendAndReceiveAsType(ss[0], ss[2], m -> this.setFuncNameHeader(m, ss[1]),
					new ParameterizedTypeReference<TouchResult>() {
					});
		});
	}

	public Mono<TradeoffStopBookingResult> tradeoffStopBookingDays(int userId, int tradeoffDays, int muserId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Integer[] { userId, tradeoffDays, muserId },
				m -> this.setFuncNameHeader(m, "tradeoffStopBookingDays"),
				new ParameterizedTypeReference<TradeoffStopBookingResult>() {
				}));
	}

	public void addWhiteUid(String barcode) {
		this.template.convertAndSend(this.rdRouteKey, barcode, m -> this.setFuncNameHeader(m, "addWhiteUid"));
	}

	public void postMissingLend(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback, m -> this.setFuncNameHeader(m, "postMissingLend"));
	}

	public void postAvailBookingLend(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback,
				m -> this.setFuncNameHeader(m, "postAvailBookingLend"));
	}

	public void postBookingAvailRemoveLend(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback,
				m -> this.setFuncNameHeader(m, "postBookingAvailRemoveLend"));
	}

	public void onTransitLend(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback, m -> this.setFuncNameHeader(m, "onTransitLend"));
	}

	public void lendBeforeBookingAvail(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback,
				m -> this.setFuncNameHeader(m, "lendBeforeBookingAvail"));
	}

	public void onBookingDistributionLend(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback,
				m -> this.setFuncNameHeader(m, "onBookingDistributionLend"));
	}

	public Mono<Boolean> checkUidByBarcode(String barcode) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.rdRouteKey, barcode,
				m -> this.setFuncNameHeader(m, "checkUidByBarcode"), new ParameterizedTypeReference<Boolean>() {
				}));
	}

	public Mono<Boolean> onMobileLendSite(String siteCode) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.rdRouteKey, siteCode,
				m -> this.setFuncNameHeader(m, "onMobileLendSite"), new ParameterizedTypeReference<Boolean>() {
				}));
	}

}
