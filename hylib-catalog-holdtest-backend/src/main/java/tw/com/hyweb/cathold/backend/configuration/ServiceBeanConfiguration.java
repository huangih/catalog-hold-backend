package tw.com.hyweb.cathold.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tw.com.hyweb.cathold.backend.controller.CatvolBookingService;
import tw.com.hyweb.cathold.backend.controller.CatvolBookingServiceImpl;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.service.TouchClientService;
import tw.com.hyweb.cathold.backend.service.TouchClientServiceImpl;

@Configuration
public class ServiceBeanConfiguration {

	@Bean
	CatvolBookingService catvolBookingService(VHoldClientService vHoldClientService,
			TouchClientService touchClientService) {
		return new CatvolBookingServiceImpl(vHoldClientService, touchClientService);
	}

	@Bean
	TouchClientService touchClientService() {
		return new TouchClientServiceImpl();
	}
}
