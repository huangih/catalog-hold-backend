package tw.com.hyweb.cathold.backend.redis.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.view.MarcHoldSummary;

@Service
@RequiredArgsConstructor
public class VMarcHoldSummaryService {

	private static final String HOLDSUMMARY_MARCID = "hS:marcId:%d:MarcHoldSummary";

	private final VHoldItemsService vHoldItemsService;

	private final VBookingService vBookingService;

	private final VMarcCallVolumeService vMarcCallVolumeService;

	private final ReactiveRedisUtils redisUtils;

	public Mono<MarcHoldSummary> findMarcHoldSummaryByMarcId(int marcId) {
		String idString = String.format(HOLDSUMMARY_MARCID, marcId);
		return this.redisUtils.getMonoFromRedis(idString, null).cast(MarcHoldSummary.class)
				.switchIfEmpty(this.refreshMarcHoldSummary(marcId))
				.flatMap(mhs -> this.countWaitBookings(mhs.getCallVolIds())
						.zipWith(this.countWaitBookings(mhs.getPmCallVolIds()), (n1, n2) -> {
							mhs.setWaitBookingNum(n1 + n2);
							return mhs;
						}));
	}

	private Mono<MarcHoldSummary> refreshMarcHoldSummary(int marcId) {
		String idString = String.format(HOLDSUMMARY_MARCID, marcId);
		return this.vHoldItemsService.findNonShadowHoldItemByMarcId(marcId).collectList()
				.zipWith(this.vMarcCallVolumeService.getMarcCallVolumesByMarcId(marcId).collectList())
				.flatMap(tup2 -> this.redisUtils.redisLockCache(idString,
						new MarcHoldSummary(marcId, tup2.getT1(), tup2.getT2()), null));
	}

	private Mono<Integer> countWaitBookings(List<Integer> callVolIds) {
		return Flux.fromIterable(callVolIds).flatMap(this.vBookingService::findBookingIdsByItemId).map(List::size)
				.reduce(0, (n1, n2) -> n1 + n2);
	}

	public void deleteMarcHoldSummary(int marcId) {
		this.redisUtils.unlink(String.format(HOLDSUMMARY_MARCID, marcId));
	}

}
