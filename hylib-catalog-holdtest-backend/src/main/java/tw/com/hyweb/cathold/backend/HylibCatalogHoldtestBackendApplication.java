package tw.com.hyweb.cathold.backend;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;

import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchControlService;
import tw.com.hyweb.cathold.model.ItemSiteDef;
import tw.com.hyweb.cathold.model.client.TouchControl;

@SpringBootApplication
@Slf4j
public class HylibCatalogHoldtestBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HylibCatalogHoldtestBackendApplication.class, args);
	}

//	@Bean
	CommandLineRunner test(R2dbcEntityOperations calVolTemplate, VTouchControlService vTouchControlService,
			VHoldClientService vHoldClientService) {
		return args -> {
			calVolTemplate.selectOne(query(where("siteCode").is("A11")), ItemSiteDef.class).log()
					.subscribe(site -> log.info("{}", site));
			TouchControl tc = vHoldClientService.getHoldClientBySessionId("341_eD67au").log()
					.flatMap(vhc -> vTouchControlService.newTouchControl("detu", vhc, 500))
					.subscribeOn(Schedulers.parallel()).block();
		};
	}
}
