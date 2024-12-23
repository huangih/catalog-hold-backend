package tw.com.hyweb.cathold.backend;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tw.com.hyweb.cathold.backend.controller.CatHoldManagerService;
import tw.com.hyweb.cathold.backend.redis.service.VCallVolHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemsService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchControlService;
import tw.com.hyweb.cathold.backend.redis.service.VUserCtrlStatusService;
import tw.com.hyweb.cathold.backend.rule.service.UserCtrlRuleService;
import tw.com.hyweb.cathold.backend.service.AmqpBackendClient;
import tw.com.hyweb.cathold.backend.service.BookingViewService;
import tw.com.hyweb.cathold.backend.service.LendLog2Service;
import tw.com.hyweb.cathold.backend.service.AmqpStreamService;
import tw.com.hyweb.cathold.backend.service.TouchClientService;
import tw.com.hyweb.cathold.model.ItemSiteDef;

@SpringBootApplication
@Slf4j
public class HylibCatalogHoldtestBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HylibCatalogHoldtestBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner test(R2dbcEntityOperations calVolTemplate, VTouchControlService vTouchControlService,
			MessageConverter messageConverter, BookingViewService bookingViewService,
			CatHoldManagerService catHoldManagerService, @Qualifier("streamExchange") DirectExchange streamExchange,
			TouchClientService touchClientService, UserCtrlRuleService userCtrlRuleService,
			AmqpBackendClient amqpBackendClient, VHoldClientService vHoldClientService, ObjectMapper om,
			LendLog2Service lendLog2Service, AmqpStreamService streamBackendService,
			VUserCtrlStatusService vUserCtrlStatusService, VHoldItemsService vHoldItemsService,
			VCallVolHoldSummaryService vCallVolHoldSummaryService) {
		return args -> {
//			catHoldManagerService.getBookingViewsForNcl(LocalDate.of(2023, 2, 10), LocalDate.of(2023, 2, 13))
//					.subscribe();
//			touchClientService.touchHoldItem("O027407", "241_Afrif234565TY", 500).subscribe(vhc -> log.info("{}", vhc));
			calVolTemplate.selectOne(query(where("siteCode").is("A11")), ItemSiteDef.class)
					.subscribe(site -> log.info("{}", site));
			vHoldClientService.getVHoldClientById(259).subscribe(vhc -> log.info("{}", vhc));
//			TouchControl tc = vHoldClientService.getHoldClientBySessionId("341_eD67au").log()
//					.flatMap(vhc -> vTouchControlService.newTouchControl("detu", vhc, 500))
//					.subscribeOn(Schedulers.parallel()).block();
//			vUserCtrlStatusService.processCheck(1, 154148).subscribe(b -> log.info("{}", b));
//			vUserCtrlStatusService.processCheck(2, 154148).subscribe(b -> log.info("{}", b));
//			vUserCtrlStatusService.processCheck(16, 154148).subscribe(b -> log.info("{}", b));
//			vUserCtrlStatusService.processCheck(1, 700044).subscribe(b -> log.info("{}", b));
//			vUserCtrlStatusService.processCheck(2, 700044).subscribe(b -> log.info("{}", b));
//			vUserCtrlStatusService.processCheck(16, 700044).subscribe(b -> log.info("{}", b));
			vHoldItemsService.findNonShadowHoldItemByCallVolId(1228210).collectList()
					.subscribe(vhis -> log.info("{}", vhis.size()));
			vCallVolHoldSummaryService.findCallVolHoldSummaryByCallVolId(1228210, 47599)
					.subscribe(cvhs -> log.info("{}", cvhs));
			vCallVolHoldSummaryService.findCallVolHoldSummaryByCallVolId(748992, 2212664)
					.subscribe(cvhs -> log.info("{}", cvhs));
			vCallVolHoldSummaryService.findCallVolHoldSummaryByCallVolId(1228210, 47599)
					.subscribe(cvhs -> log.info("{}", cvhs));
		};
	}
}
