package tw.com.hyweb.cathold.backend.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.BookingDueDate;
import tw.com.hyweb.cathold.model.BookingHistory;
import tw.com.hyweb.cathold.model.BookingResult;
import tw.com.hyweb.cathold.model.Intransit;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.MarcCallVolume;
import tw.com.hyweb.cathold.model.NoticeSmsRule;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.TouchCallback;
import tw.com.hyweb.cathold.model.TouchLog;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;
import tw.com.hyweb.cathold.model.VBookingAvailation;
import tw.com.hyweb.cathold.model.client.TouchResult;

@Component
@Slf4j
@RequiredArgsConstructor
public class AmqpBackendClient {

	private static final String FUNC_NAME = "funcName";

	@Value("${cathold.rfidctl.routekey}")
	private String rdRouteKey;

	@Value("${cathold.bookingTransit.routekey}")
	private String btRouteKey;

	@Value("${cathold.holditem.routekey}")
	private String hiRouteKey;

	@Value("${cathold.marccallvol.routekey}")
	private String mcvRouteKey;

	@Value("${cathold.nnotice.routekey}")
	private String neRouteKey;

	@Value("${cathold.typesiteloc.routekey}")
	private String itslRouteKey;

	private final RabbitTemplate template;

	private final AsyncAmqpTemplate asyncAmqpTemplate;

	private Message setFuncNameHeader(Message message, String funcName) {
		message.getMessageProperties().getHeaders().put(FUNC_NAME, funcName);
		return message;
	}

