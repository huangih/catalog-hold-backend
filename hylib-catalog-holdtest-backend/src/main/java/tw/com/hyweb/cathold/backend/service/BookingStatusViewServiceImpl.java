package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;

import lombok.RequiredArgsConstructor;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import reactor.core.publisher.Flux;
import tw.com.hyweb.cathold.backend.redis.service.VMarcCallVolumeService;
import tw.com.hyweb.cathold.model.McvBookingStatus;
import tw.com.hyweb.cathold.model.view.BookingStatusView;

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

}
