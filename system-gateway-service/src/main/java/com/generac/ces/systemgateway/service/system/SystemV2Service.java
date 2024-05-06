package com.generac.ces.systemgateway.service.system;

import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.model.SystemResponse;
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
public class SystemV2Service {

    private final WebClient systemServiceV2WebClient;
    private final CacheStore<SystemResponse> cache;

    @Autowired
    public SystemV2Service(
            WebClient systemServiceV2WebClient,
            @Qualifier("systemMsV2Cache") CacheStore<SystemResponse> systemMsV2Cache) {
        this.systemServiceV2WebClient = systemServiceV2WebClient;
        this.cache = systemMsV2Cache;
    }

    public SystemResponse getSystemBySystemId(UUID systemId) {
        try {
            return cache.get(systemId.toString(), () -> getSystemBySystemIdMono(systemId).block());
        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Execution exception on cache read for system id = " + systemId + ".", e);
        }
    }

    private Mono<SystemResponse> getSystemBySystemIdMono(UUID systemId) {
        return systemServiceV2WebClient
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemId.toString()).build())
                .exchangeToMono(
                        clientResponse -> {
                            if (!clientResponse
                                    .statusCode()
                                    .is2xxSuccessful()) { // handle all unsuccessful statuses here
                                if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                    throw new ResourceNotFoundException(
                                            "Penguin system id = " + systemId + " not found.");
                                } else {
                                    throw new InternalServerException(
                                            "Error occurred on Penguin metadata retrieval for"
                                                    + " system id = "
                                                    + systemId
                                                    + ".");
                                }
                            }
                            return clientResponse.bodyToMono(
                                    new ParameterizedTypeReference<SystemResponse>() {});
                        })
                .timeout(Duration.ofSeconds(5));
    }
}
