package tw.com.hyweb.cathold.backend.redis.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.VHoldItem;

@Service
@RequiredArgsConstructor
public class VHoldItemService {

	private static final String VHOLDITEM_HOLDID = "vhi:holdId:%d:VHoldItem";

	private static final String BARCODE_HOLDID = "vhi:barcode:%s:holdId";

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<VHoldItem> getVHoldItemById(int holdId) {
		String idString = String.format(VHOLDITEM_HOLDID, holdId);
		return this.redisUtils.getMonoFromRedis(idString, null).cast(VHoldItem.class)
				.switchIfEmpty(this.redisUtils.getMonoFromDatabase(idString, () -> this.calVolTemplate
						.selectOne(query(where("holdId").is(holdId)), VHoldItem.class)
						.doOnNext(vh -> this.redisUtils
								.redisLockCache(String.format(BARCODE_HOLDID, vh.getBarcode()), vh.getHoldId(), null)
								.subscribe()),
						null));
	}

	public void redisCache(VHoldItem vh) {
		this.redisUtils.redisLockCache(String.format(VHOLDITEM_HOLDID, vh.getHoldId()), vh, null).subscribe();
		this.redisUtils.redisLockCache(String.format(BARCODE_HOLDID, vh.getBarcode()), vh.getHoldId(), null)
				.subscribe();
	}

	public Mono<VHoldItem> getVHoldItemByBarcode(String barcode) {
		String idString = String.format(BARCODE_HOLDID, barcode);
		return this.redisUtils
				.getMonoFromRedis(idString, null).cast(Integer.class).switchIfEmpty(this.redisUtils
						.getMonoFromDatabase(idString, () -> this.getHoldIdByBarcodeFromDb(barcode), null))
				.flatMap(this::getVHoldItemById);
	}

	private Mono<Integer> getHoldIdByBarcodeFromDb(String barcode) {
		return this.calVolTemplate.selectOne(query(where("barcode").is(barcode)), VHoldItem.class)
				.doOnNext(vh -> this.redisUtils
						.redisLockCache(String.format(VHOLDITEM_HOLDID, vh.getHoldId()), vh, null).subscribe())
				.map(VHoldItem::getHoldId);
	}

	public Mono<Integer> getHoldIdByBarcode(String barcode) {
		String idString = String.format(BARCODE_HOLDID, barcode);
		return this.redisUtils.getMonoFromRedis(idString, null).cast(Integer.class).switchIfEmpty(
				this.redisUtils.getMonoFromDatabase(idString, () -> this.getHoldIdByBarcodeFromDb(barcode), null));
	}

}
