package com.generac.ces.systemgateway.service;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DeviceSimulatorService {

    private final WebClient deviceSimulatorWebClient;

    @Autowired
    public DeviceSimulatorService(WebClient deviceSimulatorWebClient) {
        this.deviceSimulatorWebClient = deviceSimulatorWebClient;
    }

    public void startStreamingForSystemId(String systemId) {
        startStreamingForSystemIdMono(systemId).block();
    }

    private Mono<Void> startStreamingForSystemIdMono(String systemId) {
        return deviceSimulatorWebClient
                .post()
                .uri("/v1/subscribe/live")
                .body(Mono.just(new SimulatorStreamRequest(systemId)), SimulatorStreamRequest.class)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimulatorStreamRequest {
        private String systemId;
    }
}
