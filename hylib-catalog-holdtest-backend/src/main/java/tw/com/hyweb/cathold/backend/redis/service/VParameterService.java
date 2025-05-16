package tw.com.hyweb.cathold.backend.redis.service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.CatalogHoldRule;
import tw.com.hyweb.cathold.model.ItemSiteDef;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.sqlserver.model.ReaderType;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderTypeRepository;

@Service
@RequiredArgsConstructor
public class VParameterService {

	private static final String REDIS_KEY = "_rediKey";

	private static final String RULECLASS_NAME = "ruleClassName";

	private final ReaderTypeRepository readerTypeRepository;

	private final ReactiveRedisUtils redisUtils;

	private final R2dbcEntityOperations calVolTemplate;

	public Mono<Integer> getNumberFromRuleName(String paramName) {
		return this
				.getParameters(paramName, Integer.class,
						code -> Mono.justOrEmpty(Integer.parseInt(code)).filter(n -> n > 0))
				.filter(list -> !list.isEmpty()).map(List::getFirst);
	}

	public Mono<List<String>> getStatusesFromRuleName(String paramName) {
		return this.getParameters(paramName, String.class, Mono::justOrEmpty);
	}

	public Mono<List<Integer>> getSiteIdsByRuleSiteCodes(String paramName) {
		return this.getParameters(paramName, Integer.class,
				code -> this.calVolTemplate
						.selectOne(query(where("siteCode").is(code)).columns("siteId"), ItemSiteDef.class)
						.map(ItemSiteDef::getSiteId));
	}

	public Mono<List<Phase>> getPhasesFromRuleName(String paramName) {
		return this.getParameters(paramName, Phase.class,
				val -> Flux.fromArray(Phase.values()).filter(p -> p.getName().equals(val)).next());
	}

	public Mono<List<Integer>> getTypeIdsByRuleName(String ruleName) {
		return this.getParameters(ruleName, Integer.class,
				typeCode -> Mono.justOrEmpty(this.readerTypeRepository.findByReaderTypeCode(typeCode))
						.map(ReaderType::getReaderTypeId));

	}

	public Mono<List<Integer>> getTypeIdsByCatHoldRule(CatalogHoldRule catalogHoldRule) {
		return this.getListParameters(catalogHoldRule.getRuleExp(),
				typeCode -> Mono.justOrEmpty(this.readerTypeRepository.findByReaderTypeCode(typeCode))
						.map(ReaderType::getReaderTypeId));
	}

	private <R> Mono<List<R>> getListParameters(String listString, Function<String, Mono<R>> function) {
		return Flux.fromArray(listString.split(",")).map(String::trim).flatMap(function).collectList();
	}

	public <R> Mono<List<R>> getParameters(String ruleName, Class<R> clazz, Function<String, Mono<R>> function) {
		String key = ruleName + REDIS_KEY;
		return this.redisUtils.getMonoListFromRedis(key, clazz, false, null)
				.switchIfEmpty(this.getParametersFromDb(ruleName).flatMap(function).collectList()
						.doOnNext(li -> this.redisUtils.redisLockCache(key, li, LocalDate.now())));
	}

	public Flux<String> getParametersFromDb(String ruleClassName) {
		return this.getRuleByRuleClassName(ruleClassName)
				.flatMapSequential(cr -> Flux.fromArray(cr.getRuleExp().split(",")));
	}

	public Flux<CatalogHoldRule> getRulesByLikeRuleNames(String ruleClassName) {
		return this.calVolTemplate.select(query(where(RULECLASS_NAME).like(ruleClassName + "%")),
				CatalogHoldRule.class);
	}

	public Flux<CatalogHoldRule> getRuleByRuleClassName(String ruleClassName) {
		return this.calVolTemplate.select(query(where(RULECLASS_NAME).is(ruleClassName)), CatalogHoldRule.class);
	}

}
