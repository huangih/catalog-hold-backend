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
import tw.com.hyweb.cathold.backend.redis.service.VMarcHoldSummaryService;
import tw.com.hyweb.cathold.backend.redis.service.VParameterService;
import tw.com.hyweb.cathold.backend.redis.service.VTouchLogService;
import tw.com.hyweb.cathold.backend.redis.service.VUserCtrlStatusService;
import tw.com.hyweb.cathold.backend.service.AmqpBackendClient;
import tw.com.hyweb.cathold.backend.service.BookingCheckService;
import tw.com.hyweb.cathold.backend.service.BookingCheckServiceImpl;
import tw.com.hyweb.cathold.backend.service.BookingExpandDuedateService;
import tw.com.hyweb.cathold.backend.service.BookingExpandDuedateServiceImpl;
import tw.com.hyweb.cathold.backend.service.BookingResultViewService;
import tw.com.hyweb.cathold.backend.service.BookingResultViewServiceImpl;
import tw.com.hyweb.cathold.backend.service.BookingStatusViewService;
import tw.com.hyweb.cathold.backend.service.BookingStatusViewServiceImpl;
import tw.com.hyweb.cathold.backend.service.BookingViewService;
import tw.com.hyweb.cathold.backend.service.BookingViewServiceImpl;
import tw.com.hyweb.cathold.backend.service.ClyTransitService;
import tw.com.hyweb.cathold.backend.service.ClyTransitServiceImpl;
import tw.com.hyweb.cathold.backend.service.ItemSiteDefService;
import tw.com.hyweb.cathold.backend.service.LendCheckService;
import tw.com.hyweb.cathold.backend.service.LendCheckServiceImpl;
import tw.com.hyweb.cathold.backend.service.LendLog2Service;
import tw.com.hyweb.cathold.backend.service.MessageMapService;
import tw.com.hyweb.cathold.backend.service.MessageMapServiceImpl;
import tw.com.hyweb.cathold.backend.service.AmqpStreamService;
import tw.com.hyweb.cathold.backend.service.AmqpStreamServiceImpl;
import tw.com.hyweb.cathold.backend.service.TouchService;
import tw.com.hyweb.cathold.backend.service.TouchServiceImpl;
import tw.com.hyweb.cathold.backend.service.TransitOverdaysService;
import tw.com.hyweb.cathold.backend.service.TransitOverdaysServiceImpl;
import tw.com.hyweb.cathold.backend.service.UserCheckService;
import tw.com.hyweb.cathold.backend.service.UserCheckServiceImpl;
import tw.com.hyweb.cathold.backend.service.UserStopBookingService;
import tw.com.hyweb.cathold.backend.service.UserStopBookingServiceImpl;
import tw.com.hyweb.cathold.backend.service.UserSuspendBookingService;
import tw.com.hyweb.cathold.backend.service.UserSuspendBookingServiceImpl;
import tw.com.hyweb.cathold.sqlserver.repository.MarcDetailRepository;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderInfoRepository;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverChargedRepository;
import tw.com.hyweb.cathold.sqlserver.repository.SqlserverHoldStatusRepository;

@Configuration
public class ServiceBeanConfiguration {

	@Bean
	AmqpStreamService amqpStreamService(MessageConverter messageConverter, Environment environment) {
		return new AmqpStreamServiceImpl(messageConverter, environment);
	}

	@Bean
	CatvolBookingService catvolBookingService(BookingViewService bookingViewService,
			BookingResultViewService bookingResultViewService, BookingExpandDuedateService bookingExpandDuedateService,
			BookingStatusViewService bookingStatusViewService, ItemSiteDefService itemSiteDefService,
			UserStopBookingService userStopBookingService, UserSuspendBookingService userSuspendBookingService,
			LendCheckService lendCheckService, VMarcHoldSummaryService vMarcHoldSummaryService,
			VCallVolHoldSummaryService vCallVolHoldSummaryService, VHoldItemService vHoldItemService,
			VHoldClientService vHoldClientService, TouchService touchService, R2dbcEntityOperations calVolTemplate,
			AmqpBackendClient amqpBackendClient) {
		return new CatvolBookingServiceImpl(bookingViewService, bookingResultViewService, bookingExpandDuedateService,
				bookingStatusViewService, itemSiteDefService, userStopBookingService, userSuspendBookingService,
				lendCheckService, vMarcHoldSummaryService, vCallVolHoldSummaryService, vHoldItemService,
				vHoldClientService, touchService, calVolTemplate, amqpBackendClient);
	}

