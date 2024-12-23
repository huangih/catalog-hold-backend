package tw.com.hyweb.cathold.backend.configuration;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;

import com.rabbitmq.stream.Environment;

import tw.com.hyweb.cathold.backend.controller.CatHoldManagerService;
import tw.com.hyweb.cathold.backend.controller.CatHoldManagerServiceImpl;
import tw.com.hyweb.cathold.backend.controller.CatvolBookingService;
import tw.com.hyweb.cathold.backend.controller.CatvolBookingServiceImpl;
import tw.com.hyweb.cathold.backend.redis.service.ReactiveRedisUtils;
import tw.com.hyweb.cathold.backend.redis.service.VBookingService;
import tw.com.hyweb.cathold.backend.redis.service.VCallVolHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldClientService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemService;
import tw.com.hyweb.cathold.backend.redis.service.VHoldItemsService;
import tw.com.hyweb.cathold.backend.redis.service.VLendCallBackService;
import tw.com.hyweb.cathold.backend.redis.service.VMarcCallVolumeService;
import tw.com.hyweb.cathold.backend.redis.service.VSpecReaderidCachService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchControlService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchLogService;
import tw.com.hyweb.cathold.backend.redis.service.VUserCtrlStatusService;
import tw.com.hyweb.cathold.backend.service.AmqpBackendClient;
import tw.com.hyweb.cathold.backend.service.BookingCheckService;
import tw.com.hyweb.cathold.backend.service.BookingCheckServiceImpl;
import tw.com.hyweb.cathold.backend.service.BookingResultViewService;
import tw.com.hyweb.cathold.backend.service.BookingResultViewServiceImpl;
import tw.com.hyweb.cathold.backend.service.BookingViewService;
import tw.com.hyweb.cathold.backend.service.BookingViewServiceImpl;
import tw.com.hyweb.cathold.backend.service.ItemSiteDefService;
import tw.com.hyweb.cathold.backend.service.LendCheckService;
import tw.com.hyweb.cathold.backend.service.LendCheckServiceImpl;
import tw.com.hyweb.cathold.backend.service.LendLog2Service;
import tw.com.hyweb.cathold.backend.service.MessageMapService;
import tw.com.hyweb.cathold.backend.service.MessageMapServiceImpl;
import tw.com.hyweb.cathold.backend.service.AmqpStreamService;
import tw.com.hyweb.cathold.backend.service.AmqpStreamServiceImpl;
import tw.com.hyweb.cathold.backend.service.TouchClientService;
import tw.com.hyweb.cathold.backend.service.TouchClientServiceImpl;
import tw.com.hyweb.cathold.backend.service.TransitOverdaysService;
import tw.com.hyweb.cathold.backend.service.TransitOverdaysServiceImpl;
import tw.com.hyweb.cathold.backend.service.UserCheckService;
import tw.com.hyweb.cathold.backend.service.UserCheckServiceImpl;
import tw.com.hyweb.cathold.backend.service.UserStopBookingService;
import tw.com.hyweb.cathold.backend.service.UserStopBookingServiceImpl;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverChargedRepository;

@Configuration
public class ServiceBeanConfiguration {

	@Bean
	AmqpStreamService amqpStreamService(MessageConverter messageConverter, Environment environment) {
		return new AmqpStreamServiceImpl(messageConverter, environment);
	}

	@Bean
	CatvolBookingService catvolBookingService(VHoldClientService vHoldClientService,
			TouchClientService touchClientService) {
		return new CatvolBookingServiceImpl(vHoldClientService, touchClientService);
	}

	@Bean
	CatHoldManagerService catHoldManagerService(BookingViewService bookingViewService,
			AmqpStreamService streamBackendService, AmqpBackendClient amqpBackendClient,
			ReactiveRedisUtils redisUtils) {
		return new CatHoldManagerServiceImpl(bookingViewService, streamBackendService, amqpBackendClient, redisUtils);
	}

	@Bean
	TouchClientService touchClientService(VTouchControlService vTouchControlService,
			AmqpBackendClient amqpBackendClient, VHoldClientService vHoldClientService,
			VTouchLogService vTouchLogService) {
		return new TouchClientServiceImpl(vTouchControlService, amqpBackendClient, vHoldClientService,
				vTouchLogService);
	}

	@Bean
	BookingViewService bookingViewService(VBookingService vBookingService, VHoldItemService vHoldItemService,
			VHoldItemsService vHoldItemsService, VMarcCallVolumeService vMarcCallVolumeService,
			ItemSiteDefService itemSiteDefService, R2dbcEntityOperations calVolTemplate) {
		return new BookingViewServiceImpl(vBookingService, vHoldItemService, vHoldItemsService, vMarcCallVolumeService,
				itemSiteDefService, calVolTemplate);
	}

	@Bean
	UserStopBookingService userStopBookingService(BookingViewService bookingViewService,
			R2dbcEntityOperations calVolTemplate) {
		return new UserStopBookingServiceImpl(bookingViewService, calVolTemplate);
	}

	@Bean
	MessageMapService messageMapService(R2dbcEntityOperations calVolTemplate) {
		return new MessageMapServiceImpl(calVolTemplate);
	}

	@Bean
	BookingResultViewService bookingResultViewService(UserStopBookingService userStopBookingService,
			MessageMapService messageMapService) {
		return new BookingResultViewServiceImpl(userStopBookingService, messageMapService);
	}

	@Bean
	BookingCheckService bookingCheckService(R2dbcEntityOperations calVolTemplate) {
		return new BookingCheckServiceImpl(calVolTemplate);
	}

	@Bean
	UserCheckService userCheckService(VSpecReaderidCachService vSpecReaderidCachService,
			VUserCtrlStatusService vUserCtrlStatusService, R2dbcEntityOperations calVolTemplate) {
		return new UserCheckServiceImpl(vSpecReaderidCachService, vUserCtrlStatusService, calVolTemplate);
	}

	@Bean
	TransitOverdaysService transitOverdaysService(R2dbcEntityOperations calVolTemplate) {
		return new TransitOverdaysServiceImpl(calVolTemplate);
	}

	@Bean
	LendCheckService lendCheckService(BookingCheckService bookingCheckService, UserCheckService userCheckService,
			TransitOverdaysService transitOverdaysService, LendLog2Service lendLog2Service,
			MessageMapService messageMapService, SqlserverChargedRepository sqlserverChargedRepository,
			VHoldItemService vHoldItemService, VCallVolHoldSummaryService vCallVolHoldSummaryService,
			VLendCallBackService vLendCallBackService, AmqpBackendClient amqpBackendClient) {
		return new LendCheckServiceImpl(bookingCheckService, userCheckService, transitOverdaysService, lendLog2Service,
				messageMapService, sqlserverChargedRepository, vHoldItemService, vCallVolHoldSummaryService,
				vLendCallBackService, amqpBackendClient);
	}

}
