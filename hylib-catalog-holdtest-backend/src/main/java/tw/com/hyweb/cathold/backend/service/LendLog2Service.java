package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendLog2;

@Service
@RequiredArgsConstructor
public class LendLog2Service {

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<LendLog2> newLendLog(LendCallback lendCallback) {
		return this.calVolTemplate.insert(new LendLog2(lendCallback));
	}

	public void saveLendLog2PreCheck(LendCallback lendCallback) {
		this.calVolTemplate.selectOne(query(where("id").is(lendCallback.getLogId())), LendLog2.class)
				.subscribe(lendLog -> {
					lendLog.setStatus(lendCallback.getStatus());
					char lastType = lendCallback.getLastType();
					if (lastType < '@')
						lastType = LendCallback.compLastType(lastType);
					lendLog.setLastType(lastType);
					lendLog.setLendType(lendCallback.getType());
					lendLog.setCallTypes(lendCallback.getCallbackTypesString());
					lendLog.setPreCheck(
							(int) ChronoUnit.MILLIS.between(lendCallback.getBegTime(), LocalDateTime.now()));
					this.calVolTemplate.update(lendLog).subscribe();
				});
	}

	public void saveLendLog2Callback(int logId) {
		this.calVolTemplate.selectOne(query(where("id").is(logId)), LendLog2.class).subscribe(log -> {
			int n = (int) ChronoUnit.MILLIS.between(log.getBegTime(), LocalDateTime.now());
			log.setCallbackTime(n - log.getPreCheck());
			this.calVolTemplate.update(log).subscribe();
		});
	}

}
