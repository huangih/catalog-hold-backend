package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;

import lombok.RequiredArgsConstructor;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import java.util.Collection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VMarcCallVolumeService;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.McvBookingStatus;
import tw.com.hyweb.cathold.model.view.BookingStatusView;
import tw.com.hyweb.cathold.model.view.TodayBookingInfo;

@RequiredArgsConstructor
public class BookingStatusViewServiceImpl implements BookingStatusViewService {

	private final VMarcCallVolumeService vMarcCallVolumeService;

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Flux<BookingStatusView> getBookingStatuses(int rate, int reqNum, int supNum, int waitdays) {
		return this.calVolTemplate
				.select(query(where("rate").greaterThanOrEquals(rate).and("requestNum").greaterThanOrEquals(reqNum)
						.and("supportNum").greaterThanOrEquals(supNum).and("waitDays").greaterThanOrEquals(waitdays)),
						McvBookingStatus.class)
				.flatMap(mbs -> this.vMarcCallVolumeService.getMarcVolumeByCallVolId(mbs.getCallVolId()).map(mv -> {
					BookingStatusView bsv = new BookingStatusView(mbs);
					bsv.setMarcVolume(mv);
					return bsv;
				}));
	}

	@Override
	public Mono<TodayBookingInfo> getTodayBookingInfo() {
		TodayBookingInfo todayBookingInfo = new TodayBookingInfo();
		return this.calVolTemplate
				.select(query(where("placeDate").greaterThan(todayBookingInfo.getToday().atStartOfDay())),
						Booking.class)
				.collectMultimap(Booking::getUserId, Booking::getId).flatMap(mmap -> {
					todayBookingInfo.setBookingUserNum(mmap.size());
					return Flux.fromIterable(mmap.values()).map(Collection::size).reduce((bs1, bs2) -> bs1 + bs2)
							.map(n -> {
								todayBookingInfo.setBookingNum(n);
								return todayBookingInfo;
							});
				});
	}

}
