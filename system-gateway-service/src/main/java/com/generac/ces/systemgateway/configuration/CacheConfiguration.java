package com.generac.ces.systemgateway.configuration;

import com.generac.ces.systemgateway.model.ParameterTimestampMap;
import com.generac.ces.systemgateway.model.SiteResponse;
import com.generac.ces.systemgateway.model.SystemResponse;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheConfiguration {

    private CacheProps essSystemMsCache;
    private CacheProps remoteSettingsCache;
    private CacheProps systemMsV2Cache;
    private CacheProps siteMsCache;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CacheProps {
        private int maxElements;
        private int ttlSeconds;
    }

    @Bean(name = "essSystemMsCache")
    public CacheStore<SystemResponse> essSystemMsCache() {
        return new CacheStore<SystemResponse>(
                essSystemMsCache.getMaxElements(),
                essSystemMsCache.getTtlSeconds(),
                TimeUnit.SECONDS);
    }

    @Bean(name = "systemMsV2Cache")
    public CacheStore<SystemResponse> systemMsV2Cache() {
        return new CacheStore<SystemResponse>(
                systemMsV2Cache.getMaxElements(),
                systemMsV2Cache.getTtlSeconds(),
                TimeUnit.SECONDS);
    }

    @Bean(name = "siteMsCache")
    public CacheStore<SiteResponse> siteMsCache() {
        return new CacheStore<SiteResponse>(
                siteMsCache.getMaxElements(), siteMsCache.getTtlSeconds(), TimeUnit.SECONDS);
    }

    @Bean(name = "remoteSettingsCache")
    public CacheStore<ParameterTimestampMap> remoteSettingsCache() {
        return new CacheStore<ParameterTimestampMap>(
                remoteSettingsCache.getMaxElements(),
                remoteSettingsCache.getTtlSeconds(),
                TimeUnit.SECONDS);
    }
}
