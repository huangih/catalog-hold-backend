package tw.com.hyweb.cathold.backend.redis.service;

import java.time.Duration;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.service.LendLog2Service;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendCheck;

@Service
@RequiredArgsConstructor
@Slf4j
public class VLendCallBackService {

	private static final String LEND_CALLBACK = "bl:blc:cbId:%s:lendCallback";

	private static final String LEND_CALLBACK_LOCK = "bl:blc:lid:%d:lock";

	private static final String MOBILE_LEND_CALLBACK = "bl:blc:bs:%s:barcode";

	private final LendLog2Service lendLog2Service;

	private final ReactiveRedisUtils redisUtils;

	public Mono<LendCheck> prepareLendCallback(LendCallback lendCallback) {
		return Flux.generate(sink -> sink.next(RandomString.make(16))).filterWhen(cbId -> {
			String redisKey = String.format(LEND_CALLBACK, cbId);
			return this.redisUtils.hasKey(redisKey).map(b -> !b);
		}).next().doOnNext(cbId -> this.lendLog2Service.saveLendLog2PreCheck(lendCallback))
				.flatMap(cbId -> this.redisUtils
						.redisLockCache(String.format(LEND_CALLBACK, cbId), lendCallback, Duration.ofMinutes(5))
						.map(lc -> new LendCheck((String) cbId, lc))
						.doOnNext(lck -> log.info("prepareLendCallback: {}-{}", lck, lendCallback)));
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

	public Mono<Boolean> lendCheck(LendCallback lendCallback, LendCheck lendCheck) {
		String lock = String.format(LEND_CALLBACK_LOCK, lendCallback.getLogId());
		return this.redisUtils.getMonoFromLock(lock, () -> lendCallback.lendCheck(lendCheck));
	}

}
