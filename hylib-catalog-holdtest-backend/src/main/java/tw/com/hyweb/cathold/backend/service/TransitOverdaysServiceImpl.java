package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.TransitOverdays;

@RequiredArgsConstructor
public class TransitOverdaysServiceImpl implements TransitOverdaysService {

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<Boolean> existsNonTouchByHoldId(int holdId) {
		return this.calVolTemplate.exists(query(where("holdId").is(holdId).and("touchTime").isNull()),
				TransitOverdays.class);
	}

}
