package com.generac.ces.systemgateway.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.common.client.WebClientFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "web-client")
public class WebClientConfig {

    @Autowired ObjectMapper objectMapper;

    private String simulatorUrl;
    private String odinUrl;
    private String systemUrl;
    private String systemV2Url;
    private String siteUrl;
    private String miSystemUrl;
    private String essDpUrl;
    private String firmwareManagementUrl;

    @Bean
    public WebClient systemServiceWebClient() {
        return WebClientFactory.newRestClient(systemUrl);
    }

    @Bean
    public WebClient systemServiceV2WebClient() {
        return WebClientFactory.newRestClient(systemV2Url);
    }

    @Bean
    public WebClient siteServiceWebClient() {
        return WebClientFactory.newRestClient(siteUrl);
    }

    @Bean
    public WebClient miSystemServiceWebClient() {
        return WebClientFactory.newRestClient(miSystemUrl);
    }

    @Bean
    public WebClient odinServiceWebClient() {
        return WebClientFactory.newRestClient(odinUrl);
    }

    @Bean
    public WebClient essDataProviderWebClient() {
        return WebClientFactory.newRestClient(essDpUrl);
    }

    @Bean
    public WebClient deviceSimulatorWebClient() {
        return WebClientFactory.newRestClient(simulatorUrl);
    }

    @Bean
    public WebClient firmwareManagementWebClient() {
        return WebClientFactory.withStandardFilters(WebClient.builder())
                .clientConnector(new ReactorClientHttpConnector())
                .baseUrl(firmwareManagementUrl)
                // There is more boilerplate for this client than typical in our applications.  See
                // javadoc on exchangeStrategies() for details about why.
                .exchangeStrategies(exchangeStrategies())
                .build();
    }

    /**
     * Creates custom exchanges strategies required for a WebClient to use the common ObjectMapper
     * (from generac-maven-parent). For some reason, this application's WebClient instances do not
     * use the common ObjectMapper by default for JSON request/response (de)serialization. This is
     * different from other applications and may have to do with this application's use of Netty
     * instead of Jetty, or a related autoconfiguration done by Spring based on this application's
     * dependencies.
     */
    private ExchangeStrategies exchangeStrategies() {
        var encoder = new Jackson2JsonEncoder(objectMapper);
        var decoder = new Jackson2JsonDecoder(objectMapper);
        return ExchangeStrategies.builder()
                .codecs(
                        configurer -> {
                            configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                            configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                        })
                .build();
    }
}
