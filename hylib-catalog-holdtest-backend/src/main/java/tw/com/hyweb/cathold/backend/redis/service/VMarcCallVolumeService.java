package tw.com.hyweb.cathold.backend.redis.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.VHotBookingDate;
import tw.com.hyweb.cathold.model.VMarcCallVolume;
import tw.com.hyweb.cathold.model.MarcCallVolume;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.view.MarcVolume;

@Component
@RequiredArgsConstructor
public class VMarcCallVolumeService {

	private static final String CALLVOLIDS_MARCID = "mcv:marcId:%d:cvIds";

	private static final String MARCALLVOL_CALLVOLID = "mcv:callVolId:%d:marcCallVol";

	private static final String VHOTBOOKINGDATE_CALLVOLID = "mcv:hb:callVolId:%d:vHotBookingDate";

	private final VHoldItemsService vHoldItemsService;

	private final R2dbcEntityOperations calVolTemplate;

	private final ReactiveRedisUtils redisUtils;

	public Flux<MarcCallVolume> getMarcCallVolumesByMarcId(int marcId) {
		String idString = String.format(CALLVOLIDS_MARCID, marcId);
		return this.redisUtils.getFluxFromRedis(idString, true).cast(Integer.class)
				.flatMap(this::getMarcCallVolumeByCallVolId, 1).switchIfEmpty(this.refreshMarcCallVolsFromDb(marcId));
	}

	private Flux<MarcCallVolume> refreshMarcCallVolsFromDb(int marcId) {
		String idString = String.format(CALLVOLIDS_MARCID, marcId);
		List<Integer> callVolIds = new ArrayList<>();
		return this.vHoldItemsService.findNonShadowHoldItemByMarcId(marcId).map(VHoldItem::getCallVolId).distinct()
				.flatMap(this::getMarcCallVolumeByCallVolId).sort(MarcCallVolumeComparator.INSTANCE).map(mcv -> {
					callVolIds.add(mcv.getId());
					return mcv;
				}).doOnComplete(() -> this.redisUtils.redisListCache(idString, callVolIds, null));
	}

	public Mono<MarcCallVolume> getMarcCallVolumeByCallVolId(int callVolId) {
		String idString = String.format(MARCALLVOL_CALLVOLID, callVolId);
		return this.redisUtils.getMonoFromRedis(idString, null).cast(MarcCallVolume.class)
				.switchIfEmpty(this.redisUtils.getMonoFromWriteLock(idString,
						() -> this.calVolTemplate.selectOne(query(where("id").is(callVolId)), VMarcCallVolume.class)
								.map(MarcCallVolume::new)
								.flatMap(mcv -> this.redisUtils.saveForCache(idString, mcv, null))));
	}

	public Mono<MarcCallVolume> refreshMarcCallVolByCallVolId(int callVolId, Mono<MarcCallVolume> supplier) {
		String idString = String.format(MARCALLVOL_CALLVOLID, callVolId);
		return this.redisUtils.redisMonoCache(idString, supplier, null);
	}

	public Mono<MarcVolume> getMarcVolumeByCallVolId(int callVolId) {
		return this.getMarcCallVolumeByCallVolId(callVolId).map(MarcVolume::new).defaultIfEmpty(new MarcVolume());
	}

	public Mono<VHotBookingDate> getVHotBookingDateByCallVolId(int callVolId) {
		String idString = String.format(VHOTBOOKINGDATE_CALLVOLID, callVolId);
		return this.redisUtils.getMonoFromRedis(idString, null).cast(VHotBookingDate.class).switchIfEmpty(
				this.calVolTemplate.selectOne(query(where("callVolId").is(callVolId)), VHotBookingDate.class)
						.doOnNext(vhbd -> this.redisUtils.redisLockCache(idString, vhbd, null)));
	}

	public Mono<Boolean> getHotTypeByCallVolId(int callVolId) {
		return this.getVHotBookingDateByCallVolId(callVolId).map(VHotBookingDate::isHotBooking).defaultIfEmpty(false);
	}

}
