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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@EnableJpaRepositories(
        entityManagerFactoryRef = "clickhouseEM",
        basePackages = {
            "com.generac.ces.systemgateway.repository.device",
            "com.generac.ces.systemgateway.repository.rgm"
        })
@Configuration(proxyBeanMethods = false)
public class ClickhouseDatasourceConfiguration {
    @Bean(name = "chDbProps")
    @ConfigurationProperties("spring.datasource.clickhouse")
    public DataSourceProperties clickhouseDbProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "chDataSource")
    @ConfigurationProperties("spring.datasource.clickhouse.hikari")
    public HikariDataSource clickhouseDataSource(
            @Qualifier("chDbProps") DataSourceProperties clickhouseDbProperties) {
        return clickhouseDbProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "clickhouseEM")
    public LocalContainerEntityManagerFactoryBean clickhouseEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("chDataSource") DataSource clickhouseDataSource) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put(
                "hibernate.dialect", "com.generac.ces.systemgateway.configuration.EmptyDialect");
        return builder.dataSource(clickhouseDataSource)
                .properties(properties)
                .packages(
                        "com.generac.ces.systemgateway.entity.device",
                        "com.generac.ces.systemgateway.entity.rgm")
                .persistenceUnit("device")
                .build();
    }
}
