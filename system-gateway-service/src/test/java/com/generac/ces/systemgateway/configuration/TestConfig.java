package com.generac.ces.systemgateway.configuration;

import com.generac.ces.systemgateway.model.ParameterTimestampMap;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean(name = "remoteSettingsCache")
    public CacheStore<ParameterTimestampMap> remoteSettingsCache() {
        return new CacheStore<ParameterTimestampMap>(10000, 2, TimeUnit.SECONDS);
    }
}
