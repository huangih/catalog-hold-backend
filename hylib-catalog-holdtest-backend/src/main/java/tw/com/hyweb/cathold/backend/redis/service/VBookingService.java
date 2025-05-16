package tw.com.hyweb.cathold.backend.redis.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.ItemSiteDef;
import tw.com.hyweb.cathold.model.VCallvolBooking;
import tw.com.hyweb.cathold.model.VHoldItem;

@Service
@RequiredArgsConstructor
public class VBookingService {

	private static final String ITEMID_BOOKINGS_LIST = "bi:callVolId:%d:bookingIds";

	private static final String ALLPICKUPSITE_KEY = "bi:pickupSites:siteDefs";

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<List<String>> findBookingIdsByItemId(int itemId) {
		String idString = String.format(ITEMID_BOOKINGS_LIST, itemId);
		return this.redisUtils.getMonoListFromRedis(idString, String.class, true, null)
				.switchIfEmpty(this.getVCallvolBookingsFromDb(itemId).map(vcb -> String.valueOf(vcb.getId()))
						.collectList().doOnNext(li -> this.redisUtils.redisLockCache(idString, li, null)));
	}

	private Flux<VCallvolBooking> getVCallvolBookingsFromDb(int callVolId) {
		return this.calVolTemplate.select(query(where("itemId").is(callVolId)).sort(Sort.by("placeDate", "oldId")),
				VCallvolBooking.class);
	}

	public Mono<List<ItemSiteDef>> findAllPickupSites() {
		return this.redisUtils.getMonoListFromRedis(ALLPICKUPSITE_KEY, ItemSiteDef.class, false, null)
				.switchIfEmpty(this.calVolTemplate.select(ItemSiteDef.class).all().filter(ItemSiteDef::canPickup)
						.sort(Comparator.comparing(ItemSiteDef::getSiteCode)).collectList()
						.doOnNext(li -> this.redisUtils.redisListCache(ALLPICKUPSITE_KEY, li, LocalDate.now())));
	}

	public Mono<ItemSiteDef> getSiteDefBySiteId(int siteId) {
		return this.findAllPickupSites().flatMapIterable(Function.identity()).filter(sd -> sd.getSiteId() == siteId)
				.next().defaultIfEmpty(new ItemSiteDef());
	}

	public Mono<Long> getAllowBookingNumByCallVolId(int callVolId) {
		return this.calVolTemplate.select(query(where("callVolId").is(callVolId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).filter(VHoldItem::allowBooking).count();
	}

}
