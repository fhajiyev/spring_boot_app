package com.generac.ces.systemgateway.service.odin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.system.control.api.configuration.OdinMetadataRequestHeaders;
import com.generac.ces.system.control.subcontrol.Subcontrol.SubControlType;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.helper.BatterySettingUpdateRequestConverter;
import com.generac.ces.systemgateway.helper.ErrorHandlingHelper;
import com.generac.ces.systemgateway.helper.EventsListMapper;
import com.generac.ces.systemgateway.helper.JsonUtil;
import com.generac.ces.systemgateway.helper.Utils;
import com.generac.ces.systemgateway.model.ActiveSystemModeOdinRequest;
import com.generac.ces.systemgateway.model.ActiveSystemModeOdinResponse;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.EventsListResponse;
import com.generac.ces.systemgateway.model.ExportLimitRequest;
import com.generac.ces.systemgateway.model.InverterSettingControlMessageRequest;
import com.generac.ces.systemgateway.model.OdinBatterySettingRequest;
import com.generac.ces.systemgateway.model.OdinExportLimitRequest;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.OdinStartStreamRequest;
import com.generac.ces.systemgateway.model.OdinSystemModeInstantRequest;
import com.generac.ces.systemgateway.model.PvLinkSettingControlMessageRequest;
import com.generac.ces.systemgateway.model.SystemModeControlMessageRequest;
import com.generac.ces.systemgateway.model.common.Actor;
import com.generac.ces.systemgateway.model.common.StreamInterval;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateResponse;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateResponse;
import com.generac.ces.systemgateway.service.SystemSettingCacheService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class OdinService {
    @Autowired private EventsListMapper eventsListMapper;

    @Autowired private SystemSettingCacheService<DeviceSetting> systemSettingCacheService;

    private final WebClient odinServiceWebClient;

    private final EssSystemService essSystemService;

    private static final String awaitResponse = "awaitResponse";

    private static final String instantControl = "instantControl";

    @Autowired ObjectMapper objectMapper;

    @Autowired
    public OdinService(
            WebClient odinServiceWebClient,
            EssSystemService essSystemService,
            ObjectMapper objectMapper) {
        this.odinServiceWebClient = odinServiceWebClient;
        this.essSystemService = essSystemService;
        this.objectMapper = objectMapper;
    }

    public OdinResponse startStreamingForSystemId(
            String systemId, int duration, SystemType systemType) {
        return startStreamingForSystemIdMono(systemId, duration, systemType).block();
    }

    private Mono<OdinResponse> startStreamingForSystemIdMono(
            String systemId, int duration, SystemType systemType) {
        OdinStartStreamRequest requestDto = new OdinStartStreamRequest();
        requestDto.setDurationSecs(duration);
        requestDto.setInterval(StreamInterval.ONE_SEC);
        requestDto.setSystemType(systemType);
        return odinServiceWebClient
                .post()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemId).build())
                .body(Mono.just(requestDto), OdinStartStreamRequest.class)
                .exchangeToMono(
                        clientResponse -> {
                            if (!clientResponse.statusCode().is2xxSuccessful()) {
                                throw new InternalServerException(
                                        "Failed to send 1Hz streaming request to systemId: "
                                                + systemId);
                            }
                            return clientResponse.bodyToMono(
                                    new ParameterizedTypeReference<OdinResponse>() {});
                        });
    }

    public Mono<ActiveSystemModeUpdateResponse> setActiveSystemModes(
            String systemId,
            ActiveSystemModeUpdateRequest requestDto,
            String callerId,
            String userId) {
        ActiveSystemModeOdinRequest activeSystemModeOdinRequest =
                ActiveSystemModeOdinRequest.toInstance(requestDto);
        return odinServiceWebClient
                .patch()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/systems/{systemId}/inverters/settings")
                                        .build(systemId))
                .header(OdinMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME, callerId)
                .header(OdinMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME, userId)
                .body(Mono.just(activeSystemModeOdinRequest), ActiveSystemModeOdinRequest.class)
                .exchangeToMono(
                        clientResponse -> {
                            if (!clientResponse.statusCode().is2xxSuccessful()) {
                                throw new InternalServerException(
                                        String.format(
                                                "Failed to set Active System Mode Configuration"
                                                        + " for systemId %s.",
                                                systemId));
                            }
                            return clientResponse
                                    .bodyToMono(ActiveSystemModeOdinResponse.class)
                                    .map(
                                            response -> {
                                                if ("SUCCESS".equals(response.getCode())) {
                                                    OdinResponse.EventSettingsChanged
                                                            settingsChanged =
                                                                    (OdinResponse
                                                                                    .EventSettingsChanged)
                                                                            response
                                                                                    .getEventDetails();
                                                    return ActiveSystemModeUpdateResponse.builder()
                                                            .systemId(UUID.fromString(systemId))
                                                            .updateId(
                                                                    UUID.fromString(
                                                                            response.getId()))
                                                            .activeModes(
                                                                    settingsChanged
                                                                            .getInverter()
                                                                            .getActiveSystemModes()
                                                                            .stream()
                                                                            .sorted(
                                                                                    Comparator
                                                                                            .comparing(
                                                                                                    SystemMode
                                                                                                            ::getOrder))
                                                                            .toList())
                                                            .updatedTimestampUtc(
                                                                    settingsChanged
                                                                            .getEventSettingsChangedTime())
                                                            .build();
                                                }

                                                return ErrorHandlingHelper.handleOdinErrorResponse(
                                                        response);
                                            });
                        });
    }

    public Mono<ResponseEntity<SystemModeUpdateResponse>> postSysModeControlMessages(
            UUID systemId,
            SystemModeUpdateRequest systemModeUpdateRequest,
            String callerId,
            String userId,
            Boolean instantControl) {
        if (!instantControl) {
            return odinServiceWebClient
                    .post()
                    .uri(uriBuilder -> uriBuilder.path("/{systemId}/systemMode").build(systemId))
                    .header(
                            OdinMetadataRequestHeaders.Constants.ODIN_CALLER_ID_HEADER_NAME,
                            callerId)
                    .header(OdinMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME, userId)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(
                            Mono.just(
                                    SystemModeControlMessageRequest.toInstance(
                                            systemModeUpdateRequest)),
                            SystemModeControlMessageRequest.class)
                    .retrieve()
                    .bodyToMono(OdinResponse.class)
                    .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                    .onErrorResume(Utils.throwProperError())
                    .map(
                            response ->
                                    SystemModeUpdateResponse.builder()
                                            .systemId(systemId)
                                            .updateId(UUID.fromString(response.getId()))
                                            .build())
                    .map(
                            systemModeUpdateResponse ->
                                    ResponseEntity.accepted().body(systemModeUpdateResponse));
        }

        return odinServiceWebClient
                .patch()
                .uri(uriBuilder -> uriBuilder.path("/systems/{systemId}/mode").build(systemId))
                .header(OdinMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME, callerId)
                .header(OdinMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME, userId)
                .body(
                        Mono.just(OdinSystemModeInstantRequest.toInstance(systemModeUpdateRequest)),
                        OdinSystemModeInstantRequest.class)
                .retrieve()
                .bodyToMono(OdinResponse.class)
                .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                .onErrorResume(Utils.throwProperError())
                .map(
                        response -> {
                            if ("SUCCESS".equals(response.getCode())
                                    && response.getEventDetails()
                                            instanceof OdinResponse.EventSettingsChanged) {
                                OdinResponse.EventSettingsChanged settingsChanged =
                                        (OdinResponse.EventSettingsChanged)
                                                response.getEventDetails();
                                return new SystemModeUpdateResponse(
                                        systemId,
                                        UUID.fromString(response.getId()),
                                        settingsChanged.getSystemMode().getSystemMode(),
                                        settingsChanged.getEventSettingsChangedTime());
                            }

                            return ErrorHandlingHelper.handleOdinErrorResponse(response);
                        })
                .map(r -> ResponseEntity.ok().body(r));
    }

    public Mono<OdinResponse> setBatterySettings(
            UUID systemId,
            DeviceSettingsUpdateRequest dto,
            String callerId,
            String userId,
            List<String> rcpns) {
        OdinBatterySettingRequest request =
                BatterySettingUpdateRequestConverter.mapToOdinBatterySetting(dto, rcpns);
        return odinBatterySettingsRequest(systemId, callerId, userId, request);
    }

    public Mono<OdinResponse> setInverterSettings(
            UUID systemId, DeviceSettingsUpdateRequest dto, String callerId, String userId) {
        InverterSettingControlMessageRequest request =
                InverterSettingControlMessageRequest.toInstance(dto);
        return odinInverterSettingsRequest(systemId, callerId, userId, request);
    }

    public Mono<OdinResponse> setPvLinkSettings(
            UUID systemId,
            DeviceSettingsUpdateRequest dto,
            String callerId,
            String userId,
            String pvlRcpn) {
        PvLinkSettingControlMessageRequest request =
                PvLinkSettingControlMessageRequest.toInstance(dto, pvlRcpn);
        return odinPvlSettingsRequest(systemId, callerId, userId, request);
    }

    public Mono<OdinResponse> setExportLimit(
            String systemId, ExportLimitRequest dto, String callerId) {
        OdinExportLimitRequest.ControlMessage controlMessage =
                new OdinExportLimitRequest.ControlMessage();
        controlMessage.setExportLimitRequest(dto);
        OdinExportLimitRequest request = new OdinExportLimitRequest();
        request.setControlMessage(controlMessage);

        return odinServiceWebClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/systems/{systemId}/export-limit")
                                        .queryParam(instantControl, true)
                                        .queryParam(awaitResponse, true)
                                        .build(systemId))
                .header(CallerMetadataRequestHeaders.Constants.ODIN_CALLER_ID_HEADER_NAME, callerId)
                .body(Mono.just(request), OdinExportLimitRequest.class)
                .exchangeToMono(
                        clientResponse -> {
                            if (!clientResponse.statusCode().is2xxSuccessful()) {
                                throw new InternalServerException(
                                        "Failed to set Export Limit Setpoint for systemId: "
                                                + systemId);
                            }
                            return clientResponse.bodyToMono(OdinResponse.class);
                        });
    }

    public Mono<OdinResponse> odinBatterySettingsRequest(
            UUID systemId, String callerId, String userId, OdinBatterySettingRequest request) {
        return odinServiceWebClient
                .patch()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/systems/{systemId}/batteries/settings")
                                        .queryParam(awaitResponse, true)
                                        .build(systemId))
                .header(OdinMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME, callerId)
                .header(OdinMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME, userId)
                .body(Mono.just(request), OdinBatterySettingRequest.class)
                .retrieve()
                .bodyToMono(OdinResponse.class)
                .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                .onErrorResume(Utils.throwProperError());
    }

    public Mono<OdinResponse> odinInverterSettingsRequest(
            UUID systemId,
            String callerId,
            String userId,
            InverterSettingControlMessageRequest request) {
        return odinServiceWebClient
                .patch()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/systems/{systemId}/inverters/settings")
                                        .queryParam(awaitResponse, true)
                                        .build(systemId))
                .header(OdinMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME, callerId)
                .header(OdinMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME, userId)
                .body(Mono.just(request), InverterSettingControlMessageRequest.class)
                .retrieve()
                .bodyToMono(OdinResponse.class)
                .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                .onErrorResume(Utils.throwProperError());
    }

    public Mono<OdinResponse> odinPvlSettingsRequest(
            UUID systemId,
            String callerId,
            String userId,
            PvLinkSettingControlMessageRequest request) {
        return odinServiceWebClient
                .patch()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/systems/{systemId}/pvlink/settings")
                                        .queryParam(awaitResponse, true)
                                        .build(systemId))
                .header(OdinMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME, callerId)
                .header(OdinMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME, userId)
                .body(Mono.just(request), PvLinkSettingControlMessageRequest.class)
                .retrieve()
                .bodyToMono(OdinResponse.class)
                .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                .onErrorResume(Utils.throwProperError());
    }

    public Mono<EventsListResponse> getEventsList(
            String systemId,
            List<Actor> actorIds,
            List<SubControlType> subControlTypes,
            String start,
            String end) {
        return odinServiceWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/{systemId}/events")
                                        .queryParam("actorId", Actor.convertToStringList(actorIds))
                                        .queryParam("subControlType", subControlTypes)
                                        .queryParam("start", start)
                                        .queryParam("end", end)
                                        .build(systemId))
                .retrieve()
                .bodyToMono(String.class)
                .doOnEach(Utils.logOnError(e -> log.error(e.getMessage(), e)))
                .onErrorResume(Utils.throwProperError())
                .map(
                        response -> {
                            try {
                                return EventsListMapper.INSTANCE.convert(
                                        JsonUtil.toListResponse(response));
                            } catch (InvalidProtocolBufferException e) {
                                throw new InternalServerException(
                                        "Error: Unable to process event list response for system: "
                                                + systemId);
                            }
                        });
    }
}
