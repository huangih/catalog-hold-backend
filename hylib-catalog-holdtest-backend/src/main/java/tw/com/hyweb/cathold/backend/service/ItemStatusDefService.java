package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.ItemStatusDef;

@Component
@RequiredArgsConstructor
public class ItemStatusDefService {

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<String> getItemStatusCodeName(String statusCode) {
		return this.calVolTemplate.selectOne(query(where("statusCode").is(statusCode)), ItemStatusDef.class)
				.map(statusDef -> statusCode + "_" + statusDef.getStatusName()).defaultIfEmpty(statusCode);
	}
}
