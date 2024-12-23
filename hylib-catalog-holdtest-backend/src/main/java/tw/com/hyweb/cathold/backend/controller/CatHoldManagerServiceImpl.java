package tw.com.hyweb.cathold.backend.controller;

import java.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tw.com.hyweb.cathold.backend.redis.service.ReactiveRedisUtils;
import tw.com.hyweb.cathold.backend.service.AmqpBackendClient;
import tw.com.hyweb.cathold.backend.service.BookingViewService;
import tw.com.hyweb.cathold.backend.service.AmqpStreamService;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.view.BookingNclView;
import tw.com.hyweb.cathold.model.view.BookingView;

@RequiredArgsConstructor
@Slf4j
public class CatHoldManagerServiceImpl implements CatHoldManagerService {

	private final BookingViewService bookingViewService;

	private final AmqpStreamService amqpStreamService;

	private final AmqpBackendClient amqpBackendClient;

	private final ReactiveRedisUtils redisUtils;

	@Override
	public Mono<ServerResponse> refreshAllRuleStatuses(ServerRequest request) {
		return ServerResponse.ok().contentType(MediaType.TEXT_HTML)
				.body(this.amqpBackendClient.rerefreshAllRuleStatuses(), BookingNclView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> setNotHotBooking(ServerRequest request) {
		Mono<Integer> argMono0 = Mono.justOrEmpty(request.queryParam("callVolId")).map(Integer::parseInt);
		Mono<Boolean> argMono1 = Mono.justOrEmpty(request.queryParam("notHotBooking")).map(Boolean::parseBoolean);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(Mono.zip(argMono0, argMono1).flatMapMany(
						tup2 -> this.amqpBackendClient.setNotHotBooking(tup2.getT1(), tup2.getT2())), Boolean.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> bookingPickupSiteClose(ServerRequest request) {
		Mono<String> argMono0 = Mono.justOrEmpty(request.queryParam("closeSite"));
		Mono<String> argMono1 = Mono.justOrEmpty(request.queryParam("toSite"));
		Flux<BookingView> flux = Mono.zip(argMono0, argMono1)
				.flatMapMany(tup2 -> this.amqpBackendClient.bookingPickupSiteClose(tup2.getT1(), tup2.getT2())
						.flatMapMany(qName -> this.amqpStreamService.createListenerContainer(qName)
								.publishOn(Schedulers.immediate(), 1).filter(Booking.class::isInstance)
								.cast(Booking.class).flatMap(bookingViewService::convert2BookingView)
								.doOnComplete(() -> this.amqpStreamService.deleteStream(qName))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> bookingPickupSiteReopen(ServerRequest request) {
		Mono<String> argMono0 = Mono.justOrEmpty(request.queryParam("closeSite"));
		Mono<LocalDate> argMono1 = Mono.justOrEmpty(request.queryParam("changeDate")).map(LocalDate::parse);
		Flux<BookingView> flux = Mono.zip(argMono0, argMono1)
				.flatMapMany(tup2 -> this.amqpBackendClient.bookingPickupSiteReopen(tup2.getT1(), tup2.getT2())
						.flatMapMany(qName -> this.amqpStreamService.createListenerContainer(qName)
								.publishOn(Schedulers.immediate(), 1).filter(Booking.class::isInstance)
								.cast(Booking.class).flatMap(bookingViewService::convert2BookingView)
								.doOnComplete(() -> this.amqpStreamService.deleteStream(qName))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> siteUidBarcodeCount(ServerRequest request) {
		Mono<String> mono = Mono.justOrEmpty(request.queryParam("siteCode"))
				.flatMap(this.amqpBackendClient::countUidBarcodeBySite);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, String.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> correctAnnexStatus(ServerRequest request) {
		Mono<String> mono = Mono.justOrEmpty(request.queryParam("marcId")).map(Integer::parseInt)
				.flatMap(marcId -> this.amqpBackendClient.correctAnnexStatus(marcId)
						.map(num -> marcId + " had sent " + num + "holds"));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, String.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> delRedisCache(ServerRequest request) {
		Mono.justOrEmpty(request.queryParam("key")).subscribe(this.redisUtils::unlinkKeys);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(true)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> getBookingViewForNcl(ServerRequest request) {
		Mono<LocalDate> argMono0 = Mono.justOrEmpty(request.queryParam("begDate")).map(LocalDate::parse);
		Mono<LocalDate> argMono1 = Mono.justOrEmpty(request.queryParam("endDate")).map(LocalDate::parse);
		Flux<BookingNclView> flux = Mono.zip(argMono0, argMono1)
				.flatMapMany(tup2 -> this.amqpBackendClient.getbookingViewsForNcl(tup2.getT1(), tup2.getT2())
						.flatMapMany(qName -> this.amqpStreamService.createListenerContainer(qName)
								.publishOn(Schedulers.immediate(), 1).filter(Booking.class::isInstance)
								.cast(Booking.class).flatMap(bookingViewService::convert2BookingNclView)
								.doOnComplete(() -> this.amqpStreamService.deleteStream(qName))));
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(flux, BookingNclView.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

}
