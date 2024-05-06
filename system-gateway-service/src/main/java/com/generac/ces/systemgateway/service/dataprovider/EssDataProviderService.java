package com.generac.ces.systemgateway.service.dataprovider;

import com.generac.ces.essdataprovider.enums.InverterSettingFields;
import com.generac.ces.essdataprovider.model.BatteryPropertiesDto;
import com.generac.ces.essdataprovider.model.InverterSettingRequestDto;
import com.generac.ces.essdataprovider.model.InverterSettingResponseDto;
import com.generac.ces.essdataprovider.model.ModeResponseDto;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.helper.Utils;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class EssDataProviderService {

    private final WebClient essDataProviderWebClient;

    @Autowired
    public EssDataProviderService(WebClient essDataProviderWebClient) {
        this.essDataProviderWebClient = essDataProviderWebClient;
    }

    public Mono<InverterSettingResponseDto> getAllowedSysModesByInverter(String deviceRcpn) {
        InverterSettingRequestDto requestDto = new InverterSettingRequestDto();
        requestDto.setFields(
                new HashSet<>(Collections.singleton(InverterSettingFields.sysModesAllowed)));
        return essDataProviderWebClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/inverter/")
                                        .pathSegment(deviceRcpn)
                                        .path("/setting")
                                        .build())
                .body(Mono.just(requestDto), InverterSettingRequestDto.class)
                .retrieve()
                .onStatus(
                        HttpStatus.NOT_FOUND::equals,
                        resp -> {
                            throw new ResourceNotFoundException(deviceRcpn);
                        })
                .bodyToMono(InverterSettingResponseDto.class);
    }

    public Mono<ModeResponseDto> getCurrentSysMode(UUID systemId) {
        return essDataProviderWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/systems/{systemId}/mode").build(systemId))
                .retrieve()
                .onStatus(
                        HttpStatus.NOT_FOUND::equals,
                        resp -> {
                            throw new ResourceNotFoundException(systemId.toString());
                        })
                .bodyToMono(ModeResponseDto.class);
    }

    public Mono<BatteryPropertiesDto> getBatteryProperties(String deviceRcpn) {
        return essDataProviderWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/batteries/")
                                        .pathSegment(deviceRcpn)
                                        .path("/settings/full")
                                        .build())
                .retrieve()
                .bodyToMono(BatteryPropertiesDto.class)
                .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                .onErrorResume(Utils.throwProperError());
    }
}
