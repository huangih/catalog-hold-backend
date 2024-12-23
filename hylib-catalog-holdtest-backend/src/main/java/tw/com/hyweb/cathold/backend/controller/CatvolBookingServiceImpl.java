package tw.com.hyweb.cathold.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.service.TouchClientService;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.client.TouchResult;

@RequiredArgsConstructor
public class CatvolBookingServiceImpl implements CatvolBookingService {

	private final VHoldClientService vHoldClientService;

	private final TouchClientService touchClientService;

	@Override
	public Mono<ServerResponse> touchHoldItem(ServerRequest request) {
		Mono<String> param0 = Mono.justOrEmpty(request.queryParam("barcode"))
				.map(code -> code.length() > 30 ? code.substring(0, 30) : code).filter(code -> code.length() > 2);
		Mono<String> param1 = Mono.justOrEmpty(request.queryParam("sessionId"));
		Mono<Integer> param2 = Mono.justOrEmpty(request.queryParam("muserId")).map(Integer::parseInt)
				.defaultIfEmpty(500);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(Mono.zip(param0, param1, param2).flatMap(
						tup3 -> this.touchClientService.touchHoldItem(tup3.getT1(), tup3.getT2(), tup3.getT3())),
						TouchResult.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> addHoldClient(ServerRequest request) {
		Mono<HoldClient> mono = Mono.justOrEmpty(request.queryParam("siteCode"))
				.zipWith(Mono.justOrEmpty(request.queryParam("name")),
						(siteCode, name) -> HoldClient.builder().siteCode(siteCode).name(name).noIntransitSites("")
								.giveSeqProp(siteCode).seqRange("1-600#0").noticeTypes("HOT-BOOK#2,HOT-BA#3")
								.noticeSites("").noticeLocations("18Y#1,BSP#2").transitDouble(false).build())
				.flatMap(hc -> {
					request.queryParam("noIntransitSites").ifPresent(hc::setNoIntransitSites);
					request.queryParam("giveSeqProp").ifPresent(hc::setGiveSeqProp);
					request.queryParam("seqRange").ifPresent(hc::setSeqRange);
					request.queryParam("noticeTypes").ifPresent(hc::setNoticeTypes);
					request.queryParam("noticeSites").ifPresent(hc::setNoticeSites);
					request.queryParam("noticeLocations").ifPresent(hc::setNoticeLocations);
					request.queryParam("transitDouble").ifPresent(s -> hc.setTransitDouble(Boolean.parseBoolean(s)));
					return this.vHoldClientService.addHoldClient(hc);
				});
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, HoldClient.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	@Override
	public Mono<ServerResponse> updateHoldClient(ServerRequest request) {
		Mono<Object> mono = Mono.justOrEmpty(request.queryParam("clientId")).map(Integer::parseInt).map(HoldClient::new)
				.flatMap(hc -> {
					request.queryParam("name").ifPresent(hc::setName);
					request.queryParam("noIntransitSites").ifPresent(hc::setNoIntransitSites);
					request.queryParam("giveSeqProp").ifPresent(hc::setGiveSeqProp);
					request.queryParam("seqRange").ifPresent(hc::setSeqRange);
					request.queryParam("noticeTypes").ifPresent(hc::setNoticeTypes);
					request.queryParam("noticeSites").ifPresent(hc::setNoticeSites);
					request.queryParam("noticeLocations").ifPresent(hc::setNoticeLocations);
					request.queryParam("transitDouble").ifPresent(s -> hc.setTransitDouble(Boolean.parseBoolean(s)));
					String s = request.queryParam("seqNum").orElse(null);
					Integer seqNum = s != null ? Integer.valueOf(s) : null;
					return this.vHoldClientService.updateHoldClient(hc, seqNum);
				});
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono, HoldClient.class)
				.switchIfEmpty(ServerResponse.notFound().build());
	}

}
