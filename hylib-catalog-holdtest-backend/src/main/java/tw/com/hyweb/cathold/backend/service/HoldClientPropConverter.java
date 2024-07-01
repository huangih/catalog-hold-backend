package tw.com.hyweb.cathold.backend.service;

import java.util.List;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.client.NoticeProp;

public interface HoldClientPropConverter {

	Mono<NoticeProp> setNoticePropId(NoticeProp noticeProp);

	Mono<List<Integer>> getIdsByCodes(List<String> codes);
}
