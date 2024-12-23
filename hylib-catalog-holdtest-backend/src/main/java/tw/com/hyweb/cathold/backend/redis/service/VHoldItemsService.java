package tw.com.hyweb.cathold.backend.redis.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.VHoldItem;

@Service
@RequiredArgsConstructor
public class VHoldItemsService {

	private static final String HOLDIDS_MARCID = "vhi:marcId:%d:holdIds";

	private static final String HOLDIDS_CALLVOLID = "vhi:callVolId:%d:holdIds";

	private final VHoldItemService vHoldItemService;

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityTemplate calVolTemplate;

	public Flux<VHoldItem> findNonShadowHoldItemByMarcId(int marcId) {
		String idString = String.format(HOLDIDS_MARCID, marcId);
		return this.redisUtils.getFluxFromRedis(idString, true).cast(Integer.class)
				.flatMap(this.vHoldItemService::getVHoldItemById, 1)
				.switchIfEmpty(this.refreshNonShadowHoldItemByMarcIdFromDb(marcId));
	}

	private Flux<VHoldItem> refreshNonShadowHoldItemByMarcIdFromDb(int marcId) {
		return this.calVolTemplate.select(query(where("marcId").is(marcId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).collectList().doOnNext(vhis -> this.redisListByVHoldItems(vhis, marcId))
				.flatMapMany(Flux::fromIterable);
	}

	public Flux<VHoldItem> findNonShadowHoldItemByCallVolId(int callVolId) {
		String idString = String.format(HOLDIDS_CALLVOLID, callVolId);
		return this.redisUtils.getFluxFromRedis(idString, true).cast(Integer.class)
				.flatMap(this.vHoldItemService::getVHoldItemById, 1)
				.switchIfEmpty(this.refreshNonShadowHoldItemByCvIdFromDb(callVolId));
	}

	private Flux<VHoldItem> refreshNonShadowHoldItemByCvIdFromDb(int callVolId) {
		return this.calVolTemplate.select(query(where("callVolId").is(callVolId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).collectList().doOnNext(vhis -> this.redisListByVHoldItems(vhis, 0))
				.flatMapMany(Flux::fromIterable);
	}

	private void redisListByVHoldItems(List<VHoldItem> vhis, int marcId) {
		Flux.fromIterable(vhis).doOnNext(this.vHoldItemService::redisCache)
				.collectMultimap(VHoldItem::getCallVolId, VHoldItem::getHoldId)
				.subscribe(mmap -> Flux.fromIterable(mmap.entrySet()).doOnNext(entry -> {
					String key = String.format(HOLDIDS_CALLVOLID, entry.getKey());
					this.redisUtils.redisListCache(key, new ArrayList<Integer>(entry.getValue()), null);
				}).doOnComplete(() -> {
					if (marcId > 0)
						Flux.fromIterable(vhis).map(VHoldItem::getHoldId).collectList()
								.subscribe(hIds -> this.redisUtils.redisListCache(String.format(HOLDIDS_MARCID, marcId),
										hIds, null));
				}));
	}

	public Mono<Integer> getOneHoldIdByCallVolId(int callVolId) {
		return this.findNonShadowHoldItemByCallVolId(callVolId).next().map(VHoldItem::getHoldId).defaultIfEmpty(0);
	}
}
