package com.generac.ces.systemgateway.service.system;

import com.generac.ces.essdataprovider.model.BaseDeviceModel;
import com.generac.ces.essdataprovider.model.BatteryPropertiesDto;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.essdataprovider.model.DeviceSettings;
import com.generac.ces.essdataprovider.model.DeviceSettingsResponse;
import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.helper.DeviceSettingsMapper;
import com.generac.ces.systemgateway.model.DeviceStateResponse;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.service.SystemSettingCacheService;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class EssSystemService {

    private final WebClient systemServiceWebClient;

    private final WebClient essDataProviderWebClient;

    private final CacheStore<SystemResponse> cache;
    private final EssDataProviderService essDataProviderService;

    private final DeviceSettingsMapper deviceSettingsMapper;
    private final SystemSettingCacheService<DeviceSetting> systemSettingCacheService;

    @Autowired
    public EssSystemService(
            WebClient systemServiceWebClient,
            WebClient essDataProviderWebClient,
            EssDataProviderService essDataProviderService,
            @Qualifier("essSystemMsCache") CacheStore<SystemResponse> essSystemMsCache,
            DeviceSettingsMapper deviceSettingsMapper,
            SystemSettingCacheService systemSettingCacheService) {
        this.systemServiceWebClient = systemServiceWebClient;
        this.essDataProviderWebClient = essDataProviderWebClient;
        this.essDataProviderService = essDataProviderService;
        this.cache = essSystemMsCache;
        this.deviceSettingsMapper = deviceSettingsMapper;
        this.systemSettingCacheService = systemSettingCacheService;
    }

    public Flux<String> retrieveBatteryRCPNs(String systemID) {
        return systemServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemID).path("/devicemap").build())
                .retrieve()
                .onStatus(
                        HttpStatus.NOT_FOUND::equals,
                        resp -> {
                            throw new ResourceNotFoundException(systemID);
                        })
                .bodyToMono(DeviceMapResponseDto.class)
                .map(DeviceMapResponseDto::getBatteries)
                .flatMapMany(Flux::fromIterable)
                .map(BaseDeviceModel::getId);
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
        return systemServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemId.toString()).build())
                .exchangeToMono(
                        clientResponse -> {
                            if (!clientResponse
                                    .statusCode()
                                    .is2xxSuccessful()) { // handle all unsuccessful statuses here
                                if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                    throw new ResourceNotFoundException(
                                            "ESS system id = " + systemId + " not found.");
                                } else {
                                    throw new InternalServerException(
                                            "Error occurred on ESS metadata retrieval for system id"
                                                    + " = "
                                                    + systemId
                                                    + ".");
                                }
                            }
                            return clientResponse.bodyToMono(
                                    new ParameterizedTypeReference<SystemResponse>() {});
                        })
                .timeout(Duration.ofSeconds(5));
    }

    public Mono<DeviceStateResponse> getDeviceState(UUID systemId, String deviceId) {
        DeviceTypeOuterClass.DeviceType deviceType =
                getDeviceType(deviceId, String.valueOf(systemId));

        return getSystemBySystemIdMono(systemId)
                .map(SystemResponse::getRcpId)
                .flatMap(
                        hostRcpn ->
                                switch (deviceType) {
                                    case BATTERY -> retrieveBatterySettings(systemId, hostRcpn)
                                            .map(
                                                    r ->
                                                            updateAndRetrieveDeviceState(
                                                                    r,
                                                                    systemId,
                                                                    deviceId,
                                                                    deviceType));
                                    case INVERTER -> retrieveInverterSettings(systemId, hostRcpn)
                                            .map(
                                                    r ->
                                                            updateAndRetrieveDeviceState(
                                                                    r,
                                                                    systemId,
                                                                    deviceId,
                                                                    deviceType));
                                    default -> Mono.error(
                                            new InternalServerException("Invalid device type."));
                                });
    }

    private DeviceStateResponse updateAndRetrieveDeviceState(
            DeviceSettingsResponse deviceSettingsResponse,
            UUID systemId,
            String deviceId,
            DeviceTypeOuterClass.DeviceType deviceType) {
        com.generac.ces.systemgateway.model.device.DeviceSettingsResponse response =
                systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        deviceSettingsMapper.mapDeviceSettingsEssDpToDeviceSettings(
                                deviceSettingsResponse),
                        systemId,
                        RequestType.INSTANT_DEVICE_STATE_PATCH);
        return getDeviceStateResponse(
                deviceSettingsMapper.mapEssDpDeviceSettingsResponseToDeviceSettingsResponse(
                        response),
                deviceId,
                systemId,
                deviceType);
    }

    private DeviceStateResponse getDeviceStateResponse(
            DeviceSettingsResponse result,
            String deviceId,
            UUID systemId,
            DeviceTypeOuterClass.DeviceType deviceType) {
        DeviceSettings.DeviceSettingsMetadata deviceStateMetadata =
                result.getDevices().stream()
                        .filter(deviceSettings -> deviceSettings.getDeviceId().equals(deviceId))
                        .flatMap(deviceSettings -> deviceSettings.getMetadata().stream())
                        .filter(
                                setting ->
                                        setting.getName()
                                                .equals(
                                                        deviceType
                                                                        == DeviceTypeOuterClass
                                                                                .DeviceType.BATTERY
                                                                ? com.generac.ces.essdataprovider
                                                                        .enums.DeviceSetting
                                                                        .BATTERY_STATE
                                                                : com.generac.ces.essdataprovider
                                                                        .enums.DeviceSetting.STATE))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                deviceType.toString()
                                                        + " State setting not found"));

        boolean enable =
                switch (deviceStateMetadata.getValue().intValue()) {
                    case 0 -> false;
                    case 1 -> true;
                    default -> throw new IllegalStateException(
                            "Invalid value for "
                                    + deviceType
                                    + " State: "
                                    + deviceStateMetadata.getValue());
                };

        return DeviceStateResponse.builder()
                .systemId(systemId)
                .deviceId(deviceId)
                .enable(enable)
                .deviceType(deviceType)
                .eventSettingsChangedTime(deviceStateMetadata.getUpdatedTimestampUtc())
                .build();
    }

    public Mono<com.generac.ces.systemgateway.model.device.DeviceSettingsResponse>
            getBatterySettings(UUID systemId) {
        return getSystemBySystemIdMono(systemId)
                .map(SystemResponse::getRcpId)
                .flatMap(hostRcpn -> retrieveBatterySettings(systemId, hostRcpn))
                .flatMap(
                        batterySettings ->
                                filterOutState(
                                        batterySettings,
                                        com.generac.ces.essdataprovider.enums.DeviceSetting
                                                .BATTERY_STATE))
                .map(deviceSettingsMapper::mapDeviceSettingsEssDpToDeviceSettings)
                .map(
                        r ->
                                systemSettingCacheService
                                        .updateDeviceSettingsResponseWithCachedSettings(
                                                r,
                                                systemId,
                                                RequestType.INSTANT_BATTERY_SETTINGS_PATCH));
    }

    private Mono<DeviceSettingsResponse> retrieveBatterySettings(UUID systemId, String hostRcpn) {
        return essDataProviderWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/hosts/")
                                        .pathSegment(hostRcpn)
                                        .path("/batteries/settings")
                                        .build())
                .retrieve()
                .onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                throw new ResourceNotFoundException(
                                        "ESS system id = " + systemId + " not found.");
                            }
                            return null;
                        })
                .bodyToMono(DeviceSettingsResponse.class);
    }

    public Mono<BatteryPropertiesDto> getBatteryProperties(UUID systemId, String deviceRcpn) {
        return retrieveBatteryRCPNs(String.valueOf(systemId))
                .collectList()
                .map(
                        deviceRcpnList -> {
                            if (!deviceRcpnList.contains(deviceRcpn)) {
                                throw new ResourceNotFoundException(
                                        "Provided battery rcpn: "
                                                + deviceRcpn
                                                + " not found within system: "
                                                + systemId);
                            }
                            return deviceRcpn;
                        })
                .flatMap(r -> essDataProviderService.getBatteryProperties(deviceRcpn));
    }

    public Mono<com.generac.ces.systemgateway.model.device.DeviceSettingsResponse>
            getInverterSettings(UUID systemId) {
        return getSystemBySystemIdMono(systemId)
                .map(SystemResponse::getRcpId)
                .flatMap(hostRcpn -> retrieveInverterSettings(systemId, hostRcpn))
                .flatMap(
                        inverterSettings ->
                                filterOutState(
                                        inverterSettings,
                                        com.generac.ces.essdataprovider.enums.DeviceSetting.STATE))
                .map(deviceSettingsMapper::mapDeviceSettingsEssDpToDeviceSettings)
                .map(
                        r ->
                                systemSettingCacheService
                                        .updateDeviceSettingsResponseWithCachedSettings(
                                                r,
                                                systemId,
                                                RequestType.INSTANT_INVERTER_SETTINGS_PATCH));
    }

    public Mono<com.generac.ces.systemgateway.model.device.DeviceSettingsResponse> getPvlSettings(
            UUID systemId) {
        return getSystemBySystemIdMono(systemId)
                .map(SystemResponse::getRcpId)
                .flatMap(hostRcpn -> retrievePvlSettings(systemId, hostRcpn))
                .map(deviceSettingsMapper::mapDeviceSettingsEssDpToDeviceSettings)
                .map(
                        r ->
                                systemSettingCacheService
                                        .updateDeviceSettingsResponseWithCachedSettings(
                                                r,
                                                systemId,
                                                RequestType.INSTANT_PVLINK_SETTINGS_PATCH));
    }

    private Mono<DeviceSettingsResponse> filterOutState(
            DeviceSettingsResponse response,
            com.generac.ces.essdataprovider.enums.DeviceSetting state) {
        List<DeviceSettings> filteredDevices =
                response.getDevices().stream()
                        .map(
                                device -> {
                                    List<DeviceSettings.DeviceSettingsMetadata> filteredMetadata =
                                            device.getMetadata().stream()
                                                    .filter(
                                                            metadata ->
                                                                    !metadata.getName()
                                                                            .equals(state))
                                                    .toList();
                                    device.setMetadata(filteredMetadata);
                                    return device;
                                })
                        .toList();
        response.setDevices(filteredDevices);
        return Mono.just(response);
    }

    private Mono<DeviceSettingsResponse> retrieveInverterSettings(UUID systemId, String hostRcpn) {
        return essDataProviderWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/inverters/")
                                        .pathSegment(hostRcpn)
                                        .path("settings")
                                        .build())
                .retrieve()
                .onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                throw new ResourceNotFoundException(
                                        "ESS system id = " + systemId + " not found.");
                            }
                            return null;
                        })
                .bodyToMono(DeviceSettingsResponse.class);
    }

    private Mono<DeviceSettingsResponse> retrievePvlSettings(UUID systemId, String hostRcpn) {
        return essDataProviderWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/inverters/")
                                        .pathSegment(hostRcpn)
                                        .pathSegment("pvls")
                                        .path("settings")
                                        .build())
                .retrieve()
                .onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                throw new ResourceNotFoundException(
                                        "ESS system id = " + systemId + " not found.");
                            }
                            return null;
                        })
                .bodyToMono(DeviceSettingsResponse.class);
    }

    public DeviceTypeOuterClass.DeviceType getDeviceType(String deviceId, String systemId) {
        return systemServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment(systemId).path("/devicemap").build())
                .retrieve()
                .onStatus(
                        HttpStatus.NOT_FOUND::equals,
                        resp -> {
                            throw new ResourceNotFoundException(systemId);
                        })
                .bodyToMono(DeviceMapResponseDto.class)
                .map(r -> findDeviceType(r, deviceId))
                .block();
    }

    public DeviceTypeOuterClass.DeviceType findDeviceType(
            DeviceMapResponseDto responseDto, String deviceId) {
        if (responseDto.getBatteries().stream()
                .anyMatch(device -> device.getId().equals(deviceId))) {
            return DeviceTypeOuterClass.DeviceType.BATTERY;
        } else if (responseDto.getInverters().stream()
                .anyMatch(device -> device.getId().equals(deviceId))) {
            return DeviceTypeOuterClass.DeviceType.INVERTER;
        }
        throw new ResourceNotFoundException(
                "Unable to find device type for provided deviceId: " + deviceId);
    }
}
