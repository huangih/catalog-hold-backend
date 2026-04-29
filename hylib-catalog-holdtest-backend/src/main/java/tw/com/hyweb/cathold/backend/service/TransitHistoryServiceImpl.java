package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;

import java.util.Comparator;

import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.WrongTransit;
import tw.com.hyweb.cathold.model.view.WrongTransitList;

@RequiredArgsConstructor
public class TransitHistoryServiceImpl implements TransitHistoryService {

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<WrongTransitList> getWrongTransitList(int totalNum) {
		return this.calVolTemplate.count(query(where("diffGroup").isTrue()), WrongTransit.class).map(Long::intValue)
				.map(wNum -> new WrongTransitList(totalNum, wNum))
				.flatMap(wtl -> this.calVolTemplate.select(WrongTransit.class).all()
						.sort(Comparator.comparingInt(WrongTransit::getId)).collectList().map(li -> {
							wtl.setWrongTransits(li);
							return wtl;
						}));
	}

}
