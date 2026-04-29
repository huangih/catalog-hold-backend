package tw.com.hyweb.cathold.backend.service;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.ReactiveRedisUtils;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchLogService;
import tw.com.hyweb.cathold.model.AnnexHold;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.Intransit;
import tw.com.hyweb.cathold.model.ItemStatusDef;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.TouchCallback;
import tw.com.hyweb.cathold.model.TouchLog;
import tw.com.hyweb.cathold.model.TouchObj;
import tw.com.hyweb.cathold.model.VBookingAvailation;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.client.TouchResult;
import tw.com.hyweb.cathold.sqlserver.model.SqlserverHoldStatus;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverHoldStatusRepository;

@RequiredArgsConstructor
@Slf4j
public class TouchServiceImpl implements TouchService {

	@Value("${cathold.bookingTransit.routekey}")
	private String btRouteKey;

	@Value("${cathold.holditem.routekey}")
	private String hiRouteKey;

	private static final String BOOKING_ITEMID_RWLOCK = "/cathold/bookingItemid_key/%d";

	private static final List<String> STATUSES = List.of("T8", "T", "T2", "T3", "T9", "Z", "W", "LO");

	private static final List<Phase> HADHOLD_BOOKINGPHASE = List.of(Phase.WAIT_ANNEX, Phase.AVAILABLE, Phase.CAB_WAIT,
			Phase.TRANSIT_B);

	private static final List<String> ANNEX_TYPES = List.of("BA", "AA", "BDA", "HOT-BA");

	private static final int CLY_SITEID = 96;

	private static final String PHASE = "phase";

	private final VHoldClientService vHoldClientService;

	private final VTouchLogService vTouchLogService;

	private final VHoldItemService vHoldItemService;

	private final ItemSiteDefService itemSiteDefService;

	private final SqlserverHoldStatusRepository sqlserverHoldStatusRepository;

	private final AmqpBackendClient amqpBackendClient;

	private final R2dbcEntityOperations calVolTemplate;

	private final ReactiveRedisUtils redisUtils;

	@Override
	public Mono<TouchResult> touchHoldItem(String barcode, String sessionId, int muserId) {
		return this.vHoldClientService.getVHoldClientBySessionId(sessionId)
				.flatMap(vhc -> this.vTouchLogService.newTouchLog(barcode, vhc.getId(), muserId)
						.map(tl -> new TouchObj(tl, vhc, barcode, sessionId, muserId)))
				.flatMap(this::touchHoldItem);
	}

	private Mono<TouchResult> touchHoldItem(TouchObj touchObj) {
		if ('!' == touchObj.getCtrlChar())
			return this.rollbackHoldItem(touchObj);
		return this.vHoldItemService.getVHoldItemByBarcode(touchObj.getBarcode()).map(vh -> {
			touchObj.setVHoldItem(vh);
			return vh.getHoldId();
		}).flatMap(holdId -> Mono.justOrEmpty(this.sqlserverHoldStatusRepository.findByHoldId(holdId))
				.map(SqlserverHoldStatus::getStatus)
				.timeout(Duration.ofSeconds(5), Mono.just(touchObj.getVHoldItem().getStatusCode())).flatMap(status -> {
					touchObj.getTouchLog().setStatus(status);
					return this.chkTouchCtrlChar(touchObj)
							.switchIfEmpty(this.touchHoldItemPre(touchObj).flatMap(tcb -> {
								int type = tcb.getType();
								if (type > 0)
									return this.amqpBackendClient.touchPostProcess(tcb)
											.doOnNext(tr -> this.amqpBackendClient.subWhiteUid(touchObj.getBarcode()))
											.timeout(Duration.ofSeconds(3),
													this.touchError("cathold.touch.postTouchTimeout"));
								if (type == -1)
									return this.touchError("cathold.touch.preTouchTimeout");
								return this.touchError("cathold.touch.wrongTouchPreCallback");
							}));
				}).switchIfEmpty(this.touchError("cathold.touch.notExistHoldOnHylib")))
				.switchIfEmpty(this.touchError("cathold.touch.wrongBarcode"))
				.doOnNext(tr -> this.vTouchLogService.updateResultTime(tr, touchObj.getTouchLogId()));
	}

	private Mono<TouchResult> rollbackHoldItem(TouchObj touchObj) {
		String barcode = touchObj.getBarcode();
		int vhcId = touchObj.getVHoldClient().getId();
		String sessionId = touchObj.getSessionId();
		int muserId = touchObj.getMuserId();
		return this.vTouchLogService.rollbackHoldItem(barcode, vhcId).flatMap(obj -> {
			if (obj instanceof TouchLog tl)
				return this.amqpBackendClient.rollBackHoldItem(sessionId, tl, muserId);
			if (obj instanceof TouchResult tr)
				return Mono.just(tr);
			return Mono.empty();
		});
	}

