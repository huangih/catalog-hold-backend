package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.MessageMap;
import tw.com.hyweb.cathold.model.ResultPhase;

@RequiredArgsConstructor
public class MessageMapServiceImpl implements MessageMapService {

	private final R2dbcEntityOperations calVolTemplate;

	@Override
	public Mono<String> resultPhaseConvert(String type, ResultPhase resultPhase) {
		String s = resultPhase.getName();
		return this.calVolTemplate.selectOne(query(where("type").is(type).and("phase").is(s)), MessageMap.class)
				.switchIfEmpty(this.calVolTemplate
						.selectOne(query(where("type").is("BookingResult").and("phase").is(s)), MessageMap.class))
				.map(MessageMap::getCode).defaultIfEmpty(s);
	}

}
