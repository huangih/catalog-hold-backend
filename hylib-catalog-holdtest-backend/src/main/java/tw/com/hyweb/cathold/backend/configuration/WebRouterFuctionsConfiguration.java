package tw.com.hyweb.cathold.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import org.springframework.web.reactive.function.server.ServerResponse;

import tw.com.hyweb.cathold.backend.controller.CatHoldManagerService;
import tw.com.hyweb.cathold.backend.controller.CatvolBookingService;

@Configuration
public class WebRouterFuctionsConfiguration {

	@Bean
	RouterFunction<ServerResponse> bookingConrolRouter(CatvolBookingService catvolBookingService) {
		return RouterFunctions.nest(RequestPredicates.path("/bookingControl"),
				this.catvolBookingRouter(catvolBookingService));
	}

	private RouterFunction<ServerResponse> catvolBookingRouter(CatvolBookingService catvolBookingService) {
		return RouterFunctions.route(GET("/getReaderBookingViews"), catvolBookingService::getReaderBookingViews)
				.andRoute(GET("/getReadersBookingViews"), catvolBookingService::getReadersBookingViews)
				.andRoute(GET("/getReaderBookingSummary"), catvolBookingService::getReaderBookingSummary)
				.andRoute(GET("/getReaderBookingHistories"), catvolBookingService::getReaderBookingHistories)
				.andRoute(GET("/getReaderStopBookingInfo"), catvolBookingService::getReaderStopBookingInfo)
				.andRoute(GET("/getReaderSuspendBooking"), catvolBookingService::getReaderSuspendBooking)
				.andRoute(GET("/readerCanLendHold"), catvolBookingService::readerCanLendHold)
				.andRoute(POST("/lendCheckCallback"), catvolBookingService::lendCheckCallback)
				.andRoute(GET("/canRenewLends"), catvolBookingService::canRenewLends)
				.andRoute(POST("/placeBooking"), catvolBookingService::placeBooking)
				.andRoute(POST("/suspendBooking"), catvolBookingService::suspendBooking)
				.andRoute(POST("/cancelBooking"), catvolBookingService::cancelBooking)
				.andRoute(POST("/updateBookingSiteDueDate"), catvolBookingService::updateBookingSiteDueDate)
				.andRoute(POST("/cancelSuspendBooking"), catvolBookingService::cancelSuspendBooking)
				.andRoute(GET("/getCallVolHoldSummariesByMarcId"),
						catvolBookingService::getCallVolHoldSummariesByMarcId)
				.andRoute(GET("/findCallVolHoldSummaryByCallVol/{callVolId}/{readerId}"),
						catvolBookingService::findCallVolHoldSummaryByCallVol)
				.andRoute(GET("/touchHoldItem"), catvolBookingService::touchHoldItem)
				.andRoute(GET("/catholdClientList"), catvolBookingService::getHoldClientsBySiteCode)
				.andRoute(GET("/getHoldAvailBookings"), catvolBookingService::getHoldAvailBookings)
				.andRoute(GET("/getReaderAvailBookings"), catvolBookingService::getReaderAvailBookings)
				.andRoute(GET("/getSiteOvarAvailBookingWaitings"),
						catvolBookingService::getSiteOvarAvailBookingWaitings)
				.andRoute(GET("/getSiteOvarAvailBookingSeqNum"), catvolBookingService::getSiteOvarAvailBookingSeqNum)
				.andRoute(GET("/getSiteBookingDistributions"), catvolBookingService::getSiteBookingDistributions)
				.andRoute(GET("/getReaderExpandDuesOnMonth"), catvolBookingService::getReaderExpandDuesOnMonth)
				.andRoute(POST("/expandAvailDueDate"), catvolBookingService::expandAvailDueDate)
				.andRoute(POST("/addDueDateRule"), catvolBookingService::addDueDateRule)
				.andRoute(POST("/addBookingCloseDate"), catvolBookingService::addBookingCloseDate)
				.andRoute(POST("/addNoticeSmsRule"), catvolBookingService::addNoticeSmsRule)
				.andRoute(GET("/getBookingDueDateRules"), catvolBookingService::getBookingDueDateRules)
				.andRoute(GET("/getBookingCloseDates"), catvolBookingService::getBookingCloseDates)
				.andRoute(GET("/getNoticeSmsRules"), catvolBookingService::getNoticeSmsRules)
				.andRoute(POST("/rollbackBookingDueDateRule"), catvolBookingService::rollbackBookingDueDateRule)
				.andRoute(DELETE("/delBookingCloseDate"), catvolBookingService::delBookingCloseDate)
				.andRoute(DELETE("/delNoticeSmsRule"), catvolBookingService::delNoticeSmsRule)
				.andRoute(PATCH("/updBookingCloseDate"), catvolBookingService::updBookingCloseDate)
				.andRoute(PATCH("/updateNoticeSmsRule"), catvolBookingService::updateNoticeSmsRule)
				.andRoute(POST("/addHoldClient"), catvolBookingService::addHoldClient)
				.andRoute(POST("/updateHoldClient"), catvolBookingService::updateHoldClient)
				.andRoute(GET("/getBookingsBySeqNum"), catvolBookingService::getBookingsBySeqNum)
				.andRoute(POST("/cancelOverdueBooking"), catvolBookingService::cancelOverdueBooking)
				.andRoute(GET("/getNoSeqBookingAvailation"), catvolBookingService::getNoSeqBookingAvailation)
				.andRoute(GET("/getWaitComfirmTransits"), catvolBookingService::getWaitComfirmTransits)
				.andRoute(GET("/getBookingViewsByHoldId"), catvolBookingService::getBookingViewsByHoldId)
				.andRoute(GET("/getIntransitByHoldId"), catvolBookingService::getIntransitByHoldId)
				.andRoute(GET("/getOverdaysTransitesBySiteId"), catvolBookingService::getOverdaysTransitesBySiteId)
				.andRoute(GET("/getBookingsDueDateAfter"), catvolBookingService::getBookingsDueDateAfter)
				.andRoute(GET("/getBookingsAvailDateBetween"), catvolBookingService::getBookingsAvailDateBetween)
				.andRoute(GET("/getSiteAvailableDateBookings"), catvolBookingService::getSiteAvailableDateBookings)
				.andRoute(GET("/getBookingsOverDueDateBetween"), catvolBookingService::getBookingsOverDueDateBetween)
				.andRoute(GET("/getBookingNoticeResult"), catvolBookingService::getBookingNoticeResult)
				.andRoute(POST("/rollbackFillBooking"), catvolBookingService::rollbackFillBooking)
				.andRoute(GET("/getBookingStatuses"), catvolBookingService::getBookingStatuses)
				.andRoute(GET("/getOverdaysTransitHoldIds"), catvolBookingService::getOverdaysTransitHoldIds)
				.andRoute(GET("/getTransitOverdaysStatic"), catvolBookingService::getTransitOverdaysStatic)
				.andRoute(GET("/getTransitOverdaysStaticView"), catvolBookingService::getTransitOverdaysStaticView)
				.andRoute(GET("/getDistriFoundStatics"), catvolBookingService::getDistriFoundStatics)
				.andRoute(POST("/editReaderBookingCallVol"), catvolBookingService::editReaderBookingCallVol)
				.andRoute(POST("/tradeoffStopBookingDays"), catvolBookingService::tradeoffStopBookingDays);
	}

