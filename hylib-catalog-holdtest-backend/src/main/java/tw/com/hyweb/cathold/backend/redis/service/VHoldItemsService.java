package tw.com.hyweb.cathold.backend.redis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

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
				.flatMap(this.vHoldItemService::getVHoldItemById)
				.switchIfEmpty(this.refreshNonShadowHoldItemByMarcIdFromDb(marcId));
	}

	private Flux<VHoldItem> refreshNonShadowHoldItemByMarcIdFromDb(int marcId) {
		String idString = String.format(HOLDIDS_MARCID, marcId);
		List<VHoldItem> vhis = new ArrayList<>();
		List<Integer> holdIds = new ArrayList<>();
		return this.calVolTemplate.select(query(where("marcId").is(marcId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).map(vh -> {
					vhis.add(vh);
					holdIds.add(vh.getHoldId());
					this.vHoldItemService.redisCache(vh);
					return vh;
				}).doOnComplete(() -> {
					this.redisUtils.redisListCache(idString, holdIds, null);
					this.redisListByMarcVHoldItems(vhis);
				});
	}

	private void redisListByMarcVHoldItems(List<VHoldItem> vhis) {
		MultiValueMap<Integer, Integer> mmap = new MultiValueMapAdapter<>(new HashMap<>());
		vhis.forEach(vh -> mmap.add(vh.getCallVolId(), vh.getHoldId()));
		mmap.forEach((cvId, hIds) -> {
			String idString = String.format(HOLDIDS_CALLVOLID, cvId);
			this.redisUtils.redisListCache(idString, hIds, null);
		});
	}

	public Flux<VHoldItem> findNonShadowHoldItemByCallVolId(int callVolId) {
		String idString = String.format(HOLDIDS_CALLVOLID, callVolId);
		return this.redisUtils.getFluxFromRedis(idString, true).cast(Integer.class)
				.flatMap(this.vHoldItemService::getVHoldItemById)
				.switchIfEmpty(this.refreshNonShadowHoldItemByCvIdFromDb(callVolId));
	}

	private Flux<VHoldItem> refreshNonShadowHoldItemByCvIdFromDb(int callVolId) {
		String idString = String.format(HOLDIDS_CALLVOLID, callVolId);
		List<Integer> holdIds = new ArrayList<>();
		return this.calVolTemplate.select(query(where("callVolId").is(callVolId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).map(vh -> {
					holdIds.add(vh.getHoldId());
					this.vHoldItemService.redisCache(vh);
					return vh;
				}).doOnComplete(() -> this.redisUtils.redisListCache(idString, holdIds, null));
	}

	public Mono<Integer> getOneHoldIdByCallVolId(int callVolId) {
		return this.findNonShadowHoldItemByCallVolId(callVolId).next().map(VHoldItem::getHoldId).defaultIfEmpty(0);
	}
}
