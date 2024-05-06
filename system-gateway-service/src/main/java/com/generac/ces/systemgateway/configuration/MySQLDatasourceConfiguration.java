package com.generac.ces.systemgateway.configuration;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@EnableJpaRepositories(
        entityManagerFactoryRef = "systemGatewayEM",
        basePackages = {"com.generac.ces.systemgateway.repository.subscription"})
@Configuration(proxyBeanMethods = false)
public class MySQLDatasourceConfiguration {
    @Bean(name = "sgDbProps")
    @Primary
    @ConfigurationProperties("spring.datasource.mysql")
    public DataSourceProperties systemGatewayDbProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "sgDataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.mysql.hikari")
    public HikariDataSource systemGatewayDataSource(
            @Qualifier("sgDbProps") DataSourceProperties systemGatewayDbProperties) {
        return systemGatewayDbProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "systemGatewayEM")
    @Primary
    public LocalContainerEntityManagerFactoryBean systemGatewayEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("sgDataSource") DataSource systemGatewayDataSource) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLInnoDBDialect");
        return builder.dataSource(systemGatewayDataSource)
                .properties(properties)
                .packages("com.generac.ces.systemgateway.entity.subscription")
                .persistenceUnit("subscription")
                .build();
    }
}
