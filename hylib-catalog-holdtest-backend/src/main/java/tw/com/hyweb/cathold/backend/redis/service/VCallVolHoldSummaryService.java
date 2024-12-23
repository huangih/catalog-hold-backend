package tw.com.hyweb.cathold.backend.redis.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.service.UserCheckService;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.VHotBookingDate;
import tw.com.hyweb.cathold.model.view.CallVolHoldSummary;
import tw.com.hyweb.cathold.sqlserver.model.SqlserverCharged;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverChargedRepository;

@Service
@RequiredArgsConstructor
public class VCallVolHoldSummaryService {

	private static final String CVHS_CALLVOLID = "hS:callVolId:%d:CallVolHoldSummary";

	private static final String MARCALLVOL_CALLVOLID = "mcv:callVolId:%d:MarcCallVolume";

	private final UserCheckService userCheckService;

	private final SqlserverChargedRepository sqlserverChargedRepository;

	private final VBookingService vBookingService;

	private final VHoldItemsService vHoldItemsService;

	private final VMarcCallVolumeService vMarcCallVolumeService;

	private final ReactiveRedisUtils redisUtils;

	public Mono<CallVolHoldSummary> findCallVolHoldSummaryByCallVolId(int callVolId) {
		String idString = String.format(CVHS_CALLVOLID, callVolId);
		return this.redisUtils.getMonoFromRedis(idString, null).cast(CallVolHoldSummary.class)
				.switchIfEmpty(this.refreshCallVolHoldSummary(callVolId))
				.zipWith(this.vBookingService.findBookingIdsByItemId(callVolId).map(List::size), (cvh, num) -> {
					cvh.setWaitBookingNum(num);
					return cvh;
				});
	}

	public Mono<CallVolHoldSummary> refreshCallVolHoldSummary(int callVolId) {
		String idString = String.format(CVHS_CALLVOLID, callVolId);
		return this.vHoldItemsService.findNonShadowHoldItemByCallVolId(callVolId).collectList()
				.flatMap(vhis -> this.refreshCallVolHoldSummary(callVolId, vhis))
				.doOnNext(cvhs -> this.redisUtils.redisLockCache(idString, cvhs, null).subscribe());
	}

	private Mono<CallVolHoldSummary> refreshCallVolHoldSummary(int callVolId, @NonNull List<VHoldItem> vhis) {
		if (callVolId == 0 || vhis.isEmpty())
			return Mono.empty();
		return this.findChargedHoldsDueDate(vhis)
				.zipWith(this.vMarcCallVolumeService.getMarcCallVolumeByCallVolId(callVolId),
						(lendLi, mcv) -> new CallVolHoldSummary(callVolId, vhis, mcv, lendLi))
				.flatMap(this::chkHotStatusDate);
	}

	private Mono<List<SqlserverCharged>> findChargedHoldsDueDate(List<VHoldItem> vhis) {
		return Flux.fromIterable(vhis).filter(VHoldItem::onCheckout).map(VHoldItem::getHoldId).collectList()
				.map(hIds -> this.sqlserverChargedRepository.findByHoldIdIn(hIds, Sort.by("returnDate")));
	}

	private Mono<CallVolHoldSummary> chkHotStatusDate(CallVolHoldSummary cvhs) {
		if (cvhs.isHotBooking())
			return this.vMarcCallVolumeService.getVHotBookingDateByCallVolId(cvhs.getId()).map(vhbd -> {
				cvhs.setStatusDate(vhbd.getStatusDate());
				return cvhs;
			});
		return Mono.just(cvhs);
	}

	public Mono<CallVolHoldSummary> findCallVolHoldSummaryByCallVolId(int callVolId, int readerId) {
		Mono<CallVolHoldSummary> mono = this.findCallVolHoldSummaryByCallVolId(callVolId);
		if (readerId > 0)
			return mono.flatMap(cvhs -> this.vHoldItemsService.findNonShadowHoldItemByCallVolId(callVolId)
					.filter(VHoldItem::ruleDepdentUser)
					.flatMap(vh -> this.userCheckService.setDependUserRuleStatus(readerId, vh)).collectList()
					.map(vhis -> {
						if (vhis.isEmpty())
							return cvhs;
						cvhs.updateHoldItemsStatus(vhis);
						return cvhs;
					}));
		return mono;
	}

	public void deleteCallVolHoldSummary(int callVolId) {
		this.redisUtils.unlink(String.format(CVHS_CALLVOLID, callVolId));
		this.redisUtils.unlink(String.format(MARCALLVOL_CALLVOLID, callVolId));
	}

	public void refrshByHotBookingDate(VHotBookingDate vhbd) {
		String idString = String.format(CVHS_CALLVOLID, vhbd.getCallVolId());
		this.redisUtils.getMonoFromRedis(idString, null).map(CallVolHoldSummary.class::cast)
				.filter(cvh -> cvh.isHotBooking() ^ vhbd.isHotBooking()).flatMap(cvh -> {
					boolean hot = vhbd.isHotBooking();
					cvh.setHotBooking(hot);
					cvh.setStatusDate(hot ? vhbd.getStatusDate() : null);
					return this.redisUtils.redisLockCache(idString, cvh, null);
				}).subscribe();
	}

}
