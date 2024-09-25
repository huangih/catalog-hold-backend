package tw.com.hyweb.cathold.backend.redis.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Service;

import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.service.ItemStatusDefService;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.TouchLog;
import tw.com.hyweb.cathold.model.client.PreTouchResult;
import tw.com.hyweb.cathold.model.client.TouchControl;
import tw.com.hyweb.cathold.model.client.TouchResult;
import tw.com.hyweb.cathold.model.client.VHoldClient;
import tw.com.hyweb.cathold.model.client.VTouchLog;

@Service
@RequiredArgsConstructor
@Slf4j
public class VTouchControlService {

	private static final String TOUCHCONTROL_PREFIX = "tC:touchLogId:%d:touchControl";

	private static final String TOUCHLOG_UPDATE_LOCK = "touchLogUpdateLock-";

	private static final String ROLLBACK_BEFORE_MINUTES = "rollbackHolditemStatus_beforeMinutes";

	private static final String AUTO_ROLLBACK_STATUSES = "autoRollbackHolditemStatuses";

	private final ConcurrentMap<String, TouchControl> touchCtrlMap = new ConcurrentHashMap<>();

	private final VParameterService vParameterService;

	private final ItemStatusDefService itemStatusDefService;

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

	public Mono<TouchResult> rollbackHoldItem(String barcode, char ctrlChar, int clientId) {
		if (ctrlChar == '!')
			return this.calVolTemplate.selectOne(query(where("id").is(clientId)), HoldClient.class)
					.map(HoldClient::getSiteCode)
					.zipWith(this.vParameterService.getNumberFromRuleName(ROLLBACK_BEFORE_MINUTES))
					.flatMapMany(tuple2 -> this.calVolTemplate
							.select(query(where("barcode").is(barcode).and("siteCode").is(tuple2.getT1())
									.and("createTime").greaterThan(LocalDateTime.now().minusMinutes(tuple2.getT2()))),
									VTouchLog.class)
							.sort(Comparator.comparing(VTouchLog::getId).reversed()))
					.next().flatMap(vtl -> this.vParameterService.getStatusesFromRuleName(AUTO_ROLLBACK_STATUSES)
							.flatMap(statuses -> {
								String status = vtl.getStatus();
								if (!statuses.contains(status))
									return this.itemStatusDefService.getItemStatusCodeName(status)
											.map(s -> "需人工處理,之前為" + s).map(s1 -> new TouchResult('E', "", s1));
								else if (" ".equals(status))
									return Mono.empty();
								else
									return this.itemStatusDefService.getItemStatusCodeName(status)
											.map(s -> "無需回復,之前為" + s).map(s1 -> new TouchResult('T', "0-0", s1));
							}))
					.switchIfEmpty(this.vParameterService.getNumberFromRuleName(ROLLBACK_BEFORE_MINUTES)
							.map(minusBefore -> "找不到前" + minusBefore + "分鐘內跳號點收紀錄")
							.map(s -> new TouchResult('E', "", s)));
		return Mono.empty();
	}

	public Mono<Void> preTouchCallback(String tcId, PreTouchResult preTouchResult) {
		return Mono.justOrEmpty(this.touchCtrlMap.get(tcId))
				.doOnNext(tc -> log.info("preCallback: {}", tc.preTouchCallback(preTouchResult))).then();
	}

}
