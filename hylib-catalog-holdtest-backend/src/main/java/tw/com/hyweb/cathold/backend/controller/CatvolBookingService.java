package tw.com.hyweb.cathold.backend.controller;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public interface CatvolBookingService {

	Mono<ServerResponse> touchHoldItem(ServerRequest request);

	Mono<ServerResponse> addHoldClient(ServerRequest request);

	Mono<ServerResponse> updateHoldClient(ServerRequest request);

}
