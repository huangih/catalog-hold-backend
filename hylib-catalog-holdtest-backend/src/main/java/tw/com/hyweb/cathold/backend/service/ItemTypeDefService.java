package tw.com.hyweb.cathold.backend.service;

import java.util.List;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.ItemTypeDef;
import tw.com.hyweb.cathold.model.client.NoticeProp;

@Service
@RequiredArgsConstructor
public class ItemTypeDefService implements HoldClientPropConverter {

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<NoticeProp> setNoticePropId(NoticeProp noticeProp) {
		return this.calVolTemplate.selectOne(query(where("itemTypeCode").is(noticeProp.getCode())), ItemTypeDef.class)
				.map(typeDef -> {
					noticeProp.setId(typeDef.getItemTypeId());
					return noticeProp;
				}).defaultIfEmpty(noticeProp);
	}

	public Mono<List<Integer>> getIdsByCodes(List<String> typeCodes) {
		return this.calVolTemplate.select(query(where("itemTypeCode").in(typeCodes)), ItemTypeDef.class)
				.map(ItemTypeDef::getItemTypeId).collectList();
	}

}