	@Bean
	RouterFunction<ServerResponse> catholdManagerRouter(CatHoldManagerService catHoldManagerService) {
		return RouterFunctions.nest(RequestPredicates.path("/managerControl"),
				this.catHoldManagerRouters(catHoldManagerService));
	}

	private RouterFunction<ServerResponse> catHoldManagerRouters(CatHoldManagerService catHoldManagerService) {
		return RouterFunctions.route(GET("/refreshAllRuleStatuses"), catHoldManagerService::refreshAllRuleStatuses)
				.andRoute(POST("/setNotHotBooking"), catHoldManagerService::setNotHotBooking)
				.andRoute(GET("/bookingPickupSiteClose"), catHoldManagerService::bookingPickupSiteClose)
				.andRoute(GET("/bookingPickupSiteReopen"), catHoldManagerService::bookingPickupSiteReopen)
				.andRoute(GET("/siteUidBarcodeCount"), catHoldManagerService::siteUidBarcodeCount)
				.andRoute(GET("/correctAnnexStatus"), catHoldManagerService::correctAnnexStatus)
				.andRoute(GET("/delRedisCache"), catHoldManagerService::delRedisCache)
				.andRoute(GET("/getClySiteDest"), catHoldManagerService::getClySiteDest)
				.andRoute(GET("/getBookingViewForNcl"), catHoldManagerService::getBookingViewForNcl)
				.andRoute(GET("/aduItemCtrlRules"), catHoldManagerService::aduItemCtrlRules)
				.andRoute(GET("/refreshHoldFromHylib"), catHoldManagerService::refreshHoldFromHylib);
	}
}