	private Mono<TouchResult> chkTouchCtrlChar(TouchObj touchObj) {
		char ctrlChar = touchObj.getCtrlChar();
		int holdId = touchObj.getVHoldItem().getHoldId();
		if ('/' == ctrlChar)
			return this.calVolTemplate.exists(query(where("holdId").is(holdId)), VBookingAvailation.class)
					.filter(b -> !b).flatMap(b -> this.touchError("cathold.touch.notBookingAvailableHold"));
		if (ctrlChar == 0)
			this.amqpBackendClient.touchOverDueWaitingCheck(holdId);
		return Mono.empty();
	}

	private Mono<TouchCallback> touchHoldItemPre(TouchObj touchObj) {
		return this.touchHoldItemStatusPre(touchObj).switchIfEmpty(this.touchHoldItemBookingPre(touchObj))
				.switchIfEmpty(this.touchHoldItemTransitPre(touchObj))
				.switchIfEmpty(this.touchHoldItemReturnPre(touchObj))
				.timeout(Duration.ofSeconds(3), Mono.just(new TouchCallback(-1)))
				.doOnNext(tc -> this.vTouchLogService.updatePreTime(touchObj.getTouchLog(), tc.getType()));
	}

	private Mono<TouchCallback> touchHoldItemStatusPre(TouchObj touchObj) {
		String status = touchObj.getTouchLog().getStatus();
		if (!STATUSES.contains(status))
			return Mono.empty();
		final String fstatus = "T8".equals(status) ? "NB" : status;
		int holdId = touchObj.getVHoldItem().getHoldId();
		if ("NB".equals(fstatus))
			this.amqpBackendClient.setHoldItemStatus(holdId, fstatus, touchObj.getMuserId());
		return this.calVolTemplate.selectOne(query(where("statusCode").is(fstatus)), ItemStatusDef.class)
				.map(ItemStatusDef::getStatusName)
				.zipWith(this.itemSiteDefService.getClyStrBySiteId(touchObj.getVHoldItem().getSiteId()),
						(sName, clyS) -> sName + "#1 " + clyS)
				.map(s -> new TouchCallback(2, "touchPostStatusHoldItem", this.hiRouteKey,
						new Object[] { touchObj.getSessionId(), holdId, s, !"NB".equals(fstatus) }));
	}

	private Mono<TouchCallback> touchHoldItemBookingPre(TouchObj touchObj) {
		VHoldItem vh = touchObj.getVHoldItem();
		char ctrlChar = touchObj.getCtrlChar();
		String sessionId = touchObj.getSessionId();
		int muserId = touchObj.getMuserId();
		return this.calVolTemplate.selectOne(query(where("associateId").is(vh.getHoldId())), Booking.class)
				.switchIfEmpty(Mono.just(touchObj.isFloatPriority('a')).filter(b -> !b)
						.flatMap(b -> this.findBookingFirstPosition(vh.getHoldId(), vh.getCallVolId())))
				.defaultIfEmpty(new Booking()).mapNotNull(bi -> {
					if (bi.getId() == 0) {
						this.amqpBackendClient.touchDistribution(vh.getHoldId(), Phase.NOMORENEED);
						return null;
					}
					Phase phase = bi.getPhase();
					if (ctrlChar == '-' && HADHOLD_BOOKINGPHASE.contains(phase))
						return new TouchCallback(3, "touchCancelBooking", this.btRouteKey,
								new Object[] { sessionId, bi.getId(), muserId });
					else if (ctrlChar == 0 || ctrlChar == '/')
						return new TouchCallback(4, "touchPostBooking", this.btRouteKey,
								new Object[] { sessionId, ctrlChar, bi, muserId });
					return null;
				});
	}

	private Mono<Booking> findBookingFirstPosition(int holdId, int callVolId) {
		String key = String.format(BOOKING_ITEMID_RWLOCK, callVolId);
		List<Phase> phases = List.of(Phase.PLACE, Phase.DISTRIBUTION);
		return this.redisUtils.getMonoFromReadLock(key, () -> {
			Mono<Boolean> checkMono = this.vHoldItemService.getVHoldItemById(holdId)
					.map(vh -> vh.supportBooking() && vh.bookingCheckOut());
			return this.calVolTemplate
					.select(query(where("itemId").is(holdId).and("type").is("C").and(PHASE).in(phases))
							.sort(Sort.by("placeDate")), Booking.class)
					.next()
					.switchIfEmpty(checkMono.filter(b -> b)
							.flatMap(b -> this.calVolTemplate.select(
									query(where("itemId").is(callVolId).and("type").is("T").and(PHASE).in(phases))
											.sort(Sort.by("placeDate")),
									Booking.class).next()))
					.map(bi -> {
						bi.setAssociateId(holdId);
						return bi;
					});
		});
	}

