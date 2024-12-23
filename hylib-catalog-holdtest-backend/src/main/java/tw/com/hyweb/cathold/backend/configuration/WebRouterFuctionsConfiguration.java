package tw.com.hyweb.cathold.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

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
		return RouterFunctions.route(GET("/touchHoldItem"), catvolBookingService::touchHoldItem)
				.andRoute(POST("/addHoldClient"), catvolBookingService::addHoldClient)
				.andRoute(POST("/updateHoldClient"), catvolBookingService::updateHoldClient)
//				.andRoute(GET("/reportDoorThrough.do"), catvolBookingService::reportDoorThrough)
//				.andRoute(GET("/renewWhiteUid.do"), catvolBookingService::renewWhiteUid)
//				.andRoute(GET("/siteUidBarcodeCount.do/{siteCode}"), catvolBookingService::siteUidBarcodeCount)
		;
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
				.andRoute(GET("/getBookingViewForNcl"), catHoldManagerService::getBookingViewForNcl);
	}
}
