package tw.com.hyweb.cathold.backend.controller;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public interface CatHoldManagerService {

	Mono<ServerResponse> refreshAllRuleStatuses(ServerRequest request);

	Mono<ServerResponse> setNotHotBooking(ServerRequest request);

	Mono<ServerResponse> bookingPickupSiteClose(ServerRequest request);

	Mono<ServerResponse> bookingPickupSiteReopen(ServerRequest request);

	Mono<ServerResponse> siteUidBarcodeCount(ServerRequest request);

	Mono<ServerResponse> correctAnnexStatus(ServerRequest request);

	Mono<ServerResponse> delRedisCache(ServerRequest request);

	Mono<ServerResponse> getClySiteDest(ServerRequest request);

	Mono<ServerResponse> getBookingViewForNcl(ServerRequest request);

	Mono<ServerResponse> aduItemCtrlRules(ServerRequest request);

	Mono<ServerResponse> refreshHoldFromHylib(ServerRequest request);
	
}