	private Mono<TouchCallback> touchHoldItemTransitPre(TouchObj touchObj) {
		int holdId = touchObj.getVHoldItem().getHoldId();
		List<Integer> siteIds = touchObj.getVHoldClient().getNoIntransitSites();
		char ctrlChar = touchObj.getCtrlChar();
		int muserId = touchObj.getMuserId();
		String typeCode = touchObj.getVHoldItem().getTypeCode();
		String sessionId = touchObj.getSessionId();
		int oriSiteId = touchObj.getVHoldItem().getOriSiteId();
		return this.calVolTemplate.selectOne(query(where("holdId").is(holdId)), Intransit.class).map(it -> {
			touchObj.setTransitPhase(it.getPhase());
			if (!siteIds.contains(it.getFromSiteId())) // 非調出館重複點收
				return this.checkTransitPostProcess(it, siteIds, muserId); // 除巡迴車點收,調撥檔轉歷史檔,若非調撥檔目的館,回傳true
			// 若調出館==點收館,檢查是否為字元'-'(調撥待附件)
			if (ctrlChar == '-' && it.getPhase() == Phase.TRANSIT_B) {
				it.setPhase(Phase.TRANSIT_WA);
				this.amqpBackendClient.moveTransitToHistory(it, siteIds.getFirst(), muserId);
				return false;
			}
			return true;
		}).defaultIfEmpty(true).filter(b -> b).mapNotNull(b -> {
			if (this.transitReturnOut(touchObj)// chek浮動館藏機制若仍需"調撥回館"傳回true
					&& !ANNEX_TYPES.contains(typeCode))
				return new TouchCallback(5, "touchPostIntransit", this.btRouteKey,
						new Object[] { sessionId, holdId, oriSiteId, Phase.TRANSIT_R, null, muserId });// 依原始館藏館設調撥目的館
			return null;
		});
	}

	private boolean transitReturnOut(TouchObj touchObj) {
		List<Integer> siteIds = touchObj.getVHoldClient().getNoIntransitSites();
		int oriSiteId = touchObj.getVHoldItem().getOriSiteId();
		int siteId = touchObj.getVHoldItem().getSiteId();
		boolean bRet = !siteIds.contains(oriSiteId) && !siteIds.contains(siteId);
		return bRet && (!touchObj.compFloatTouch() || Phase.WAIT_TRANSITR == touchObj.getTransitPhase());
	}

	private boolean checkTransitPostProcess(Intransit intransit, List<Integer> siteIds, int muserId) {
		// 處理現存調撥記錄,回傳是否續處理調撥,有值表己送至目的館不需處理。
		int siteId = siteIds.getFirst();
		if (CLY_SITEID != siteId) { // 若非巡迴車，將目前的的調撥轉為歷史檔
			int toSiteId = intransit.getToSiteId();
			boolean onToSiteId = siteIds.contains(toSiteId);
			this.amqpBackendClient.moveTransitToHistory(intransit, onToSiteId ? toSiteId : siteId, muserId);
			return !onToSiteId;
		}
		// 巡迴車廠商點收處理
		intransit.setRelayDate(LocalDateTime.now());
		intransit.setUpdateDate(LocalDateTime.now());
		this.calVolTemplate.update(intransit).subscribe();
		return false;
	}

	private Mono<TouchCallback> touchHoldItemReturnPre(TouchObj touchObj) {
		int holdId = touchObj.getVHoldItem().getHoldId();
		String typeCode = touchObj.getVHoldItem().getTypeCode();
		String sessionId = touchObj.getSessionId();
		int muserId = touchObj.getMuserId();
//		if (ctrlChar > 0)
//			return Mono.just(new TouchCallback(8, "touchInvalidCtrl", this.hiRouteKey,
//					new Object[] { sessionId, vHoldItem.getBarcode(), ctrlChar }));
		if (ANNEX_TYPES.contains(typeCode))
			return this.calVolTemplate.selectOne(query(where("annexHoldId").is(holdId)), AnnexHold.class)
					.map(AnnexHold::getHoldId).defaultIfEmpty(0).map(hId -> new TouchCallback(7, "touchPostAnnexItem",
							this.hiRouteKey, new Object[] { sessionId, holdId, hId, muserId }));
		return Mono.just(this.returnItemIn(touchObj)).filter(b -> b).map(b -> new TouchCallback(6, "touchPostHoldItem",
				this.hiRouteKey, new Object[] { sessionId, holdId, "", touchObj.getTouchLogId(), muserId }));
	}

	private boolean returnItemIn(TouchObj touchObj) {
		List<Integer> siteIds = touchObj.getVHoldClient().getNoIntransitSites();
		int oriSiteId = touchObj.getVHoldItem().getOriSiteId();
		int siteId = touchObj.getVHoldItem().getSiteId();
		return siteIds.contains(oriSiteId) || siteIds.contains(siteId) || touchObj.compFloatTouch();
	}

	private Mono<TouchResult> touchError(String msg) {
		TouchResult touchResult = new TouchResult('E', "error");
		touchResult.setResultClass(String.class);
		touchResult.setResultObject(msg);
		return Mono.just(touchResult);
	}

}
