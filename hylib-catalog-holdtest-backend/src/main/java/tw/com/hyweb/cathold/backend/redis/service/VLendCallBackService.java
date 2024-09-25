package tw.com.hyweb.cathold.backend.redis.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendCheck;

@Service
@RequiredArgsConstructor
public class VLendCallBackService {

	private static final String LEND_CALLBACK = "bl:blc:rs:%s:ri";

	private static final String MOBILE_LEND_CALLBACK = "bl:blc:bs:%s:barcode";

	private final ReactiveRedisUtils redisUtils;

	public Mono<LendCheck> prepareLendCallback(LendCallback lendCallback) {
		return Flux.generate(sink -> sink.next(RandomString.make(16))).filterWhen(cbId -> {
			String redisKey = String.format(LEND_CALLBACK, cbId);
			return this.redisUtils.hasKey(redisKey).map(b -> !b);
		}).next()
				.flatMap(cbId -> this.redisUtils
						.redisLockCache(String.format(LEND_CALLBACK, cbId), lendCallback, Duration.ofMinutes(5))
						.map(lc -> new LendCheck((String) cbId, lc)));
	}

	public Mono<LendCallback> getLendCallback(String callbackId) {
		return this.redisUtils.getBuketFromRedis(String.format(LEND_CALLBACK, callbackId), false, null);
	}

	public String getCallbackMobileBarcode(String callbackId) {
		Object obj = this.redisUtils.getBuketFromRedis(String.format(MOBILE_LEND_CALLBACK, callbackId), false, null);
		if (obj instanceof String barcode)
			return barcode;
		return "";
	}

}
