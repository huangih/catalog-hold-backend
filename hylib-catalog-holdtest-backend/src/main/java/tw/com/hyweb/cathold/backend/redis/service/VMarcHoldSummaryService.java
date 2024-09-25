package tw.com.hyweb.cathold.backend.redis.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VMarcHoldSummaryService {

	private static final String HOLDSUMMARY_MARCID = "hS:marcId:%d:MarcHoldSummary";

	private final ReactiveRedisUtils redisUtils;

	public void deleteMarcHoldSummary(int marcId) {
		this.redisUtils.unlink(String.format(HOLDSUMMARY_MARCID, marcId));
	}

}