	public Mono<TouchResult> touchPostProcess(TouchCallback touchCallback) {
		String funcName = touchCallback.getFuncName();
		String routeKey = touchCallback.getRouteKey();
		Object[] args = touchCallback.getArgs();
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(routeKey, args,
				m -> this.setFuncNameHeader(m, funcName), new ParameterizedTypeReference<TouchResult>() {
				}));
	}

	public Mono<TradeoffStopBookingResult> tradeoffStopBookingDays(int userId, int tradeoffDays, int muserId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Integer[] { userId, tradeoffDays, muserId },
				m -> this.setFuncNameHeader(m, "tradeoffStopBookingDays"),
				new ParameterizedTypeReference<TradeoffStopBookingResult>() {
				}));
	}

	public void addWhiteUid(int holdId) {
		this.template.convertAndSend(this.rdRouteKey, holdId, m -> this.setFuncNameHeader(m, "addWhiteUid"));
	}

	public void postBookingLendCallback(LendCallback lendCallback) {
		this.template.convertAndSend(this.btRouteKey, lendCallback,
				m -> this.setFuncNameHeader(m, "postBookingLendCallback"));
	}

	public Mono<Boolean> checkUidByBarcode(String barcode) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.rdRouteKey, barcode,
				m -> this.setFuncNameHeader(m, "checkUidByBarcode"), new ParameterizedTypeReference<Boolean>() {
				}));
	}

	public Mono<Boolean> onMobileLendSite(String siteCode) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.rdRouteKey, siteCode,
				m -> this.setFuncNameHeader(m, "onMobileLendSite"), new ParameterizedTypeReference<Boolean>() {
				}));
	}

	public Mono<TouchResult> rollBackHoldItem(String sessionId, TouchLog touchLog, int muserId) {
		Object[] args = new Object[] { sessionId, touchLog, muserId };
		this.template.convertAndSend(this.btRouteKey, args, m -> this.setFuncNameHeader(m, "rollBackHoldItem"));
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.hiRouteKey, args,
				m -> this.setFuncNameHeader(m, "rollBackHoldItem"), new ParameterizedTypeReference<TouchResult>() {
				}));
	}

	public Mono<List<MarcCallVolume>> getMarcCallVolumesByMarcId(int marcId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.mcvRouteKey, marcId,
				m -> this.setFuncNameHeader(m, "getMarcCallVolumesByMarcId"),
				new ParameterizedTypeReference<List<MarcCallVolume>>() {
				}));
	}

	public Mono<String> getbookingViewsForNcl(LocalDate begDate, LocalDate endDate) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Object[] { begDate, endDate }, m -> this.setFuncNameHeader(m, "getbookingViewsForNcl"),
				new ParameterizedTypeReference<String>() {
				}));
	}

	public Mono<String> refreshAllRuleStatuses() {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.itslRouteKey, "",
				m -> this.setFuncNameHeader(m, "refreshAllRuleStatuses"), new ParameterizedTypeReference<String>() {
				}));
	}

	public Mono<Boolean> setNotHotBooking(int callVolId, boolean notHotBooking) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.mcvRouteKey,
				new Object[] { callVolId, notHotBooking }, m -> this.setFuncNameHeader(m, "setNotHotBooking"),
				new ParameterizedTypeReference<Boolean>() {
				}));
	}

	public Mono<String> bookingPickupSiteClose(String closeSite, String toSite) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new String[] { closeSite, toSite }, m -> this.setFuncNameHeader(m, "bookingPickupSiteClose"),
				new ParameterizedTypeReference<String>() {
				}));
	}

	public Mono<String> bookingPickupSiteReopen(String closeSite, LocalDate changeDate) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Object[] { closeSite, changeDate }, m -> this.setFuncNameHeader(m, "bookingPickupSiteReopen"),
				new ParameterizedTypeReference<String>() {
				}));
	}

	public Mono<String> countUidBarcodeBySite(String siteCode) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.rdRouteKey, siteCode,
				m -> this.setFuncNameHeader(m, "countUidBarcodeBySite"), new ParameterizedTypeReference<String>() {
				}));
	}

	public Mono<Integer> correctAnnexStatus(int marcId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.hiRouteKey, marcId,
				m -> this.setFuncNameHeader(m, "correctAnnexStatus"), new ParameterizedTypeReference<Integer>() {
				}));
	}

	public void setHoldItemTempStatus(int holdId, int tempStatus) {
		log.info("setHoldItemTempStatus: {}-{}", holdId, tempStatus);
		this.template.convertAndSend(this.hiRouteKey, new Integer[] { holdId, tempStatus },
				m -> this.setFuncNameHeader(m, "setHoldItemTempStatus"));
	}

	public Mono<String> aduItemCtrlRules(Boolean b) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.itslRouteKey, b,
				m -> this.setFuncNameHeader(m, "aduItemCtrlRules"), new ParameterizedTypeReference<String>() {
				})).timeout(Duration.ofSeconds(10), Mono.just("timeout"));
	}

	public void setHoldItemStatus(int holdId, String status, int muserId) {
		Object[] args = new Object[] { holdId, status, muserId };
		this.template.convertAndSend(this.hiRouteKey, args, m -> this.setFuncNameHeader(m, "setHoldItemStatus"));
	}

	public void moveTransitToHistory(Intransit intransit, int siteId, int muserId) {
		Object[] args = new Object[] { intransit, siteId, muserId };
		this.template.convertAndSend(this.btRouteKey, args, m -> this.setFuncNameHeader(m, "moveTransitToHistory"));
	}

	public void subWhiteUid(String barcode) {
		this.template.convertAndSend(this.rdRouteKey, barcode, m -> this.setFuncNameHeader(m, "subWhiteUid"));
	}

	public void touchOverDueWaitingCheck(String barcode) {
		this.template.convertAndSend(this.btRouteKey, barcode,
				m -> this.setFuncNameHeader(m, "touchOverDueWaitingCheck"));
	}

	public void touchDistribution(int holdId, Phase phase) {
		this.template.convertAndSend(this.btRouteKey, new Object[] { holdId, phase },
				m -> this.setFuncNameHeader(m, "touchDistribution"));
	}

	public Mono<String> refreshHoldFromHylib(String barcode) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.hiRouteKey, barcode,
				m -> this.setFuncNameHeader(m, "refreshHoldFromHylib"), new ParameterizedTypeReference<String>() {
				}));
	}

	public Mono<BookingResult> placeBooking(int readerId, int callVolId, int pickupSiteId, String comment,
			int muserId) {
		Object[] args = new Object[] { readerId, callVolId, pickupSiteId, comment, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "placeBooking"), new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<BookingResult> placeCopyBooking(int readerId, int holdId, int pickupSiteId, String comment,
			int muserId) {
		Object[] args = new Object[] { readerId, holdId, pickupSiteId, comment, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "placeCopyBooking"), new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<BookingResult> suspendBooking(int readerId, LocalDate begDate, LocalDate endDate, int muserId) {
		Object[] args = new Object[] { readerId, begDate, endDate, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "suspendBooking"), new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<BookingResult> cancelBooking(int readerId, long bookingId, String comment, boolean override,
			int muserId) {
		Object[] args = new Object[] { readerId, bookingId, comment, override, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "cancelBooking"), new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<BookingResult> updateBookingSiteDueDate(long bookingId, int readerId, int pickupSiteId,
			LocalDate dueDate, String comment, int muserId) {
		Object[] args = new Object[] { bookingId, readerId, pickupSiteId, dueDate, comment, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "updateBookingSiteDueDate"),
				new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<BookingResult> cancelSuspendBooking(int readerId, int muserId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Integer[] { readerId, muserId }, m -> this.setFuncNameHeader(m, "cancelSuspendBooking"),
				new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<List<NoticeSmsRule>> updateNoticeSmsRule(NoticeSmsRule nsr) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.neRouteKey, nsr,
				m -> this.setFuncNameHeader(m, "updateNoticeSmsRule"),
				new ParameterizedTypeReference<List<NoticeSmsRule>>() {
				}));
	}

	public Mono<List<NoticeSmsRule>> delNoticeSmsRule(int ruleId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.neRouteKey, ruleId,
				m -> this.setFuncNameHeader(m, "delNoticeSmsRule"),
				new ParameterizedTypeReference<List<NoticeSmsRule>>() {
				}));
	}

	public Mono<BookingResult> expandAvailDueDate(int readerId, long bookingId, int muserId) {
		Object[] args = new Object[] { readerId, bookingId, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "expandAvailDueDate"), new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<List<BookingDueDate>> addDueDateRule(LocalDate begDate, LocalDate endDate, LocalDate dueDate,
			String siteCode, int expOper, boolean overJustify) {
		Object[] args = new Object[] { begDate, endDate, dueDate, siteCode, expOper, overJustify };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "addDueDateRule"),
				new ParameterizedTypeReference<List<BookingDueDate>>() {
				}));
	}

	public Mono<List<NoticeSmsRule>> addNoticeSmsRule(NoticeSmsRule noticeSmsRule) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.neRouteKey, noticeSmsRule,
				m -> this.setFuncNameHeader(m, "addNoticeSmsRule"),
				new ParameterizedTypeReference<List<NoticeSmsRule>>() {
				}));
	}

	public void rollbackBookingDueDateRule(BookingDueDate bookingDueDate) {
		this.template.convertAndSend(this.btRouteKey, bookingDueDate.getId(),
				m -> this.setFuncNameHeader(m, "rollbackBookingDueDateRule"));
	}

	public Mono<List<VBookingAvailation>> getBookingsBySeqNum(String siteCode, int seqNum, Integer type) {
		Object[] args = new Object[] { siteCode, seqNum, type < 0 ? null : type };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "getBookingsBySeqNum"),
				new ParameterizedTypeReference<List<VBookingAvailation>>() {
				}));
	}

	public Mono<BookingHistory> cancelOverdueBooking(long bookingId, int readerId, String comment, int muserId) {
		Object[] args = new Object[] { bookingId, readerId, comment, muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "cancelOverdueBooking"),
				new ParameterizedTypeReference<BookingHistory>() {
				}));
	}

	public Mono<List<Booking>> getBookingsByItemId(int callVolId, int skip, int take) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Integer[] { callVolId, skip, take }, m -> this.setFuncNameHeader(m, "getBookingsByItemId"),
				new ParameterizedTypeReference<List<Booking>>() {
				}));
	}

	public Mono<BookingResult> rollbackFillBooking(long bookingId, int muserId) {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey,
				new Object[] { bookingId, muserId }, m -> this.setFuncNameHeader(m, "rollbackFillBooking"),
				new ParameterizedTypeReference<BookingResult>() {
				}));
	}

	public Mono<List<Integer>> getOverdaysTransitHoldIds() {
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, LocalDateTime.now(),
				m -> this.setFuncNameHeader(m, "getOverdaysTransitHoldIds"),
				new ParameterizedTypeReference<List<Integer>>() {
				}));
	}

	public Mono<BookingResult> editReaderBookingCallVol(long bookingId, int callVolId, String action, int muserId) {
		Object[] args = new Object[] { bookingId, callVolId, action.charAt(0), muserId };
		return Mono.fromFuture(this.asyncAmqpTemplate.convertSendAndReceiveAsType(this.btRouteKey, args,
				m -> this.setFuncNameHeader(m, "editReaderBookingCallVol"),
				new ParameterizedTypeReference<BookingResult>() {
				}));
	}
}
