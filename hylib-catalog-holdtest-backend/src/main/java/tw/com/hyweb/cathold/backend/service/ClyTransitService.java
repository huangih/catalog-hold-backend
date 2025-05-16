package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;

public interface ClyTransitService {

	Mono<String> getClyTransitSiteDest(String barcode);

}
