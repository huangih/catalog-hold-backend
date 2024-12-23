package tw.com.hyweb.cathold.backend.redis.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.rule.service.UserCtrlRuleService;

@Service
@RequiredArgsConstructor
public class VUserCtrlStatusService {

	private static final String BOOLEAN_USERID_RULENUM = "ucrs:checkUserRule:userId:ruleNum:%d:%d:boolean";

	private final UserCtrlRuleService userCtrlRuleService;

	private final ReactiveRedisUtils redisUtils;

	private Mono<Boolean> checkUserRule(int readerId, int ruleNum) {
		String idString = String.format(BOOLEAN_USERID_RULENUM, readerId, ruleNum);
		return this.redisUtils.getMonoFromRedis(idString, Duration.ofHours(1)).cast(Boolean.class)
				.switchIfEmpty(this.redisUtils.getMonoFromDatabase(idString,
						() -> userCtrlRuleService.checkUserRule(readerId, ruleNum), Duration.ofHours(1)));
	}

	public Mono<Boolean> processCheck(int userRule, int readerId) {
		int i = 0;
		List<Mono<Boolean>> monos = new ArrayList<>();
		while (userRule > 0) {
			int ruleNum = userRule & 15;
			if (ruleNum > 0)
				monos.add(this.checkUserRule(readerId, ruleNum << (i << 2)));
			userRule >>= 4;
			i++;
		}
		return Flux.fromIterable(monos).flatMap(mono -> mono).filter(b -> !b).next().defaultIfEmpty(true);
	}

}
