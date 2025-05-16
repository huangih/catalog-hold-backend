package tw.com.hyweb.cathold.backend.configuration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class ReactiveRedisConfiguration {

	private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@Bean
	@Primary
	ObjectMapper mapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		om.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT));
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		om.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//		om.activateDefaultTyping(om.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);
		return om;
	}

	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		om.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT));
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		om.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.activateDefaultTyping(om.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.WRAPPER_ARRAY);
		return om;
	}

	@Bean
	RedissonClient redissonClient(@Qualifier("objectMapper") ObjectMapper objectMapper) throws IOException {
		File file = ResourceUtils.getFile("classpath:redisson-config.yaml");
		Config config = Config.fromYAML(file);
		config.setCodec(new JsonJacksonCodec(objectMapper));
		return Redisson.create(config);
	}

}
