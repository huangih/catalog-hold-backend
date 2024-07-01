package tw.com.hyweb.cathold.backend.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.NoArgsConstructor;

@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:multipledb-datasource.yaml")
@EnableJpaRepositories(basePackages = "tw.com.hyweb.cathold.sqlserver.repository", entityManagerFactoryRef = "sqlserverEntityManager", transactionManagerRef = "sqlserverTransactionManager")
@NoArgsConstructor
public class SqlserverDataSourceConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "spring.sqlserver-datasource")
	DataSourceProperties sqlserverDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@Primary
	@ConfigurationProperties(prefix = "spring.sqlserver-datasource.hikari")
	DataSource sqlserverDataSource() {
		return sqlserverDataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Bean
	LocalContainerEntityManagerFactoryBean sqlserverEntityManager(
			@Qualifier("sqlserverDataSource") DataSource dataSource, EntityManagerFactoryBuilder builder) {
		LocalContainerEntityManagerFactoryBean emf = builder.dataSource(dataSource)
				.packages("tw.com.hyweb.cathold.sqlserver.model").persistenceUnit("sqlserver_unit").build();
		final Map<String, Object> properties = new HashMap<>();
		properties.put("hibernate.physical_naming_strategy",
				"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
		emf.setJpaPropertyMap(properties);
		return emf;
	}

	@Bean
	PlatformTransactionManager sqlserverTransactionManager(
			@Qualifier("sqlserverEntityManager") LocalContainerEntityManagerFactoryBean emf) {
		return new JpaTransactionManager(Objects.requireNonNull(emf.getObject()));
	}

}
