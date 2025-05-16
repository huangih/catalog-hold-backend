package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import tw.com.hyweb.cathold.backend.redis.service.VParameterService;
import tw.com.hyweb.cathold.backend.redis.service.VUserCtrlStatusService;
import tw.com.hyweb.cathold.model.UserItemRuleCtrl;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.sqlserver.model.ReaderInfo;
import tw.com.hyweb.cathold.sqlserver.model.SqlserverCharged;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderInfoRepository;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverChargedRepository;

@RequiredArgsConstructor
@Slf4j
public class UserCheckServiceImpl implements UserCheckService {

	private static final String LENDCHECK_READERTYPES = "lendCheckService_readerTypeCodes_";

	private final VParameterService vParameterService;

	private final VUserCtrlStatusService vUserCtrlStatusService;

	private final ReaderInfoRepository readerInfoRepository;

	private final SqlserverChargedRepository sqlserverChargedRepository;

	private final R2dbcEntityOperations calVolTemplate;

	private final AmqpBackendClient amqpBackendClient;

	private Map<String, List<Integer>> lendReaderTypeIdMap;

	private Mono<Map<String, List<Integer>>> getLendReaderTypeIdMap() {
		return Mono.justOrEmpty(this.lendReaderTypeIdMap)
				.switchIfEmpty(this.vParameterService.getRulesByLikeRuleNames(LENDCHECK_READERTYPES)
						.flatMap(chr -> this.vParameterService.getTypeIdsByCatHoldRule(chr)
								.map(li -> Tuples.of(chr.getRuleClassName().replace(LENDCHECK_READERTYPES, ""), li)))
						.collectMap(Tuple2::getT1, Tuple2::getT2)
						.doOnNext(map -> log.info("newLendReaderTypeIdMap: {}", map)).map(map -> {
							this.lendReaderTypeIdMap = map;
							return this.lendReaderTypeIdMap;
						}));
	}

	@Override
	public Mono<VHoldItem> setDependUserRuleStatus(int readerId, VHoldItem vh) {
		return this.calVolTemplate
				.select(query(where("itslId").is(vh.getItslId()).and("statusId").is(vh.getStatusId())),
						UserItemRuleCtrl.class)
				.flatMap(uir -> this.vUserCtrlStatusService.processCheck(uir.getUserRule(), readerId).map(b -> {
					vh.setPropertyStatus(uir.getType(), b);
					return b;
				})).collectList().thenReturn(vh);
	}

	@Override
	public Mono<Boolean> checkReaderType(int readerId, String typeName) {
		return this.getLendReaderTypeIdMap().map(map -> map.get(typeName))
				.flatMap(li -> Mono.justOrEmpty(this.readerInfoRepository.findByReaderId(readerId))
						.map(ri -> li.contains(ri.getReaderTypeId())).defaultIfEmpty(false));
	}

	@Override
	public void correctNoFloatUserLendStatus() {
		this.getLendReaderTypeIdMap()
				.flatMapIterable(map -> this.readerInfoRepository.findByReaderTypeIdIn(map.get("noFloatLend"))
						.map(ReaderInfo::getReaderId).toList())
				.flatMapIterable(rId -> this.sqlserverChargedRepository.findByReaderId(rId).toList())
				.delayElements(Duration.ofMillis(200)).doOnNext(sc -> log.info("sqlserverCharged: {}", sc))
				.map(SqlserverCharged::getHoldId)
				.subscribe(holdId -> this.amqpBackendClient.setHoldItemTempStatus(holdId, 1));
	}
}
