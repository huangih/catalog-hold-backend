package tw.com.hyweb.cathold.backend.service;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Criteria.where;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VSpecReaderidCachService;
import tw.com.hyweb.cathold.backend.redis.service.VUserCtrlStatusService;
import tw.com.hyweb.cathold.model.UserItemRuleCtrl;
import tw.com.hyweb.cathold.model.VHoldItem;

@RequiredArgsConstructor
public class UserCheckServiceImpl implements UserCheckService {

	private final VSpecReaderidCachService vSpecReaderidCachService;

	private final VUserCtrlStatusService vUserCtrlStatusService;

	private final R2dbcEntityOperations calVolTemplate;

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
	public Mono<Boolean> checkReaderType(int readerId, String ruleName) {
		return this.vSpecReaderidCachService.readerTypeCheck(readerId, ruleName);
	}

}
