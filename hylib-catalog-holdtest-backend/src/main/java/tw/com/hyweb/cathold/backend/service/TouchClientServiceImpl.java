package tw.com.hyweb.cathold.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.TouchControl;
import tw.com.hyweb.cathold.model.client.TouchResult;

@RequiredArgsConstructor
public class TouchClientServiceImpl implements TouchClientService {

	private final ConcurrentMap<String, TouchControl> touchCtrlMap = new ConcurrentHashMap<>();

	@Value("${cathold.holditem.routekey}")
	private String hiRouteKey;

	@Value("${cathold.bookingTransit.routekey}")
	private String biRouteKey;

	@Value("${cathold.intransit.routekey}")
	private String itRouteKey;

//	private final VTouchLogService vTouchLogService;
//	
//	private final VHoldClientService vHoldClientService;
//	
	private List<Character> prefixChars;

	private int enableNum = 0;

	@PostConstruct
	public void initParameters() {
		this.prefixChars = Arrays.asList('-', '/', '!');
		for (int enable : Arrays.asList(1, 3, 5, 7, 9))
			this.enableNum -= 1 << enable;
	}

	@Override
	public Mono<TouchResult> touchHoldItem(Mono<Tuple3<String, String, Integer>> args) {
//		args.flatMap(tuple3-> {
//			this.vHoldClientService.getHoldClientBySessionId(tuple3.getT2());
//		});
//		if (this.vHoldClientService.getHoldClientBySessionId(sessionId) != null && barcode.length() > 0) {
//			int clientId = Integer.parseInt(sessionId.split("_")[0]);
//			int touchLogId = this.vTouchLogService.newTouchLog(barcode, clientId);
//			LocalTime beg = LocalTime.now();
//			String tcId = RandomString.make() + "|" + barcode + "#" + muserId;
//			TouchControl touchCtrl = new TouchControl(this.enableNum);
//			touchCtrlMap.put(tcId, touchCtrl);
//			char ctrlChar = 0;
//			if (this.prefixChars.contains(barcode.charAt(0))) {
//				ctrlChar = barcode.charAt(0);
//				barcode = barcode.substring(1);
//			}
//			if (ctrlChar == '!') {
//				TouchResult tResult = this.vTouchLogService.rollbackHoldItem(barcode, clientId);
//				if (tResult != null)
//					return tResult;
//			}
//			this.amqpGraphQLClient.touchHoldItemPre(barcode, ctrlChar, sessionId, tcId);
//			Map<Integer, String> map = this.preTouchResp(tcId);
//			if (map == null)
//				return this.touchError("map == null");
//			String status = map.remove(-1);
//			List<Integer> keys = map.keySet().stream().sorted(Comparator.reverseOrder()).toList();
//			CompletableFuture.runAsync(
//					() -> this.vTouchLogService.setPreTime(keys, touchCtrl.getLastCallback(), status, beg, touchLogId));
//			TouchResult touchResult;
//			if (!keys.isEmpty() && keys.get(0) < (1 << 16))
//				touchResult = this.amqpGraphQLClient.touchPostProcess(map.get(keys.get(0)));
//			else
//				touchResult = null;
//			CompletableFuture.runAsync(() -> this.vTouchLogService.saveLog(touchResult, beg, touchLogId));
//			if (touchResult != null)
//				return touchResult;
//			return this
//					.touchError(keys.isEmpty() ? "cathold.touch.wrongTouchPreCallback" : "cathold.touch.wrongBarcode");
//		}
		return Mono.just(this.touchError("cathold.touch.nosuchClientId"));
	}

	@Override
	public void preTouchCallback(String touchId, PreTouchResult preTouchResult) {
		if (this.touchCtrlMap.containsKey(touchId))
			this.touchCtrlMap.get(touchId).preTouchCallback(preTouchResult);
	}

	private Map<Integer, String> preTouchResp(String tcId) {
		TouchControl touchControl = touchCtrlMap.get(tcId);
		Map<Integer, String> map = null;
		if (touchControl != null)
			try {
				map = CompletableFuture.supplyAsync(touchControl::waitPostMap).orTimeout(2, TimeUnit.SECONDS)
						.exceptionally(e -> touchControl.waitPostMapTimeout()).get();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		this.touchCtrlMap.remove(tcId);
		return map;
	}

	private TouchResult touchError(String msg) {
		TouchResult touchResult = new TouchResult('E', "error");
		touchResult.setResultClass(String.class);
		touchResult.setResultObject(msg);
		return touchResult;
	}

}
