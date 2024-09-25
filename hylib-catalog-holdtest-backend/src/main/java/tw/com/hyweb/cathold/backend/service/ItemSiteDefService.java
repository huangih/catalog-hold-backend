package tw.com.hyweb.cathold.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.ItemSiteDef;
import tw.com.hyweb.cathold.model.client.NoticeProp;

@Service
@RequiredArgsConstructor
public class ItemSiteDefService implements HoldClientPropConverter {

	private static final String SITE_CODE = "siteCode";

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<NoticeProp> setNoticePropId(NoticeProp noticeProp) {
		return this.calVolTemplate.selectOne(query(where(SITE_CODE).is(noticeProp.getCode())), ItemSiteDef.class)
				.map(siteDef -> {
					noticeProp.setId(siteDef.getSiteId());
					return noticeProp;
				}).defaultIfEmpty(noticeProp);
	}

	@Override
	public Mono<List<Integer>> getIdsByCodes(List<String> siteCodes) {
		return this.calVolTemplate.select(query(where(SITE_CODE).in(siteCodes)), ItemSiteDef.class)
				.map(ItemSiteDef::getSiteId).collectList();
	}

	public Mono<List<Integer>> getOrderIdsByCodes(List<String> siteCodes) {
		return Flux.fromIterable(siteCodes)
				.flatMapSequential(code -> this.calVolTemplate
						.selectOne(query(where(SITE_CODE).is(code)), ItemSiteDef.class).map(ItemSiteDef::getSiteId))
				.collectList();
	}

//	public String getCodeById(int siteId) {
//		String siteCode = "Unknown";
//		ItemSiteDef siteDef = this.itemSiteDefRepository.findById(siteId).orElse(null);
//		if (siteDef != null)
//			siteCode = siteDef.getSiteCode();
//		return siteCode;
//	}
//
//	public boolean allowExpandDueDateBySiteIdAndAvailDate(int siteId, LocalDateTime availDate) {
//		ItemSiteDef itemSiteDef = this.itemSiteDefRepository.findBySiteId(siteId).orElse(null);
//		if (itemSiteDef != null)
//			return itemSiteDef.isExpandAvail() && availDate.toLocalDate().isBefore(itemSiteDef.getAvailDate());
//		return true;
//	}
//
	public Mono<Integer> getIdByCode(String siteCode) {
		return this.calVolTemplate.selectOne(query(where(SITE_CODE).is(siteCode)), ItemSiteDef.class)
				.map(ItemSiteDef::getSiteId);
	}

	public Mono<Boolean> allowExpandDueDateBySiteIdAndAvailDate(int pickupSiteId, LocalDateTime availableDate) {
		return this.calVolTemplate.selectOne(query(where("siteId").is(pickupSiteId)), ItemSiteDef.class)
				.map(siteDef -> siteDef.canExpand(availableDate)).defaultIfEmpty(true);
	}

}
