package tw.com.hyweb.cathold.backend.configuration;

import java.text.SimpleDateFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:redisson-config.yaml")
public class ReactiveRedisConfiguration {

	private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		om.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT));
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		om.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL,
				As.WRAPPER_ARRAY);
		return om;
	}

}
