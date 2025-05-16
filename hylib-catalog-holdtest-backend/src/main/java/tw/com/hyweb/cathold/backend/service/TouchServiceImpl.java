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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;
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
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.client.TouchResult;
import tw.com.hyweb.cathold.model.client.VHoldClient;
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

	private static final List<Phase> HADHOLD_BOOKINGPHASE = List.of(Phase.WAIT_ANNEX, Phase.AVAILABLE, Phase.TRANSIT_B);

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

	private final List<Character> prefixChars = List.of('-', '/', '!');

	@Override
	public Mono<TouchResult> touchHoldItem(String barcode, String sessionId, int muserId) {
		return this.vHoldClientService
				.getVHoldClientBySessionId(sessionId).flatMap(vhc -> this.vTouchLogService
						.newTouchLog(barcode, vhc.getId()).map(tl -> Tuples.of(tl, vhc, barcode, muserId, sessionId)))
				.flatMap(this::touchHoldItem);
	}

	private Mono<TouchResult> touchHoldItem(Tuple5<TouchLog, VHoldClient, String, Integer, String> tup5) {
		TouchLog touchLog = tup5.getT1();
		VHoldClient vhc = tup5.getT2();
		Tuple2<Character, String> tuple2 = this.convertInBarcdoe(tup5.getT3());
		char ctrlChar = tuple2.getT1();
		String barcode = tuple2.getT2();
		int muserId = tup5.getT4();
		String sessionId = tup5.getT5();
		if ('!' == ctrlChar)
			return this.rollbackHoldItem(barcode, vhc, sessionId, muserId);
		return this.vHoldItemService.getVHoldItemByBarcode(barcode).flatMap(vh -> {
			SqlserverHoldStatus sqlHold = this.sqlserverHoldStatusRepository.findByHoldId(vh.getHoldId()).orElse(null);
			if (sqlHold != null) {
				touchLog.setStatus(sqlHold.getStatus());
				if (ctrlChar == 0)
					this.amqpBackendClient.touchOverDueWaitingCheck(barcode);
				return this.touchHoldItemPre(vh, ctrlChar, vhc, muserId, sessionId, touchLog).flatMap(tcb -> {
					int type = tcb.getType();
					if (type > 0)
						return this.amqpBackendClient.touchPostProcess(tcb)
								.doOnNext(tr -> this.amqpBackendClient.subWhiteUid(
										barcode)).timeout(Duration.ofSeconds(3),
								this.touchError("cathold.touch.postTouchTimeout"));
					if (type == -1)
						return this.touchError("cathold.touch.preTouchTimeout");
					return this.touchError("cathold.touch.wrongTouchPreCallback");
				});
			}
			return this.touchError("cathold.touch.notExistHoldOnHylib");
		}).switchIfEmpty(this.touchError("cathold.touch.wrongBarcode"))
				.doOnNext(tr -> this.vTouchLogService.updateResultTime(tr, touchLog.getId()));
	}

	private Tuple2<Character, String> convertInBarcdoe(String barcode) {
		char ctrlChar = 0;
		if (this.prefixChars.contains(barcode.charAt(0))) {
			ctrlChar = barcode.charAt(0);
			barcode = barcode.substring(1);
		}
		return Tuples.of(ctrlChar, barcode);
	}

	private Mono<TouchResult> rollbackHoldItem(String barcode, VHoldClient vhc, String sessionId, int muserId) {
		return this.vTouchLogService.rollbackHoldItem(barcode, vhc.getId()).flatMap(obj -> {
			if (obj instanceof TouchLog tl)
				return this.amqpBackendClient.rollBackHoldItem(sessionId, tl, muserId);
			if (obj instanceof TouchResult tr)
				return Mono.just(tr);
			return Mono.empty();
		});
	}

	private Mono<TouchCallback> touchHoldItemPre(VHoldItem vHoldItem, char ctrlChar, VHoldClient vHoldClient,
			int muserId, String sessionId, TouchLog touchLog) {
		return this.touchHoldItemFloatPre(vHoldItem, ctrlChar, vHoldClient, muserId, sessionId)
				.switchIfEmpty(this.touchHoldItemStatusPre(vHoldItem, touchLog.getStatus(), muserId, sessionId))
				.switchIfEmpty(this.touchHoldItemBookingPre(vHoldItem, ctrlChar, muserId, sessionId))
				.switchIfEmpty(this.touchHoldItemTransitPre(vHoldItem, ctrlChar, vHoldClient, muserId, sessionId))
				.switchIfEmpty(this.touchHoldItemReturnPre(vHoldItem, ctrlChar, vHoldClient, touchLog.getId(), muserId,
						sessionId))
				.timeout(Duration.ofSeconds(3), Mono.just(new TouchCallback(-1)))
				.doOnNext(tc -> this.vTouchLogService.updatePreTime(touchLog, tc.getType()));
	}

	private Mono<TouchCallback> touchHoldItemFloatPre(VHoldItem vHoldItem, char ctrlChar, VHoldClient vHoldClient,
			int muserId, String sessionId) {
		int holdId = vHoldItem.getHoldId();
		return Mono.just(ctrlChar == 0 && this.onFloatGroup(vHoldItem, vHoldClient)).mapNotNull(b -> {
			if (Boolean.TRUE.equals(b))
				return new TouchCallback(1, "touchPostFloatHoldItem", this.hiRouteKey,
						new Object[] { sessionId, holdId, muserId });
			return null;
		});
	}

	private boolean onFloatGroup(VHoldItem vHoldItem, VHoldClient vHoldClient) {
		char group = vHoldClient.getFloatGroup();
		String itemGroup = vHoldItem.getFloatGroup();
		if (group == 0 || itemGroup == null || group >= 'a')
			return false;
		return vHoldItem.floatItem() && vHoldClient.isFloatReceive() && group == itemGroup.charAt(0);
	}

	private Mono<TouchCallback> touchHoldItemStatusPre(VHoldItem vHoldItem, String status, int muserId,
			String sessionId) {
		if (!STATUSES.contains(status))
			return Mono.empty();
		final String fstatus = "T8".equals(status) ? "NB" : status;
		int holdId = vHoldItem.getHoldId();
		if ("NB".equals(fstatus))
			this.amqpBackendClient.setHoldItemStatus(holdId, fstatus, muserId);
		return this.calVolTemplate.selectOne(query(where("statusCode").is(fstatus)), ItemStatusDef.class)
				.map(ItemStatusDef::getStatusName)
				.zipWith(this.itemSiteDefService.getClyStrBySiteId(vHoldItem.getSiteId()),
						(sName, clyS) -> sName + "#1 " + clyS)
				.map(s -> new TouchCallback(2, "touchPostStatusHoldItem", this.hiRouteKey,
						new Object[] { sessionId, holdId, s, !"NB".equals(fstatus) }));
	}

	private Mono<TouchCallback> touchHoldItemBookingPre(VHoldItem vh, char ctrlChar, int muserId, String sessionId) {
		return this.calVolTemplate.selectOne(query(where("associateId").is(vh.getHoldId())), Booking.class)
				.switchIfEmpty(this.findBookingFirstPosition(vh.getHoldId(), vh.getCallVolId()))
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

	private Mono<TouchCallback> touchHoldItemTransitPre(VHoldItem vHoldItem, char ctrlChar, VHoldClient vHoldClient,
			int muserId, String sessionId) {
		int holdId = vHoldItem.getHoldId();
		if (ctrlChar == 0 || ctrlChar == '-') {
			List<Integer> siteIds = vHoldClient.getNoIntransitSites();
			return this.calVolTemplate.selectOne(query(where("holdId").is(holdId)), Intransit.class)
					.defaultIfEmpty(new Intransit()).filter(it -> {
						if (it.getHoldId() == 0)
							return true;
						if (siteIds.getFirst() != it.getFromSiteId()) // 非調出館重復點收
							return this.checkTransitPostProcess(it, siteIds, muserId);
						return this.touchTransitPreCheck(ctrlChar, it, siteIds.getFirst(), muserId);
					}).mapNotNull(it -> {
						if (this.transitReturnOut(vHoldItem, vHoldClient)
								&& !ANNEX_TYPES.contains(vHoldItem.getTypeCode()))
							return new TouchCallback(5, "touchPostIntransit", this.btRouteKey, new Object[] { sessionId,
									vHoldItem.getHoldId(), vHoldItem.getOriSiteId(), Phase.TRANSIT_R, null, muserId });// 依原始館藏館設調撥目的館
						return null;
					});

		}
		return Mono.empty();
	}

	private boolean transitReturnOut(VHoldItem vHoldItem, VHoldClient vHoldClient) {
		String group = vHoldItem.getFloatGroup();
		List<Integer> siteIds = vHoldClient.getNoIntransitSites();
		boolean bRet = !siteIds.contains(vHoldItem.getOriSiteId()) && !siteIds.contains(vHoldItem.getSiteId());
		boolean bFloat = vHoldItem.floatItem() && vHoldClient.isFloatReceive() && group != null
				&& vHoldClient.getFloatGroup() == group.charAt(0);
//		if (bRet && !bFloat)
//			log.info("transitReturnOut: {}-{}-{}-{}-{}", vHoldItem.getBarcode(), bRet, vHoldItem.floatItem(),
//					vHoldClient.isFloatReceive(), vHoldClient.getFloatGroup());
		return bRet && !bFloat;
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

	private boolean touchTransitPreCheck(char ctrlChar, Intransit intransit, int siteId, int muserId) {
		if (ctrlChar == '-' && intransit.getPhase() == Phase.TRANSIT_B) {
			intransit.setPhase(Phase.TRANSIT_WA);
			this.amqpBackendClient.moveTransitToHistory(intransit, siteId, muserId);
			return false;
		}
		return true;
	}

	private Mono<TouchCallback> touchHoldItemReturnPre(VHoldItem vHoldItem, char ctrlChar, VHoldClient vHoldClient,
			int touchLogId, int muserId, String sessionId) {
		int holdId = vHoldItem.getHoldId();
		if (ctrlChar > 0)
			return Mono.just(new TouchCallback(8, "touchInvalidCtrl", this.hiRouteKey,
					new Object[] { sessionId, vHoldItem.getBarcode(), ctrlChar }));
		if (ANNEX_TYPES.contains(vHoldItem.getTypeCode()))
			return this.calVolTemplate.selectOne(query(where("annexHoldId").is(holdId)), AnnexHold.class)
					.map(AnnexHold::getHoldId).defaultIfEmpty(0).map(hId -> new TouchCallback(7, "touchPostAnnexItem",
							this.hiRouteKey, new Object[] { sessionId, holdId, hId, muserId }));
		return this.returnItemIn(vHoldItem, vHoldClient).filter(b -> b).map(b -> new TouchCallback(6,
				"touchPostHoldItem", this.hiRouteKey, new Object[] { sessionId, holdId, "", touchLogId, muserId }));
	}

	private Mono<Boolean> returnItemIn(VHoldItem vHoldItem, VHoldClient vHoldClient) {
		String group = vHoldItem.getFloatGroup();
		List<Integer> siteIds = vHoldClient.getNoIntransitSites();
		boolean bFloatIn = vHoldClient.isFloatReceive() && group != null
				&& vHoldClient.getFloatGroup() == group.charAt(0) && vHoldItem.floatItem();
//		if (bFloatIn)
//			log.info("transitReturnOut: {}-{}-{}-{}", vHoldItem.getBarcode(), vHoldItem.floatItem(),
//					vHoldClient.isFloatReceive(), vHoldClient.getFloatGroup());
		return Mono.just(
				siteIds.contains(vHoldItem.getOriSiteId()) || siteIds.contains(vHoldItem.getSiteId()) || bFloatIn);
	}

	private Mono<TouchResult> touchError(String msg) {
		TouchResult touchResult = new TouchResult('E', "error");
		touchResult.setResultClass(String.class);
		touchResult.setResultObject(msg);
		return Mono.just(touchResult);
	}

}
