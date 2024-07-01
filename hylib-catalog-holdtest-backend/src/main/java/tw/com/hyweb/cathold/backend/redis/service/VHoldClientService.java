package tw.com.hyweb.cathold.backend.redis.service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.lang.NonNull;

import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.service.HoldClientPropConverter;
import tw.com.hyweb.cathold.backend.service.ItemLocationDefService;
import tw.com.hyweb.cathold.backend.service.ItemSiteDefService;
import tw.com.hyweb.cathold.backend.service.ItemTypeDefService;
import tw.com.hyweb.cathold.model.ClientSequence;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.client.GiveSeqProp;
import tw.com.hyweb.cathold.model.client.NoticeProp;
import tw.com.hyweb.cathold.model.client.SeqRange;
import tw.com.hyweb.cathold.model.client.VHoldClient;

@Service
@RequiredArgsConstructor
public class VHoldClientService {

	private static final String HOLDCLIENT_PREFIX = "hc:holdClientId:%d:holdClient";

	private static final String SITE_CODE = "siteCode";

	private final ItemSiteDefService itemSiteDefService;

	private final ItemTypeDefService itemTypeDefService;

	private final ItemLocationDefService itemLocationDefService;

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<VHoldClient> getVHoldClientById(int holdClientId) {
		String idString = String.format(HOLDCLIENT_PREFIX, holdClientId);
		return this.redisUtils.getMonoFromRedis(idString, null).map(VHoldClient.class::cast)
				.switchIfEmpty(this.redisUtils.getMonoFromDatabase(idString,
						() -> this.calVolTemplate.selectOne(query(where("id").is(holdClientId)), HoldClient.class)
								.flatMap(this::convertHoldClient),
						null));
	}

	private Mono<VHoldClient> convertHoldClient(@NonNull HoldClient holdClient) {
		VHoldClient vhc = new VHoldClient(holdClient.getId(), holdClient.isTransitDouble());
		vhc.setAvailSeqRange(new SeqRange(holdClient.getSeqRange()));
		return Mono.justOrEmpty(holdClient.getNoIntransitSites())
				.flatMap(s -> this.itemSiteDefService.getOrderIdsByCodes(List.of(s.split(","))))
				.filter(siteIds -> !siteIds.isEmpty())
				.switchIfEmpty(this.itemSiteDefService.getIdByCode(holdClient.getSiteCode()).map(List::of))
				.flatMap(siteIds -> {
					vhc.setNoIntransitSites(siteIds);
					return Mono.justOrEmpty(holdClient.getGiveSeqProp());
				}).flatMap(this::parseGiveSeqProp).flatMap(gsp -> {
					vhc.setGiveSeqProp(gsp);
					return this.convertNoticeProp(holdClient.getNoticeTypes(), this.itemTypeDefService);
				}).switchIfEmpty(this.convertNoticeProp(holdClient.getNoticeTypes(), this.itemTypeDefService))
				.flatMap(nTypeMap -> {
					vhc.setNoticeTypesMap(nTypeMap);
					return this.convertNoticeProp(holdClient.getNoticeSites(), this.itemSiteDefService);
				}).flatMap(nSiteMap -> {
					vhc.setNoticeTypesMap(nSiteMap);
					return this.convertNoticeProp(holdClient.getNoticeLocations(), this.itemLocationDefService);
				}).map(nLocMap -> {
					vhc.setNoticeLocsMap(nLocMap);
					return vhc;
				});
	}

	private Mono<GiveSeqProp> parseGiveSeqProp(@NonNull String s) {
		GiveSeqProp giveSeqProp = new GiveSeqProp();
		return Mono.just(s).flatMap(s1 -> {
			int inx = this.getDelimitIndex(s1);
			if (inx > 0) {
				giveSeqProp.setSiteProp(inx, s1);
				return this.itemSiteDefService.getIdsByCodes(giveSeqProp.getSiteCodes()).map(siteIds -> {
					giveSeqProp.setSiteIds(siteIds);
					return s1.substring(inx + 1);
				});
			}
			if (inx < 0 || s1.length() <= inx)
				return Mono.empty();
			return Mono.just(s1.substring(inx + 1));
		}).flatMap(s2 -> {
			int inx = this.getDelimitIndex(s2);
			if (inx > 0)
				giveSeqProp.setAnnexProp(inx, s2);
			if (inx < 0 || s2.length() <= inx)
				return Mono.empty();
			return Mono.just(s2.substring(inx + 1));
		}).flatMap(s3 -> {
			int inx = this.getDelimitIndex(s3);
			if (inx > 0) {
				giveSeqProp.setTypeProp(inx, s3);
				return this.itemTypeDefService.getIdsByCodes(giveSeqProp.getTypeCodes()).map(typeIds -> {
					giveSeqProp.setTypeIds(typeIds);
					return s3.substring(inx + 1);
				});
			}
			if (inx < 0 || s3.length() <= inx)
				return Mono.empty();
			return Mono.just(s3.substring(inx + 1));
		}).flatMap(s4 -> {
			int inx = this.getDelimitIndex(s4);
			if (inx > 0) {
				giveSeqProp.setLocProp(inx, s4);
				return this.itemLocationDefService.getIdsByCodes(giveSeqProp.getLocCodes()).map(typeIds -> {
					giveSeqProp.setLocIds(typeIds);
					return giveSeqProp;
				});
			}
			return Mono.empty();
		}).defaultIfEmpty(giveSeqProp);
	}

