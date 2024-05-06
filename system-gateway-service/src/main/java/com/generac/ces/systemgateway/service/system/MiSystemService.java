package com.generac.ces.systemgateway.service.system;

import com.generac.ces.systemgateway.exception.*;
import com.generac.ces.systemgateway.model.SystemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MiSystemService {

    private final WebClient miSystemServiceWebClient;

    @Autowired
    public MiSystemService(WebClient miSystemServiceWebClient) {
        this.miSystemServiceWebClient = miSystemServiceWebClient;
    }

    public SystemResponse getSystemBySystemId(String systemId) {
        return getSystemBySystemIdMono(systemId).block();
    }

    private Mono<SystemResponse> getSystemBySystemIdMono(String systemId) {
        return miSystemServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemId).build())
                .retrieve()
                .onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                throw new ResourceNotFoundException(
                                        "PWRMicro system id = " + systemId + " not found.");
                            }
                            return null;
                        })
                .bodyToMono(new ParameterizedTypeReference<SystemResponse>() {});
    }
}
