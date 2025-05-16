package tw.com.hyweb.cathold.backend.configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.r2dbc.core.DatabaseClient;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import tw.com.hyweb.cathold.model.AppendixStatusReadConverter;
import tw.com.hyweb.cathold.model.AppendixStatusWriteConverter;
import tw.com.hyweb.cathold.model.PhaseReadConverter;
import tw.com.hyweb.cathold.model.PhaseWriteConverter;
import tw.com.hyweb.cathold.model.ResultPhaseReadConverter;
import tw.com.hyweb.cathold.model.ResultPhaseWriteConverter;
import tw.com.hyweb.cathold.model.RulePickupReadConverter;
import tw.com.hyweb.cathold.model.RuleStatusReadConverter;
import tw.com.hyweb.cathold.model.RuleStatusWriteConverter;
import tw.com.hyweb.cathold.model.SuspendPhaseReadConverter;
import tw.com.hyweb.cathold.model.SuspendPhaseWriteConverter;

@Configuration
public class MariadbR2dbcDataSourceConfiguration {

	@Bean
	ConnectionPool mariadbConnectionPool() {
		Map<String, String> map = new HashMap<>();
		map.put("characterencoding", "utf8");
		MariadbConnectionConfiguration conf = MariadbConnectionConfiguration.builder().host("tml-230.tpml.edu.tw")
				.database("cal_vol_test").username("hyweb").password("1qaz@WSX3edc").useServerPrepStmts(true)
				.prepareCacheSize(512).connectionAttributes(map).build();
		ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(new MariadbConnectionFactory(conf))
				.initialSize(1).validationQuery("select 1").maxSize(10).name("cal_vol-pool")
				.maxValidationTime(Duration.ofMillis(3000)).build();
		return new ConnectionPool(poolConfig);
	}

	@Bean
	@Primary
	R2dbcEntityOperations calVolR2dbcEntityTemplate() {
		List<Converter<?, ?>> converters = Arrays.asList(new AppendixStatusReadConverter(),
				new AppendixStatusWriteConverter(), new PhaseReadConverter(), new PhaseWriteConverter(),
				new ResultPhaseReadConverter(), new ResultPhaseWriteConverter(), new RulePickupReadConverter(),
				new RuleStatusReadConverter(), new RuleStatusWriteConverter(), new SuspendPhaseReadConverter(),
				new SuspendPhaseWriteConverter());
		R2dbcCustomConversions r2dbcCustomConversions = R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
		R2dbcMappingContext context = new R2dbcMappingContext(DefaultNamingStrategy.INSTANCE);
		R2dbcConverter r2dbcConverter = new MappingR2dbcConverter(context, r2dbcCustomConversions);
		DatabaseClient databaseClient = DatabaseClient.create(mariadbConnectionPool());
		return new R2dbcEntityTemplate(databaseClient, MySqlDialect.INSTANCE, r2dbcConverter);
	}

}
