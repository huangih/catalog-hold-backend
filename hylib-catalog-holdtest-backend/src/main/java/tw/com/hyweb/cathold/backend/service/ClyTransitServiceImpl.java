package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.model.Intransit;
import tw.com.hyweb.cathold.model.VIntransitBooking;
import tw.com.hyweb.cathold.sqlserver.model.MarcDetail;
import tw.com.hyweb.cathold.sqlserver.repository.MarcDetailRepository;

@RequiredArgsConstructor
public class ClyTransitServiceImpl implements ClyTransitService {

	private final VHoldItemService vHoldItemService;

	private final MarcDetailRepository marcDetailRepository;

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<String> getClyTransitSiteDest(String barcode) {
		return this.vHoldItemService.getVHoldItemByBarcode(barcode)
				.flatMap(vh -> Mono.justOrEmpty(this.marcDetailRepository.findByMarcId(vh.getMarcId()))
						.map(MarcDetail::getTitle).map(s -> {
							String s1 = s.trim();
							return s1.length() > 16 ? s1.substring(0, 16) : s1;
						}))
				.zipWith(
						this.calVolTemplate.selectOne(
								query(where("barcode").is(barcode)).columns("holdId", "barcode", "siteCode", "clyMark"),
								VIntransitBooking.class).doOnNext(this::setRelayDate),
						(title, vtb) -> vtb.getBarcode() + "|" + title + "|" + vtb.getClyMark() + "|"
								+ vtb.getSiteCode()).defaultIfEmpty("Not Found.");
	}

	private void setRelayDate(VIntransitBooking vIntransitBooking) {
		this.calVolTemplate.selectOne(query(where("holdId").is(vIntransitBooking.getHoldId())), Intransit.class)
				.subscribe(it -> {
					it.setRelayDate(LocalDateTime.now());
					it.setUpdateDate(LocalDateTime.now());
					this.calVolTemplate.update(it).subscribe();
				});
	}
}
