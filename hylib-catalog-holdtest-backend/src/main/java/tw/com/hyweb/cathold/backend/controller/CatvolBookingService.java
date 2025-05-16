package tw.com.hyweb.cathold.backend.controller;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public interface CatvolBookingService {

	Mono<ServerResponse> getReaderBookingViews(ServerRequest request);

	Mono<ServerResponse> getReadersBookingViews(ServerRequest request);

	Mono<ServerResponse> getReaderBookingSummary(ServerRequest request);

	Mono<ServerResponse> getReaderBookingHistories(ServerRequest request);

	Mono<ServerResponse> getReaderStopBookingInfo(ServerRequest request);

	Mono<ServerResponse> getReaderSuspendBooking(ServerRequest request);

	Mono<ServerResponse> readerCanLendHold(ServerRequest request);

	Mono<ServerResponse> lendCheckCallback(ServerRequest request);

	Mono<ServerResponse> canRenewLends(ServerRequest request);

	Mono<ServerResponse> placeBooking(ServerRequest request);

	Mono<ServerResponse> suspendBooking(ServerRequest request);

	Mono<ServerResponse> cancelBooking(ServerRequest request);

	Mono<ServerResponse> updateBookingSiteDueDate(ServerRequest request);

	Mono<ServerResponse> cancelSuspendBooking(ServerRequest request);

	Mono<ServerResponse> getCallVolHoldSummariesByMarcId(ServerRequest request);

	Mono<ServerResponse> findCallVolHoldSummaryByCallVol(ServerRequest request);

	Mono<ServerResponse> touchHoldItem(ServerRequest request);

	Mono<ServerResponse> getHoldClientsBySiteCode(ServerRequest request);

	Mono<ServerResponse> getHoldAvailBookings(ServerRequest request);

	Mono<ServerResponse> getReaderAvailBookings(ServerRequest request);

	Mono<ServerResponse> getSiteOvarAvailBookingWaitings(ServerRequest request);

	Mono<ServerResponse> getSiteOvarAvailBookingSeqNum(ServerRequest request);

	Mono<ServerResponse> getSiteBookingDistributions(ServerRequest request);

	Mono<ServerResponse> getReaderExpandDuesOnMonth(ServerRequest request);

	Mono<ServerResponse> expandAvailDueDate(ServerRequest request);

	Mono<ServerResponse> addDueDateRule(ServerRequest request);

	Mono<ServerResponse> addBookingCloseDate(ServerRequest request);

	Mono<ServerResponse> addNoticeSmsRule(ServerRequest request);

	Mono<ServerResponse> getBookingDueDateRules(ServerRequest request);

	Mono<ServerResponse> getBookingCloseDates(ServerRequest request);

	Mono<ServerResponse> getNoticeSmsRules(ServerRequest request);

	Mono<ServerResponse> rollbackBookingDueDateRule(ServerRequest request);

	Mono<ServerResponse> delBookingCloseDate(ServerRequest request);

	Mono<ServerResponse> delNoticeSmsRule(ServerRequest request);

	Mono<ServerResponse> updBookingCloseDate(ServerRequest request);

	Mono<ServerResponse> updateNoticeSmsRule(ServerRequest request);

	Mono<ServerResponse> addHoldClient(ServerRequest request);

	Mono<ServerResponse> updateHoldClient(ServerRequest request);

	Mono<ServerResponse> getBookingsBySeqNum(ServerRequest request);

	Mono<ServerResponse> cancelOverdueBooking(ServerRequest request);

	Mono<ServerResponse> getNoSeqBookingAvailation(ServerRequest request);

	Mono<ServerResponse> getWaitComfirmTransits(ServerRequest request);

	Mono<ServerResponse> getBookingViewsByHoldId(ServerRequest request);

	Mono<ServerResponse> getIntransitByHoldId(ServerRequest request);

	Mono<ServerResponse> getOverdaysTransitesBySiteId(ServerRequest request);

	Mono<ServerResponse> getBookingsDueDateAfter(ServerRequest request);

	Mono<ServerResponse> getBookingsAvailDateBetween(ServerRequest request);

	Mono<ServerResponse> getSiteAvailableDateBookings(ServerRequest request);

	Mono<ServerResponse> getBookingsOverDueDateBetween(ServerRequest request);

	Mono<ServerResponse> getBookingNoticeResult(ServerRequest request);

	Mono<ServerResponse> rollbackFillBooking(ServerRequest request);

	Mono<ServerResponse> getBookingStatuses(ServerRequest request);

	Mono<ServerResponse> getOverdaysTransitHoldIds(ServerRequest request);

	Mono<ServerResponse> getTransitOverdaysStatic(ServerRequest request);

	Mono<ServerResponse> getTransitOverdaysStaticView(ServerRequest request);

	Mono<ServerResponse> getDistriFoundStatics(ServerRequest request);

	Mono<ServerResponse> editReaderBookingCallVol(ServerRequest request);

	Mono<ServerResponse> tradeoffStopBookingDays(ServerRequest request);

}
