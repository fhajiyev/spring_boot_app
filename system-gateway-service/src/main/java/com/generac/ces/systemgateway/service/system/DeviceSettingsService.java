package com.generac.ces.systemgateway.service.system;

import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.helper.DeviceCompositeKey;
import com.generac.ces.systemgateway.helper.DeviceSettingUpdateResponseConverter;
import com.generac.ces.systemgateway.helper.DeviceSettingsCachingUtil;
import com.generac.ces.systemgateway.helper.DeviceStateResponseConvert;
import com.generac.ces.systemgateway.helper.ErrorHandlingHelper;
import com.generac.ces.systemgateway.helper.InverterSettingsResponseConverter;
import com.generac.ces.systemgateway.helper.PvlSettingsResponseConverter;
import com.generac.ces.systemgateway.model.DeviceStateRequest;
import com.generac.ces.systemgateway.model.DeviceStateResponse;
import com.generac.ces.systemgateway.model.InverterSettingControlMessageRequest;
import com.generac.ces.systemgateway.model.OdinBatterySettingRequest;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.ParameterTimestampMap;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.service.SystemSettingCacheService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DeviceSettingsService {

    @Autowired private SystemSettingCacheService<DeviceSetting> systemSettingCacheService;
    private final EssSystemService essSystemService;
    private final OdinService odinService;
    private final CacheStore<ParameterTimestampMap> remoteSettingsCache;

    @Autowired
    public DeviceSettingsService(
            @Qualifier("remoteSettingsCache") CacheStore<ParameterTimestampMap> remoteSettingsCache,
            OdinService odinService,
            EssSystemService essSystemService,
            SystemSettingCacheService<DeviceSetting> systemSettingCacheService) {
        this.odinService = odinService;
        this.remoteSettingsCache = remoteSettingsCache;
        this.essSystemService = essSystemService;
        this.systemSettingCacheService = systemSettingCacheService;
    }

    public Mono<DeviceStateResponse> setDeviceState(
            UUID systemId,
            String deviceRcpn,
            String callerId,
            String userId,
            DeviceStateRequest deviceStateRequest,
            SystemType systemType) {
        switch (systemType) {
            case ESS:
                return setEssDeviceState(
                        deviceStateRequest, callerId, userId, systemId, deviceRcpn);
            default:
                throw new IllegalArgumentException("Unsupported SystemType: " + systemType);
        }
    }

    public Mono<DeviceStateResponse> setEssDeviceState(
            DeviceStateRequest deviceStateRequest,
            String callerId,
            String userId,
            UUID systemId,
            String deviceRcpn) {
        DeviceState deviceState = DeviceState.fromBoolean(deviceStateRequest.getEnable());
        DeviceTypeOuterClass.DeviceType deviceType =
                essSystemService.getDeviceType(deviceRcpn, String.valueOf(systemId));

        // Get corresponding DeviceSetting from DeviceState
        DeviceSetting deviceSetting = getDeviceSettingFromDeviceState(deviceType);
        DeviceSettingsUpdateRequest updateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(deviceSetting)
                                                .value((double) deviceState.getStateVal())
                                                .build()))
                        .build();
        if (cachedKeysMatchDeviceSettingRequestKeys(
                updateRequest, deviceRcpn, systemId, RequestType.INSTANT_DEVICE_STATE_PATCH)) {
            List<DeviceCompositeKey> cacheKeyList =
                    getKeyListFromDeviceSettingsRequest(
                            RequestType.INSTANT_DEVICE_STATE_PATCH,
                            updateRequest,
                            systemId,
                            deviceRcpn);
            systemSettingCacheService.throwOnRateLimit(
                    systemId, RequestType.INSTANT_DEVICE_STATE_PATCH, cacheKeyList);
        }
        switch (deviceType) {
            case BATTERY -> {
                return Optional.ofNullable(deviceRcpn)
                        .map(r -> Mono.just(List.of(r)))
                        .orElseGet(
                                () ->
                                        essSystemService
                                                .retrieveBatteryRCPNs(String.valueOf(systemId))
                                                .collectList())
                        .flatMap(
                                rcpns -> {
                                    OdinBatterySettingRequest.ControlMessage controlMessage =
                                            new OdinBatterySettingRequest.ControlMessage();
                                    controlMessage.setRcpns(rcpns);
                                    controlMessage.setState(deviceState);
                                    OdinBatterySettingRequest request =
                                            new OdinBatterySettingRequest();
                                    request.setControlMessage(controlMessage);

                                    return odinService
                                            .odinBatterySettingsRequest(
                                                    systemId, callerId, userId, request)
                                            .map(
                                                    response -> {
                                                        if ("SUCCESS".equals(response.getCode())
                                                                && response.getEventDetails()
                                                                        instanceof
                                                                        OdinResponse
                                                                                .EventSettingsChanged) {
                                                            cacheUpdatedSettings(
                                                                    updateRequest,
                                                                    response,
                                                                    systemId,
                                                                    RequestType
                                                                            .INSTANT_DEVICE_STATE_PATCH,
                                                                    rcpns,
                                                                    DeviceTypeOuterClass.DeviceType
                                                                            .BATTERY);
                                                            return DeviceStateResponseConvert
                                                                    .toInstance(
                                                                            response,
                                                                            systemId,
                                                                            deviceType);
                                                        }

                                                        return ErrorHandlingHelper
                                                                .handleOdinErrorResponse(response);
                                                    });
                                });
            }
            case INVERTER -> {
                return Optional.of(Mono.just(deviceRcpn))
                        .orElseGet(
                                () ->
                                        Mono.just(
                                                essSystemService
                                                        .getSystemBySystemId(systemId)
                                                        .getRcpId()))
                        .flatMap(
                                rcpn -> {
                                    InverterSettingControlMessageRequest request =
                                            InverterSettingControlMessageRequest.toInstance(
                                                    deviceState, rcpn);
                                    return odinService
                                            .odinInverterSettingsRequest(
                                                    systemId, callerId, userId, request)
                                            .map(
                                                    response -> {
                                                        if ("SUCCESS".equals(response.getCode())
                                                                && response.getEventDetails()
                                                                        instanceof
                                                                        OdinResponse
                                                                                .EventSettingsChanged) {
                                                            cacheUpdatedSettings(
                                                                    updateRequest,
                                                                    response,
                                                                    systemId,
                                                                    RequestType
                                                                            .INSTANT_DEVICE_STATE_PATCH,
                                                                    List.of(rcpn),
                                                                    DeviceTypeOuterClass.DeviceType
                                                                            .INVERTER);
                                                            return DeviceStateResponseConvert
                                                                    .toInstance(
                                                                            response,
                                                                            systemId,
                                                                            deviceType);
                                                        }

                                                        return ErrorHandlingHelper
                                                                .handleOdinErrorResponse(response);
                                                    });
                                });
            }
            default -> throw new InternalServerException("Invalid device type.");
        }
    }

    public Mono<DeviceStateResponse> getDeviceState(UUID systemId, String deviceRcpn) {
        // hardcoding for now as we only have one system type.
        SystemType systemType = SystemType.ESS;

        switch (systemType) {
            case ESS:
                return essSystemService.getDeviceState(systemId, deviceRcpn);
            default:
                throw new IllegalArgumentException("Unsupported SystemType: " + systemType);
        }
    }

    public Mono<DeviceSettingUpdateResponse> setDeviceSettings(
            UUID systemId,
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            String callerId,
            String userId,
            String providedRcpn,
            RequestType requestType,
            SystemType systemType) {
        switch (systemType) {
            case ESS -> {
                return setEssDeviceSettings(
                        systemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        providedRcpn,
                        requestType);
            }
            default -> throw new IllegalArgumentException("Unsupported SystemType: " + systemType);
        }
    }

    private Mono<DeviceSettingUpdateResponse> setEssDeviceSettings(
            UUID systemId,
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            String callerId,
            String userId,
            String providedRcpn,
            RequestType requestType) {
        String deviceRcpn =
                Objects.requireNonNullElseGet(
                        providedRcpn,
                        () ->
                                (requestType == RequestType.INSTANT_BATTERY_SETTINGS_PATCH)
                                        ? providedRcpn
                                        : essSystemService
                                                .getSystemBySystemId(systemId)
                                                .getRcpId());
        if (cachedKeysMatchDeviceSettingRequestKeys(
                deviceSettingsUpdateRequest, deviceRcpn, systemId, requestType)) {
            List<DeviceCompositeKey> cacheKeyList =
                    getKeyListFromDeviceSettingsRequest(
                            requestType, deviceSettingsUpdateRequest, systemId, deviceRcpn);
            systemSettingCacheService.throwOnRateLimit(systemId, requestType, cacheKeyList);
        }
        switch (requestType) {
            case INSTANT_BATTERY_SETTINGS_PATCH -> {
                return Optional.of(Mono.just(List.of(deviceRcpn)))
                        .orElseGet(
                                () ->
                                        essSystemService
                                                .retrieveBatteryRCPNs(String.valueOf(systemId))
                                                .collectList())
                        .flatMap(
                                rcpns ->
                                        odinService
                                                .setBatterySettings(
                                                        systemId,
                                                        deviceSettingsUpdateRequest,
                                                        callerId,
                                                        userId,
                                                        rcpns)
                                                .map(
                                                        response -> {
                                                            if ("SUCCESS".equals(response.getCode())
                                                                    && response.getEventDetails()
                                                                            instanceof
                                                                            OdinResponse
                                                                                    .EventSettingsChanged) {
                                                                cacheUpdatedSettings(
                                                                        deviceSettingsUpdateRequest,
                                                                        response,
                                                                        systemId,
                                                                        requestType,
                                                                        rcpns,
                                                                        DeviceTypeOuterClass
                                                                                .DeviceType
                                                                                .BATTERY);
                                                                return DeviceSettingUpdateResponseConverter
                                                                        .toInstance(
                                                                                response, systemId);
                                                            }
                                                            return ErrorHandlingHelper
                                                                    .handleOdinErrorResponse(
                                                                            response);
                                                        }));
            }
            case INSTANT_INVERTER_SETTINGS_PATCH -> {
                return odinService
                        .setInverterSettings(
                                systemId, deviceSettingsUpdateRequest, callerId, userId)
                        .map(
                                response -> {
                                    if ("SUCCESS".equals(response.getCode())
                                            && response.getEventDetails()
                                                    instanceof
                                                    OdinResponse.EventSettingsChanged
                                                    eventSettingsChanged) {
                                        cacheUpdatedSettings(
                                                deviceSettingsUpdateRequest,
                                                response,
                                                systemId,
                                                requestType,
                                                List.of(deviceRcpn),
                                                DeviceTypeOuterClass.DeviceType.INVERTER);
                                        return DeviceSettingUpdateResponse.builder()
                                                .systemId(systemId)
                                                .updateId(UUID.fromString(response.getId()))
                                                .devices(
                                                        List.of(
                                                                DeviceSettingUpdateResponse.Device
                                                                        .builder()
                                                                        .deviceId(deviceRcpn)
                                                                        .settings(
                                                                                InverterSettingsResponseConverter
                                                                                        .mapDevices(
                                                                                                eventSettingsChanged))
                                                                        .build()))
                                                .updatedTimestampUtc(
                                                        eventSettingsChanged
                                                                .getEventSettingsChangedTime())
                                                .build();
                                    }

                                    return ErrorHandlingHelper.handleOdinErrorResponse(response);
                                });
            }
            case INSTANT_PVLINK_SETTINGS_PATCH -> {
                return odinService
                        .setPvLinkSettings(
                                systemId,
                                deviceSettingsUpdateRequest,
                                callerId,
                                userId,
                                providedRcpn)
                        .map(
                                response -> {
                                    if ("SUCCESS".equals(response.getCode())
                                            && response.getEventDetails()
                                                    instanceof
                                                    OdinResponse.EventSettingsChanged
                                                    eventSettingsChanged) {
                                        cacheUpdatedSettings(
                                                deviceSettingsUpdateRequest,
                                                response,
                                                systemId,
                                                requestType,
                                                List.of(deviceRcpn),
                                                DeviceTypeOuterClass.DeviceType.PVLINK);
                                        return DeviceSettingUpdateResponse.builder()
                                                .systemId(systemId)
                                                .updateId(UUID.fromString(response.getId()))
                                                .devices(
                                                        List.of(
                                                                DeviceSettingUpdateResponse.Device
                                                                        .builder()
                                                                        .deviceId(deviceRcpn)
                                                                        .settings(
                                                                                PvlSettingsResponseConverter
                                                                                        .mapDevices(
                                                                                                eventSettingsChanged))
                                                                        .build()))
                                                .updatedTimestampUtc(
                                                        eventSettingsChanged
                                                                .getEventSettingsChangedTime())
                                                .build();
                                    }

                                    return ErrorHandlingHelper.handleOdinErrorResponse(response);
                                });
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported RequestType: " + requestType);
        }
    }

    private void cacheUpdatedSettings(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            OdinResponse response,
            UUID systemId,
            RequestType requestType,
            List<String> rcpns,
            DeviceTypeOuterClass.DeviceType deviceType) {
        ParameterTimestampValueMap<DeviceSetting> currentCacheContents =
                systemSettingCacheService.getSettings(systemId, requestType);
        ParameterTimestampValueMap<DeviceSetting> batterySettingsToBeCached =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        deviceSettingsUpdateRequest,
                        response,
                        currentCacheContents,
                        rcpns,
                        requestType,
                        deviceType);
        systemSettingCacheService.saveSettings(systemId, requestType, batterySettingsToBeCached);
    }

    private List<DeviceCompositeKey> getKeyListFromDeviceSettingsRequest(
            RequestType requestType,
            DeviceSettingsUpdateRequest updateRequest,
            UUID systemId,
            String rcpn) {
        ParameterTimestampValueMap cache =
                systemSettingCacheService.getSettings(systemId, requestType);
        List<DeviceSetting> requestSettings =
                updateRequest.settings().stream()
                        .map(DeviceSettingsUpdateRequest.DeviceSettingChange::name)
                        .toList();
        List<DeviceCompositeKey> cacheKeyList = new ArrayList<>();
        if (cache != null) {
            for (DeviceSetting key : requestSettings) {
                if (cache.getParameterTimestampValue(
                                new DeviceCompositeKey(key.getFormattedSettingName(), rcpn))
                        != null) {
                    cacheKeyList.add(new DeviceCompositeKey(key.getFormattedSettingName(), rcpn));
                }
            }
        }
        return cacheKeyList;
    }

    private boolean cachedKeysMatchDeviceSettingRequestKeys(
            DeviceSettingsUpdateRequest updateRequest,
            String rcpn,
            UUID systemId,
            RequestType requestType) {
        ParameterTimestampValueMap cache =
                systemSettingCacheService.getSettings(systemId, requestType);
        if (cache == null) return false;
        List<DeviceSetting> requestSettings =
                updateRequest.settings().stream()
                        .map(DeviceSettingsUpdateRequest.DeviceSettingChange::name)
                        .toList();
        for (DeviceSetting key : requestSettings) {
            if (cache.getParameterTimestampValue(
                            new DeviceCompositeKey(key.getFormattedSettingName(), rcpn))
                    == null) return false;
        }
        return true;
    }

    private DeviceSetting getDeviceSettingFromDeviceState(
            DeviceTypeOuterClass.DeviceType deviceType) {
        switch (deviceType) {
            case INVERTER -> {
                return DeviceSetting.STATE;
            }
            case BATTERY -> {
                return DeviceSetting.BATTERY_STATE;
            }
            case PVLINK -> {
                return DeviceSetting.PVLINK_STATE;
            }
            default -> throw new IllegalArgumentException("Unsupported DeviceType: " + deviceType);
        }
    }
}
