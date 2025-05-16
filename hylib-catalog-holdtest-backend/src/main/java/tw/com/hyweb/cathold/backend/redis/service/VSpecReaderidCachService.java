package tw.com.hyweb.cathold.backend.redis.service;

import java.util.List;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.SpecReaderidCache;
import tw.com.hyweb.cathold.sqlserver.model.ReaderType;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderTypeRepository;

@Service
@RequiredArgsConstructor
public class VSpecReaderidCachService {

	private static final String LENDCHECK_READERTYPES = "lendCheckService_readerTypeCodes_";

	private final ReaderTypeRepository readerTypeRepository;

	private final R2dbcEntityOperations calVolTemplate;

	private final VParameterService vParameterService;

	public Mono<Boolean> readerTypeCheck(int readerId, String key) {
		String ruleName = LENDCHECK_READERTYPES + key;
		return this.calVolTemplate.selectOne(query(where("readerId").is(readerId)), SpecReaderidCache.class)
				.map(SpecReaderidCache::getReaderType)
				.zipWith(this.getTypeIdsByRuleName(ruleName), (type, li) -> li.contains(type)).defaultIfEmpty(false);
	}

	private Mono<List<Integer>> getTypeIdsByRuleName(String ruleName) {
		return this.vParameterService.getParameters(ruleName, Integer.class,
				typeCode -> Mono.justOrEmpty(this.readerTypeRepository.findByReaderTypeCode(typeCode))
						.map(ReaderType::getReaderTypeId));
	}

}
