package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.view.WrongTransitList;

public interface TransitHistoryService {

	Mono<WrongTransitList> getWrongTransitList(int totalNum);

}
