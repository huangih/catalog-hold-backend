package tw.com.hyweb.cathold.backend.redis.service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.lang.NonNull;

import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
		return this.redisUtils.getMonoFromRedis(idString, Duration.ofMinutes(30)).map(VHoldClient.class::cast)
				.switchIfEmpty(this.calVolTemplate.selectOne(query(where("id").is(holdClientId)), HoldClient.class)
						.flatMap(this::convertHoldClient)
						.doOnNext(vhc -> this.redisUtils.redisLockCache(idString, vhc, Duration.ofMinutes(30))));
	}

	private Mono<VHoldClient> convertHoldClient(@NonNull HoldClient holdClient) {
		return this.itemSiteDefService.getSiteDefBySiteCode(holdClient.getSiteCode()).flatMap(siteDef -> {
			VHoldClient vhc = new VHoldClient(siteDef, holdClient);
			vhc.setAvailSeqRange(new SeqRange(holdClient.getSeqRange()));
			return this.itemSiteDefService.getOrderIdsByCodes(List.of(holdClient.getNoIntransitSites().split(",")))
					.map(siteIds -> {
						if (siteIds.isEmpty())
							siteIds = List.of(siteDef.getSiteId());
						vhc.setNoIntransitSites(siteIds);
						return holdClient.getGiveSeqProp();
					}).flatMap(s -> this.parseGiveSeqProp(s).map(gsp -> {
						vhc.setGiveSeqProp(gsp);
						return this.nextParameter(holdClient.getNoticeTypes());
					})).flatMap(s1 -> this.convertNoticeProp(s1, itemTypeDefService).map(nTypeMap -> {
						vhc.setNoticeTypesMap(nTypeMap);
						return this.nextParameter(holdClient.getNoticeSites());
					})).flatMap(s2 -> this.convertNoticeProp(s2, itemSiteDefService).map(nSiteMap -> {
						vhc.setNoticeSitesMap(nSiteMap);
						return this.nextParameter(holdClient.getNoticeLocations());
					})).flatMap(s3 -> this.convertNoticeProp(s3, itemLocationDefService).map(nLocsMap -> {
						vhc.setNoticeLocsMap(nLocsMap);
						return vhc;
					}).defaultIfEmpty(vhc));
		});
	}

	private String nextParameter(String parameter) {
		return parameter != null ? parameter : "";
	}

	private Mono<GiveSeqProp> parseGiveSeqProp(@NonNull String s) {
		GiveSeqProp giveSeqProp = new GiveSeqProp();
		return Mono.just(this.getDelimitIndex(s)).filterWhen(index -> {
			if (index > 0) {
				giveSeqProp.setSiteProp(index, s);
				return this.itemSiteDefService.getIdsByCodes(giveSeqProp.getSiteCodes()).map(siteIds -> {
					giveSeqProp.setSiteIds(siteIds);
					return s.length() > index;
				});
			}
			return Mono.just(index >= 0 && s.length() > index);
		}).map(index -> s.substring(index + 1)).flatMap(s1 -> Mono.just(this.getDelimitIndex(s1)).filterWhen(inx1 -> {
			if (inx1 > 0)
				giveSeqProp.setAnnexProp(inx1, s1);
			return Mono.just(inx1 >= 0 && s1.length() > inx1);
		}).map(inx1 -> s1.substring(inx1 + 1))).flatMap(s2 -> Mono.just(this.getDelimitIndex(s2)).filterWhen(inx2 -> {
			if (inx2 > 0) {
				giveSeqProp.setTypeProp(inx2, s2);
				return this.itemTypeDefService.getIdsByCodes(giveSeqProp.getTypeCodes()).map(typeIds -> {
					giveSeqProp.setTypeIds(typeIds);
					return s2.length() > inx2;
				});
			}
			return Mono.just(inx2 >= 0 && s2.length() > inx2);
		}).map(inx2 -> s2.substring(inx2 + 1))).flatMap(s3 -> Mono.just(this.getDelimitIndex(s3)).filterWhen(inx3 -> {
			if (inx3 > 0) {
				giveSeqProp.setLocProp(inx3, s3);
				return this.itemLocationDefService.getIdsByCodes(giveSeqProp.getLocCodes()).map(locIds -> {
					giveSeqProp.setLocIds(locIds);
					return true;
				});
			}
			return Mono.just(false);
		}).map(inx3 -> giveSeqProp)).defaultIfEmpty(giveSeqProp);
	}

	private int getDelimitIndex(String s) {
		if (s.isEmpty())
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

	public Flux<HoldClient> getHoldClientsBySiteCode(String siteCode) {
		return this.calVolTemplate.select(query(where(SITE_CODE).is(siteCode)), HoldClient.class)
				.filter(hc -> !hc.getName().endsWith("-SIP2"))
				.flatMap(hc -> this.getSeqNumById(hc.getId()).map(ClientSequence::getSeqNum).map(seqNum -> {
					hc.setCurrentSeq(seqNum);
					return hc;
				}));
	}

	private Mono<ClientSequence> getSeqNumById(int holdClientId) {
		return this.calVolTemplate.selectOne(query(where("id").is(holdClientId)), ClientSequence.class)
				.switchIfEmpty(this.getVHoldClientById(holdClientId)
						.map(vhc -> new ClientSequence(holdClientId, vhc.getAvailSeqRange().getMinNum()))
						.flatMap(this.calVolTemplate::insert));
	}

	public Mono<VHoldClient> getVHoldClientBySessionId(String sessionId) {
		return Mono.justOrEmpty(sessionId.split("_")[0]).map(Integer::parseInt).flatMap(this::getVHoldClientById);
	}

	public Mono<HoldClient> addHoldClient(HoldClient holdClient) {
		log.info("{}", holdClient);
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
			this.copyProperty("toFloatLoc", nhc, hc);
			if (seqNum != null && seqNum >= 0)
				return this.setHoldClientSeqNum(nhc.getId(), seqNum).map(ClientSequence::getSeqNum).map(sn -> {
					hc.setCurrentSeq(sn);
					return hc;
				});
			return Mono.just(hc);
		}).flatMap(this.calVolTemplate::update).doOnNext(this::redisVHoldClient);
	}

	private void redisVHoldClient(HoldClient holdClient) {
		String idString = String.format(HOLDCLIENT_PREFIX, holdClient.getId());
		this.convertHoldClient(holdClient)
				.subscribe(vhc -> this.redisUtils.redisLockCache(idString, vhc, Duration.ofMinutes(30)));
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
		return this.getVHoldClientById(holdClientId).filter(Objects::nonNull).map(vhc -> {
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
