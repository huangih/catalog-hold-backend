package tw.com.hyweb.cathold.backend.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchControlService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchLogService;
import tw.com.hyweb.cathold.model.TouchLog;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.TouchControl;
import tw.com.hyweb.cathold.model.client.TouchResult;

@RequiredArgsConstructor
public class TouchClientServiceImpl implements TouchClientService {

	private final ConcurrentMap<String, TouchControl> touchCtrlMap = new ConcurrentHashMap<>();

	@Value("${cathold.holditem.routekey}")
	private String hiRouteKey;

	@Value("${cathold.bookingTransit.routekey}")
	private String btRouteKey;

	private final VTouchControlService vTouchControlService;

	private final AmqpBackendClient amqpBackendClient;

	private final VHoldClientService vHoldClientService;

	private final VTouchLogService vTouchLogService;

	private final List<Character> prefixChars = List.of('-', '/', '!');

	@Override
	public Mono<TouchResult> touchHoldItem(String barcode, String sessionId, int muserId) {
		return this.vHoldClientService.getHoldClientBySessionId(sessionId)
				.flatMap(hc -> this.vTouchControlService.newTouchControl(barcode, hc, muserId)
						.flatMap(tc -> this.touchHoldItemPre(sessionId, tc.getTouchControlId())
								.switchIfEmpty(this.touchHoldItem(tc))))
				.switchIfEmpty(this.touchError("cathold.touch.nosuchClientId"));
	}

	private Mono<TouchResult> touchHoldItem(TouchControl touchControl) {
		return Mono.fromFuture(touchControl.waitPreReady())
				.timeout(Duration.ofSeconds(2), Mono.defer(() -> Mono.just(touchControl.waitPostMapTimeoutN())))
				.doOnNext(this.vTouchControlService::updatePreTime).flatMap(tc -> {
					Map<Integer, String> map = tc.getPreMap();
					if (map.isEmpty())
						return this.touchError("cathold.touch.wrongTouchPreCallback");
					int key = Collections.max(map.keySet());
					if (key == (1 << 16))
						return this.touchError("cathold.touch.wrongBarcode");
					if (key == (1 << 17))
						return this.touchError("cathold.touch.preTouchTimeout");
					return this.amqpBackendClient.touchPostProcess(map.get(key))
							.doOnNext(tr -> this.vTouchControlService.updateResultTime(tr, tc));
				});
	}

	private Mono<TouchResult> touchHoldItemPre(String sessionId, String tcId) {
		String barcode = tcId.split("#")[0].split("\\|")[1];
		char ctrlChar = 0;
		if (this.prefixChars.contains(barcode.charAt(0))) {
			ctrlChar = barcode.charAt(0);
			barcode = barcode.substring(1);
		}
		if ('!' == ctrlChar)
			return this.vTouchLogService.rollbackHoldItem(barcode, Integer.parseInt(sessionId.split("_")[0]))
					.flatMap(obj -> {
						if (obj instanceof TouchLog tl) {
							int muserId = 0;
							muserId = Integer.parseInt(tcId.split("#")[1]);
							return this.amqpBackendClient.rollBackHoldItem(sessionId, tl, muserId);
						}
						if (obj instanceof TouchResult tr)
							return Mono.just(tr);
						return Mono.empty();
					});
		return this.amqpBackendClient.touchHoldItemPre(barcode, ctrlChar, sessionId, tcId);
	}

	@Override
	public void preTouchCallback(String touchId, PreTouchResult preTouchResult) {
		if (this.touchCtrlMap.containsKey(touchId))
			this.touchCtrlMap.get(touchId).preTouchCallback(preTouchResult);
	}

	private Mono<TouchResult> touchError(String msg) {
		TouchResult touchResult = new TouchResult('E', "error");
		touchResult.setResultClass(String.class);
		touchResult.setResultObject(msg);
		return Mono.just(touchResult);
	}

}
