package tw.com.hyweb.cathold.backend.redis.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Service;

import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.TouchLog;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.TouchControl;
import tw.com.hyweb.cathold.model.client.TouchResult;
import tw.com.hyweb.cathold.model.client.VHoldClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class VTouchControlService {

	private static final String TOUCHCONTROL_PREFIX = "tC:touchLogId:%d:touchControl";

	private static final String TOUCHLOG_UPDATE_LOCK = "touchLogUpdateLock-";

	private final ConcurrentMap<String, TouchControl> touchCtrlMap = new ConcurrentHashMap<>();

	private final R2dbcEntityOperations calVolTemplate;

	private final ReactiveRedisUtils redisUtils;

	private final int enableNum = Stream.of(1, 3, 5, 7, 9).reduce(0, (num, en) -> num -= 1 << en);

	public Mono<TouchControl> newTouchControl(String barcode, VHoldClient vHoldClient, int muserId) {
		return this.calVolTemplate.insert(new TouchLog(barcode, vHoldClient.getId())).map(TouchLog::getId)
				.flatMap(tlId -> this.redisUtils.getMonoFromDatabase(String.format(TOUCHCONTROL_PREFIX, tlId),
						() -> Mono.just(new TouchControl(this.enableNum, tlId + "|" + barcode + "#" + muserId))
								.doOnNext(tc -> this.touchCtrlMap.put(tc.getTouchControlId(), tc)),
						Duration.ofSeconds(10)));
	}

	public void updatePreTime(TouchControl touchControl) {
		String tcId = touchControl.getTouchControlId();
		log.info("remove touchControl: {}", this.touchCtrlMap.remove(tcId));
		this.touchCtrlMap.remove(tcId);
		int touchLogId = Integer.parseInt(tcId.split("\\|")[0]);
		Set<Integer> keys = touchControl.getPreMap().keySet();
		this.redisUtils.getMonoFromLock(TOUCHLOG_UPDATE_LOCK + touchLogId,
				() -> this.calVolTemplate.selectOne(query(where("id").is(touchLogId)), TouchLog.class).flatMap(tl -> {
					tl.setStatus(touchControl.getPreMap().get(-1));
					if (!keys.isEmpty())
						tl.setNumber(Collections.max(keys));
					LocalDateTime beg = tl.getCreateTime();
					long millions = beg.until(LocalDateTime.now(), ChronoUnit.MILLIS);
					tl.setPreMillions(millions);
					tl.setPreLast(touchControl.getLastCallback());
					return this.calVolTemplate.update(tl);
				})).subscribe();
	}

	public void updateResultTime(final TouchResult touchResult, TouchControl touchControl) {
		String tcId = touchControl.getTouchControlId();
		int touchLogId = Integer.parseInt(tcId.split("\\|")[0]);
		this.redisUtils.getMonoFromLock(TOUCHLOG_UPDATE_LOCK + touchLogId,
				() -> this.calVolTemplate.selectOne(query(where("id").is(touchLogId)), TouchLog.class).flatMap(tl -> {
					LocalDateTime beg = tl.getCreateTime();
					long millions = beg.until(LocalDateTime.now(), ChronoUnit.MILLIS);
					tl.setMillions(millions);
					if (touchResult != null) {
						tl.setSeqResult(touchResult.getSeqResult());
						tl.setResult(String.valueOf(touchResult.getType()));
					}
					return this.calVolTemplate.update(tl);
				})).subscribe();
	}

	public Mono<Void> preTouchCallback(String tcId, PreTouchResult preTouchResult) {
		return Mono.justOrEmpty(this.touchCtrlMap.get(tcId))
				.doOnNext(tc -> log.info("preCallback: {}", tc.preTouchCallback(preTouchResult))).then();
	}

}
