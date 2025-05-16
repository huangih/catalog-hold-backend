package tw.com.hyweb.cathold.backend.redis.service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.service.ItemStatusDefService;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.TouchLog;
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

	public void updatePreTime(TouchLog touchLog, int type) {
		String key = TOUCHLOG_UPDATE_LOCK + touchLog.getId();
		touchLog.setNumber(type);
		LocalDateTime beg = touchLog.getCreateTime();
		touchLog.setPreMillions(beg.until(LocalDateTime.now(), ChronoUnit.MILLIS));
		this.redisUtils.getMonoFromLock(key, () -> this.calVolTemplate.update(touchLog)).subscribe();
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

	public Mono<Serializable> rollbackHoldItem(String barcode, int clientId) {
		return this.vParameterService.getNumberFromRuleName(ROLLBACK_BEFORE_MINUTES)
				.flatMap(minusBefore -> this.calVolTemplate.selectOne(query(where("barcode").is(barcode)
						.and("createTime").greaterThan(LocalDateTime.now().minusMinutes(minusBefore)))
						.sort(Sort.by(Direction.DESC, "id")).limit(1), TouchLog.class))
				.flatMap(tl -> {
					if (tl.getClientId() != clientId)
						return this.calVolTemplate.selectOne(query(where("id").is(tl.getClientId())), HoldClient.class)
								.map(hc -> "最近點收client為 " + hc.getName() + " 不同於此client")
								.map(s -> new TouchResult('E', "", s));
					return this.vParameterService.getStatusesFromRuleName(AUTO_ROLLBACK_STATUSES).flatMap(statuses -> {
						String status = tl.getStatus();
						if (!statuses.contains(status))
							return this.itemStatusDefService.getItemStatusCodeName(status).map(s -> "需人工處理,之前為" + s)
									.map(s1 -> new TouchResult('E', "", s1));
						else if (" ".equals(status))
							return Mono.just(tl);
						else
							return this.itemStatusDefService.getItemStatusCodeName(status).map(s -> "無需回復,之前為" + s)
									.map(s1 -> new TouchResult('T', "0-0", s1));
					});
				}).switchIfEmpty(this.vParameterService.getNumberFromRuleName(ROLLBACK_BEFORE_MINUTES)
						.map(minusBefore -> "找不到前" + minusBefore + "分鐘內跳號點收紀錄").map(s -> new TouchResult('E', "", s)));
	}

}