	@Bean
	CatHoldManagerService catHoldManagerService(BookingViewService bookingViewService,
			ClyTransitService clyTransitService, ItemSiteDefService itemSiteDefService,
			AmqpStreamService streamBackendService, AmqpBackendClient amqpBackendClient,
			ReactiveRedisUtils redisUtils) {
		return new CatHoldManagerServiceImpl(bookingViewService, clyTransitService, itemSiteDefService,
				streamBackendService, amqpBackendClient, redisUtils);
	}

	@Bean
	BookingViewService bookingViewService(BookingExpandDuedateService bookingExpandDuedateService,
			VBookingService vBookingService, VHoldItemService vHoldItemService, VHoldItemsService vHoldItemsService,
			VMarcCallVolumeService vMarcCallVolumeService, ItemSiteDefService itemSiteDefService,
			R2dbcEntityOperations calVolTemplate) {
		return new BookingViewServiceImpl(bookingExpandDuedateService, vBookingService, vHoldItemService,
				vHoldItemsService, vMarcCallVolumeService, itemSiteDefService, calVolTemplate);
	}

	@Bean
	BookingStatusViewService bookingStatusViewService(VMarcCallVolumeService vMarcCallVolumeService,
			R2dbcEntityOperations calVolTemplate) {
		return new BookingStatusViewServiceImpl(vMarcCallVolumeService, calVolTemplate);
	}

	@Bean
	UserStopBookingService userStopBookingService(BookingViewService bookingViewService,
			R2dbcEntityOperations calVolTemplate) {
		return new UserStopBookingServiceImpl(bookingViewService, calVolTemplate);
	}

	@Bean
	UserSuspendBookingService userSuspendBookingService(R2dbcEntityOperations calVolTemplate) {
		return new UserSuspendBookingServiceImpl(calVolTemplate);
	}

	@Bean
	MessageMapService messageMapService(R2dbcEntityOperations calVolTemplate) {
		return new MessageMapServiceImpl(calVolTemplate);
	}

	@Bean
	BookingResultViewService bookingResultViewService(BookingViewService bookingViewService,
			UserStopBookingService userStopBookingService, MessageMapService messageMapService) {
		return new BookingResultViewServiceImpl(bookingViewService, userStopBookingService, messageMapService);
	}

	@Bean
	BookingExpandDuedateService bookingExpandDuedateService(ReactiveRedisUtils redisUtils,
			R2dbcEntityOperations calVolTemplate) {
		return new BookingExpandDuedateServiceImpl(redisUtils, calVolTemplate);
	}

	@Bean
	BookingCheckService bookingCheckService(R2dbcEntityOperations calVolTemplate) {
		return new BookingCheckServiceImpl(calVolTemplate);
	}

	@Bean
	UserCheckService userCheckService(VParameterService vParameterService,
			VUserCtrlStatusService vUserCtrlStatusService, ReaderInfoRepository readerInfoRepository,
			SqlserverChargedRepository sqlserverChargedRepository, R2dbcEntityOperations calVolTemplate,
			AmqpBackendClient amqpBackendClient) {
		return new UserCheckServiceImpl(vParameterService, vUserCtrlStatusService, readerInfoRepository,
				sqlserverChargedRepository, calVolTemplate, amqpBackendClient);
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

	@Bean
	ClyTransitService clyTransitService(VHoldItemService vHoldItemService, MarcDetailRepository marcDetailRepository,
			R2dbcEntityOperations calVolTemplate) {
		return new ClyTransitServiceImpl(vHoldItemService, marcDetailRepository, calVolTemplate);
	}

	@Bean
	TouchService touchService(VHoldClientService vHoldClientService, VTouchLogService vTouchLogService,
			VHoldItemService vHoldItemService, ItemSiteDefService itemSiteDefService,
			SqlserverHoldStatusRepository sqlserverHoldStatusRepository, AmqpBackendClient amqpBackendClient,
			R2dbcEntityOperations calVolTemplate, ReactiveRedisUtils redisUtils) {
		return new TouchServiceImpl(vHoldClientService, vTouchLogService, vHoldItemService, itemSiteDefService,
				sqlserverHoldStatusRepository, amqpBackendClient, calVolTemplate, redisUtils);
	}

}
