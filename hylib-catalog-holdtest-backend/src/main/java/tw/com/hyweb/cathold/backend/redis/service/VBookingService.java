package tw.com.hyweb.cathold.backend.redis.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.ItemSiteDef;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.VCallvolBooking;
import tw.com.hyweb.cathold.model.VHoldItem;

@Service
@RequiredArgsConstructor
@Slf4j
public class VBookingService {

	private static final String ITEMID_BOOKINGS_LIST = "bi:callVolId:%d:bookingIds";

	private static final String ALLPICKUPSITE_KEY = "bi:pickupSites:siteDefs";

	private static final String DUEDATE_AFTER_BOOKINGS = "bi:days:%s:%d:bookings";

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

	private Flux<ItemSiteDef> findAllPickupSites() {
		return this.redisUtils.getFluxFromRedis(ALLPICKUPSITE_KEY, false, null).cast(ItemSiteDef.class)
				.switchIfEmpty(this.refreshAllPickupSites());
	}

	private Flux<ItemSiteDef> refreshAllPickupSites() {
		return this.calVolTemplate.select(ItemSiteDef.class).all().filter(ItemSiteDef::canPickup)
				.sort(Comparator.comparing(ItemSiteDef::getSiteCode)).collectList()
				.doOnNext(li -> this.redisUtils.redisListCache(ALLPICKUPSITE_KEY, li, LocalDate.now()))
				.flatMapMany(Flux::fromIterable);
	}

	public Mono<ItemSiteDef> getSiteDefBySiteId(int siteId) {
		return this.findAllPickupSites().filter(sd -> sd.getSiteId() == siteId).next()
				.defaultIfEmpty(new ItemSiteDef());
	}

	public Mono<Long> getAllowBookingNumByCallVolId(int callVolId) {
		return this.calVolTemplate.select(query(where("callVolId").is(callVolId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).filter(VHoldItem::allowBooking).count();
	}

	public Flux<Integer> getBookingUserIdsByDueDateAfter(int days) {
		String idString = String.format(DUEDATE_AFTER_BOOKINGS, LocalDate.now().toString(), days);
		Flux<Booking> flux = this.calVolTemplate.select(
				query(where("dueDate").is(LocalDate.now().plusDays(days)).and("phase").is(Phase.AVAILABLE)),
				Booking.class);
		return this.redisUtils.getRMultimapKeySet(idString, flux, Booking::getUserId, Duration.ofSeconds(30))
				.flatMapMany(Flux::fromIterable).sort();
	}

	public Flux<Booking> getBookingsByUserIdDuedateAfter(int userId, int days) {
		String idString = String.format(DUEDATE_AFTER_BOOKINGS, LocalDate.now().toString(), days);
		return this.redisUtils.getRMultimapValues(idString, userId, Duration.ofSeconds(30));
	}

}
