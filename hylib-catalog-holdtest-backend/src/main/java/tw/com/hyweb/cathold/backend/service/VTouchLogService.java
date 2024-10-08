package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.ReactiveRedisUtils;
import tw.com.hyweb.cathold.backend.redis.service.VParameterService;
import tw.com.hyweb.cathold.model.TouchLog;
import tw.com.hyweb.cathold.model.client.TouchControl;
import tw.com.hyweb.cathold.model.client.TouchResult;

@Service
@RequiredArgsConstructor
public class VTouchLogService {

	private static final String TOUCHLOG_UPDATE_LOCK = "touchLogUpdateLock-";

	private static final String ROLLBACK_BEFORE_MINUTES = "rollbackHolditemStatus_beforeMinutes";

	private static final String AUTO_ROLLBACK_STATUSES = "autoRollbackHolditemStatuses";

	private final VParameterService vParameterService;

	private final ItemStatusDefService itemStatusDefService;

	private final R2dbcEntityOperations calVolTemplate;

	private final ReactiveRedisUtils redisUtils;

	public Mono<TouchLog> newTouchLog(String barcode, int clientId) {
		return this.calVolTemplate.insert(new TouchLog(barcode, clientId));
	}

	public void updatePreTime(TouchControl touchControl) {
		String tcId = touchControl.getTouchControlId();
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

	public void updateResultTime(final TouchResult touchResult, int touchLogId) {
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

//	public void setPreTime(List<Integer> keys, int lastPreCallback, String status, LocalTime beg, int touchLogId) {
//		this.redisUtils.getBuketFromRedis(String.format(TOUCHLOG_PREFIX, touchLogId), false, null).cast(TouchLog.class)
//				.subscribe(tl -> {
//					tl.setStatus(status);
//					Integer key = keys.isEmpty() ? 0 : keys.get(0);
//					tl.setNumber(key);
//					long millions = beg.until(LocalDateTime.now(), ChronoUnit.MILLIS);
//					tl.setPreMillions(millions);
//					tl.setPreLast(lastPreCallback);
//					this.calVolTemplate.update(tl).subscribe();
//					this.redisUtils
//							.redisLockCache(String.format(TOUCHLOG_PREFIX, touchLogId), tl, Duration.ofSeconds(10))
//							.subscribe();
//				});
//	}
//
//	public void saveLog(final TouchResult touchResult, LocalTime beg, int touchLogId) {
//		this.redisUtils.getBuketFromRedis(String.format(TOUCHLOG_PREFIX, touchLogId), false, null).cast(TouchLog.class)
//				.subscribe(tl -> {
//					long millions = beg.until(LocalDateTime.now(), ChronoUnit.MILLIS);
//					tl.setMillions(millions);
//					if (touchResult != null) {
//						tl.setSeqResult(touchResult.getSeqResult());
//						tl.setResult(touchResult.getType());
//					}
//					this.calVolTemplate.update(tl);
//				});
//	}
//
	public Mono<TouchResult> rollbackHoldItem(String barcode, int clientId) {
		return this.vParameterService.getNumberFromRuleName(ROLLBACK_BEFORE_MINUTES)
				.flatMap(minusBefore -> this.calVolTemplate
						.select(query(where("barcode").is(barcode).and("clientId").is(clientId).and("createTime")
								.greaterThan(LocalDateTime.now().minusMinutes(minusBefore))), TouchLog.class)
						.sort(Comparator.comparing(TouchLog::getId).reversed()).next())
				.flatMap(tl -> this.vParameterService.getStatusesFromRuleName(AUTO_ROLLBACK_STATUSES)
						.flatMap(statuses -> {
							String status = tl.getStatus();
							if (!statuses.contains(status))
								return this.itemStatusDefService.getItemStatusCodeName(status).map(s -> "需人工處理,之前為" + s)
										.map(s1 -> new TouchResult('E', "", s1));
							else if (" ".equals(status))
								return Mono.just(new TouchResult());
							else
								return this.itemStatusDefService.getItemStatusCodeName(status).map(s -> "無需回復,之前為" + s)
										.map(s1 -> new TouchResult('T', "0-0", s1));
						}))
				.switchIfEmpty(this.vParameterService.getNumberFromRuleName(ROLLBACK_BEFORE_MINUTES)
						.map(minusBefore -> "找不到前" + minusBefore + "分鐘內跳號點收紀錄").map(s -> new TouchResult('E', "", s)));
	}
}
