package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendLog2;
import tw.com.hyweb.cathold.model.VHoldItem;

@Service
@RequiredArgsConstructor
public class LendLog2Service {

	private final VHoldItemService vHoldItemService;

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<LendLog2> newLendLog(int readerId, int holdId, int muserId, LocalDateTime begTime) {
		return this.vHoldItemService.getVHoldItemById(holdId).defaultIfEmpty(new VHoldItem()).flatMap(vh -> {
			LendLog2 lendLog2 = new LendLog2(readerId, holdId, muserId, begTime);
			if (vh.getHoldId() > 0)
				lendLog2.setStatus(vh.getStatusCode());
			return this.calVolTemplate.insert(lendLog2);
		});
	}

	public void saveLendLog2PreCheck(LendCallback lendCallback) {
		this.calVolTemplate.selectOne(query(where("id").is(lendCallback.getLogId())), LendLog2.class).subscribe(ll -> {
			char lendType = lendCallback.getCanotLendType();
			if (lendType == 0)
				lendType = '_';
			ll.setLendType(String.valueOf(lendType));
			if (lendCallback.isTimeout())
				ll.setLastType(String.valueOf(lendType));
			ll.setCallTypes(lendCallback.getCallbackTypesString());
			ll.setPreCheck((int) ChronoUnit.MILLIS.between(ll.getBegTime(), LocalDateTime.now()));
			ll.setUpdateTime(LocalDateTime.now());
			this.calVolTemplate.update(ll).subscribe();
		});
	}

	public Mono<LendLog2> saveLendLog2Callback(int logId) {
		return this.calVolTemplate.selectOne(query(where("id").is(logId)), LendLog2.class).flatMap(log -> {
			int n = (int) ChronoUnit.MILLIS.between(log.getBegTime(), LocalDateTime.now());
			log.setCallbackTime(n - log.getPreCheck());
			return this.calVolTemplate.update(log);
		});
	}

}
