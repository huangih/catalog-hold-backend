package tw.com.hyweb.cathold.backend.service;

import java.util.Comparator;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.UserStopBooking;
import tw.com.hyweb.cathold.model.view.BookingHistoryView;
import tw.com.hyweb.cathold.model.view.ReaderStopBookingInfo;

@RequiredArgsConstructor
public class UserStopBookingServiceImpl implements UserStopBookingService {

	private final BookingViewService bookingViewService;

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<ReaderStopBookingInfo> getReaderStopBookingInfo(int readerId) {
		return this.calVolTemplate
				.selectOne(query(where("userId").is(readerId).and("available").isTrue()), UserStopBooking.class)
				.map(ReaderStopBookingInfo::new)
				.flatMap(rsbi -> this.bookingViewService
						.getReaderOnStopBookingHistories(readerId, Phase.ON_STOP_BOOKING)
						.sort(Comparator.comparing(BookingHistoryView::getInActiveDate)).collectList().map(li -> {
							rsbi.setBookingHistoryViews(li);
							return rsbi;
						}));
	}

}
