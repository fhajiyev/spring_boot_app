package com.generac.ces.systemgateway.service.system;

import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.model.SiteResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class SiteService {

    private final WebClient siteServiceWebClient;
    private final CacheStore<SiteResponse> cache;

    @Autowired
    public SiteService(
            WebClient siteServiceWebClient,
            @Qualifier("siteMsCache") CacheStore<SiteResponse> siteMsCache) {
        this.siteServiceWebClient = siteServiceWebClient;
        this.cache = siteMsCache;
    }

    public SiteResponse getSiteBySystemId(UUID systemId) {
        try {
            return cache.get(systemId.toString(), () -> getSiteBySystemIdMono(systemId).block());
        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Execution exception on cache read for system id = " + systemId + ".", e);
        }
    }

    private Mono<SiteResponse> getSiteBySystemIdMono(UUID systemId) {
        return siteServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemId.toString(), "metadata").build())
                .exchangeToMono(
                        clientResponse -> {
                            if (!clientResponse
                                    .statusCode()
                                    .is2xxSuccessful()) { // handle all unsuccessful statuses here
                                if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                    throw new ResourceNotFoundException(
                                            "Penguin site for system id = "
                                                    + systemId
                                                    + " not found.");
                                } else {
                                    throw new InternalServerException(
                                            "Error occurred on Penguin site metadata retrieval for"
                                                    + " system id = "
                                                    + systemId
                                                    + ".");
                                }
                            }
                            return clientResponse.bodyToMono(
                                    new ParameterizedTypeReference<SiteResponse>() {});
                        })
                .timeout(Duration.ofSeconds(5));
    }
}