	private int getDelimitIndex(String s) {
		if (s.length() == 0)
			return -1;
		int inx1 = s.indexOf('|');
		int inx2 = s.indexOf('^');
		if (inx1 >= 0) {
			if (inx2 >= 0)
				return Integer.min(inx1, inx2);
			return inx1;
		} else if (inx2 >= 0)
			return inx2;
		return s.length();
	}

	private Mono<Map<Integer, NoticeProp>> convertNoticeProp(String s,
			@NonNull HoldClientPropConverter noticePropConverter) {
		if (s != null && s.length() > 2)
			return Flux.fromArray(s.split(",")).map(NoticeProp::new).flatMap(noticePropConverter::setNoticePropId)
					.collectMap(NoticeProp::getId);
		return Mono.just(new HashMap<>());
	}

	public Mono<List<HoldClient>> getHoldClientsBySiteCode(String siteCode) {
		return this.calVolTemplate.select(query(where(SITE_CODE).is(siteCode)), HoldClient.class)
				.filter(hc -> !hc.getName().endsWith("-SIP2"))
				.flatMap(hc -> this.getSeqNumById(hc.getId()).map(ClientSequence::getSeqNum).map(seqNum -> {
					hc.setCurrentSeq(seqNum);
					return hc;
				})).collectList();
	}

	private Mono<ClientSequence> getSeqNumById(int holdClientId) {
		return this.calVolTemplate.selectOne(query(where("id").is(holdClientId)), ClientSequence.class)
				.switchIfEmpty(this.getVHoldClientById(holdClientId)
						.map(vhc -> new ClientSequence(holdClientId, vhc.getAvailSeqRange().getMinNum()))
						.flatMap(this.calVolTemplate::insert));
	}

	public Mono<VHoldClient> getHoldClientBySessionId(String sessionId) {
		return Mono.justOrEmpty(sessionId.split("_")[0]).map(Integer::parseInt).flatMap(this::getVHoldClientById);
	}

	public Mono<HoldClient> addHoldClient(HoldClient holdClient) {
		return this.calVolTemplate
				.selectOne(query(where(SITE_CODE).is(holdClient.getSiteCode()).and("name").is(holdClient.getName())),
						HoldClient.class)
				.switchIfEmpty(this.calVolTemplate.insert(holdClient).doOnNext(this::redisVHoldClient))
				.flatMap(hc -> this.getSeqNumById(hc.getId()).map(ClientSequence::getSeqNum).map(seqNum -> {
					hc.setCurrentSeq(seqNum);
					return hc;
				}));
	}

	public Mono<HoldClient> updateHoldClient(HoldClient nhc, Integer seqNum) {
		return this.calVolTemplate.selectOne(query(where("id").is(nhc.getId())), HoldClient.class).flatMap(hc -> {
			this.copyProperty("name", nhc, hc);
			this.copyProperty("noIntransitSites", nhc, hc);
			this.copyProperty("giveSeqProp", nhc, hc);
			this.copyProperty("seqRange", nhc, hc);
			this.copyProperty("noticeSites", nhc, hc);
			this.copyProperty("noticeTypes", nhc, hc);
			this.copyProperty("noticeLocations", nhc, hc);
			this.copyProperty("transitDouble", nhc, hc);
			return this.setHoldClientSeqNum(nhc.getId(), seqNum).map(ClientSequence::getSeqNum).flatMap(sn -> {
				hc.setCurrentSeq(sn);
				return this.calVolTemplate.update(hc);
			});
		}).doOnNext(this::redisVHoldClient);
	}

	private void redisVHoldClient(HoldClient holdClient) {
		String idString = String.format(HOLDCLIENT_PREFIX, holdClient.getId());
		this.redisUtils.redisLockCache(idString, this.convertHoldClient(holdClient), null).subscribe();
	}

	private void copyProperty(@NonNull String property, @NonNull HoldClient source, @NonNull HoldClient target) {
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(source.getClass(), property);
		if (pd != null) {
			try {
				Object object = pd.getReadMethod().invoke(source);
				if (object != null)
					pd.getWriteMethod().invoke(target, object);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	private Mono<ClientSequence> setHoldClientSeqNum(int holdClientId, Integer seqNum) {
		return this.getVHoldClientById(holdClientId).filter(vhc -> seqNum != null).map(vhc -> {
			SeqRange seqRange = vhc.getAvailSeqRange();
			if (seqRange.getMinNum() > seqNum)
				return seqRange.getMinNum();
			if (seqRange.getMaxNum() < seqNum)
				return seqRange.getMaxNum();
			return seqNum;
		}).flatMap(sn -> this.getSeqNumById(holdClientId).map(cs -> {
			cs.setSeqNum(sn);
			return cs;
		}).flatMap(this.calVolTemplate::update));
	}

}
