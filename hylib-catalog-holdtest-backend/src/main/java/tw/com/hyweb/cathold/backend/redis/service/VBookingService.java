package tw.com.hyweb.cathold.backend.redis.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.ItemSiteDef;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.VHoldItem;

@Service
@RequiredArgsConstructor
public class VBookingService {

	private static final String ITEMID_BOOKINGS_LIST = "bi:callVolId:%d:bookingIds";

	private static final String ALLPICKUPSITE_KEY = "bi:pickupSites:siteDefs";

	private static final List<Phase> WAITBOOKING_PHASES = List.of(Phase.PLACE, Phase.SUSPENSION, Phase.DISTRIBUTION);

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<List<String>> findBookingIdsByItemId(int itemId) {
		String idString = String.format(ITEMID_BOOKINGS_LIST, itemId);
		return this.redisUtils.getMonoListFromRedis(idString, String.class, true, null)
				.switchIfEmpty(this.redisUtils.getMonoListFromDatabase(idString, String.class, true, () -> this
						.getVCallvolBookingsFromDb(itemId).map(Booking::getId).map(String::valueOf).collectList(),
						null));
	}

	public Flux<Booking> getVCallvolBookingsFromDb(int callVolId) {
		return this.calVolTemplate
				.select(query(where("type").is("T").and("itemId").is(callVolId).and("phase").in(WAITBOOKING_PHASES)),
						Booking.class)
				.sort(Comparator.comparing(Booking::getPlaceDate).thenComparing(Booking::getOldId));
	}

	public Mono<List<ItemSiteDef>> findAllPickupSites() {
		return this.redisUtils.getMonoListFromRedis(ALLPICKUPSITE_KEY, ItemSiteDef.class, false, null)
				.switchIfEmpty(this.redisUtils.getMonoListFromDatabase(ALLPICKUPSITE_KEY, ItemSiteDef.class, false,
						() -> this.calVolTemplate.select(ItemSiteDef.class).all().filter(ItemSiteDef::canPickup)
								.sort(Comparator.comparing(ItemSiteDef::getSiteCode)).collectList(),
						null, LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
	}

	public Mono<ItemSiteDef> getSiteDefBySiteId(int siteId) {
		return this.findAllPickupSites().flatMapMany(Flux::fromIterable).filter(sd -> sd.getSiteId() == siteId).next()
				.defaultIfEmpty(new ItemSiteDef());
	}

	public Mono<Long> getAllowBookingNumByCallVolId(int callVolId) {
		return this.calVolTemplate.select(query(where("callVolId").is(callVolId)), VHoldItem.class)
				.filter(VHoldItem::nonShadow).filter(VHoldItem::allowBooking).count();
	}

}
