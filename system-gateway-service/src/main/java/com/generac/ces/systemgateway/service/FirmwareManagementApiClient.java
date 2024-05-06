package com.generac.ces.systemgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.bluemarlin.firmwaremanagement.dto.FirmwareUpdateForSystemHttpRequest;
import com.generac.ces.bluemarlin.firmwaremanagement.dto.FirmwareUpdateForSystemHttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FirmwareManagementApiClient {

    @Autowired ObjectMapper objectMapper;

    private static final Duration THIRTY_SECONDS = Duration.ofSeconds(30);
    private final WebClient firmwareManagementWebClient;

    public FirmwareManagementApiClient(WebClient firmwareManagementWebClient) {
        this.firmwareManagementWebClient = firmwareManagementWebClient;
    }

    // TODO: Use an actual entity from Firmware Management service.
    public List<String> getAvailableFirmwareUpdateVersions() {
        // TODO: Confirm why this does not require a system ID
        var availableVersions =
                firmwareManagementWebClient
                        .get()
                        .uri("/available-updates")
                        .retrieve()
                        .bodyToMono(FutureFirmwareManagementModel.class)
                        .block(THIRTY_SECONDS);

        return List.of("0.4.3", "0.9.5");
    }

    public FirmwareUpdateForSystemHttpResponse updateSystemFirmwareVersion(
            UUID systemId,
            String updateVersion,
            String notes,
            UUID initiatingUserId,
            String initiatingApplicationName) {

        var requestBody =
                new FirmwareUpdateForSystemHttpRequest(
                        updateVersion,
                        initiatingUserId.toString(),
                        initiatingApplicationName,
                        null,
                        notes);

        return firmwareManagementWebClient
                .post()
                .uri("/v1/updates/systems/" + systemId)
                .body(Mono.just(requestBody), FirmwareUpdateForSystemHttpRequest.class)
                .exchangeToMono(
                        clientResponse -> {
                            if (clientResponse.statusCode().is4xxClientError()) {
                                return clientResponse.createException().flatMap(Mono::error);
                            }

                            return clientResponse.bodyToMono(
                                    FirmwareUpdateForSystemHttpResponse.class);
                        })
                .block(THIRTY_SECONDS);
    }

    /**
     * Placeholder for things that will be defined in Firmware Management service's model package
     * once it exists.
     */
    public record FutureFirmwareManagementModel(
            UUID updateId,
            String updateVersion,
            UUID systemId,
            String message,
            List<String> availableFirmwareVersions) {}
}
