package tw.com.hyweb.cathold.backend.service;

import java.util.List;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.TplLocationDef;
import tw.com.hyweb.cathold.model.client.NoticeProp;

@Service
@RequiredArgsConstructor
public class ItemLocationDefService implements HoldClientPropConverter {

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<NoticeProp> setNoticePropId(NoticeProp noticeProp) {
	return	this.calVolTemplate.selectOne(query(where("locationCode").is(noticeProp.getCode())), TplLocationDef.class).map(locationDef->{
			noticeProp.setId(locationDef.getId());
			return noticeProp;
		}).defaultIfEmpty(noticeProp);
	}

	@Override
	public Mono<List<Integer>> getIdsByCodes(List<String> locCodes) {
		return this.calVolTemplate.select(query(where("locationCode").in(locCodes)),TplLocationDef.class).map(TplLocationDef::getId)
				.collectList();
	}

}
