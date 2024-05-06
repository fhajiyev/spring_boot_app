package com.generac.ces.systemgateway.service.system;

import com.generac.ces.essdataprovider.enums.InverterSettingFields;
import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.essdataprovider.fields.SysModesListField;
import com.generac.ces.essdataprovider.model.InverterSettingResponseDto;
import com.generac.ces.essdataprovider.model.ModeResponseDto;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.exception.UnprocessableEntityException;
import com.generac.ces.systemgateway.helper.DeviceCompositeKey;
import com.generac.ces.systemgateway.helper.RedisUtil;
import com.generac.ces.systemgateway.model.ParameterTimestampMap;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateResponse;
import com.generac.ces.systemgateway.model.system.SystemModeGetResponse;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateResponse;
import com.generac.ces.systemgateway.service.SystemSettingCacheService;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SystemModeService {

    @Autowired private EssSystemService essSystemService;

    @Autowired private EssDataProviderService essDataProviderService;

    @Autowired private OdinService odinService;

    @Autowired private SystemSettingCacheService<SystemMode> systemSettingCacheService;

    @Autowired
    @Qualifier("remoteSettingsCache") private CacheStore<ParameterTimestampMap> remoteSettingsCache;

    public Mono<SystemModeGetResponse> getSystemMode(UUID systemId) {
        SystemResponse systemDevices = essSystemService.getSystemBySystemId(systemId);

        if (systemDevices == null || Strings.isEmpty(systemDevices.getRcpId())) {
            throw new UnprocessableEntityException("Rcp Id not found for systemId: " + systemId);
        }

        Mono<InverterSettingResponseDto> inverterSettingResponseDtoMono =
                essDataProviderService.getAllowedSysModesByInverter(systemDevices.getRcpId());

        Mono<ModeResponseDto> currentSysModeMono =
                essDataProviderService.getCurrentSysMode(systemId);

        return Mono.zip(
                inverterSettingResponseDtoMono,
                currentSysModeMono,
                (availableSysMode, currentSysMode) -> {
                    SysModesListField sysModesAllowedFld =
                            (SysModesListField)
                                    availableSysMode
                                            .getSettings()
                                            .get(InverterSettingFields.sysModesAllowed.name());

                    if (currentSysMode == null || currentSysMode.getSysMode() == null) {
                        throw new ResourceNotFoundException(
                                String.format(
                                        "System mode was not found for systemId: %s", systemId));
                    }

                    OffsetDateTime availableModesTimestampUtc =
                            availableSysMode.getLastUpdatedTimestampUtc();

                    List<SystemMode> allowedModes =
                            sysModesAllowedFld.getValue().stream()
                                    // This is because we're blocking SafetyShutdown on
                                    // setSystemMode,
                                    // so we should not return it as one of the available
                                    // modes
                                    .filter(mode -> !SysModes.SafetyShutdown.equals(mode))
                                    .map(SystemMode::fromEssDpSystemMode)
                                    .filter(mode -> mode != SystemMode.UNKNOWN_SYSTEM_MODE)
                                    .sorted(Comparator.comparing(SystemMode::getOrder))
                                    .toList();

                    return Optional.ofNullable(
                                    systemSettingCacheService.updateSystemModeWithCachedSettings(
                                            currentSysMode,
                                            allowedModes,
                                            systemId,
                                            systemDevices.getBeaconRcpn(),
                                            availableModesTimestampUtc))
                            .orElseGet(
                                    () ->
                                            SystemModeGetResponse.builder()
                                                    .systemId(systemId)
                                                    .updatedTimestampUtc(
                                                            currentSysMode.getTimestampUtc())
                                                    .mode(
                                                            currentSysMode.getSysMode() != null
                                                                    ? SystemMode
                                                                            .fromEssDpSystemMode(
                                                                                    currentSysMode
                                                                                            .getSysMode())
                                                                    : null)
                                                    .activeModes(allowedModes)
                                                    .availableModes(
                                                            SystemMode.getAllModesExceptExcluded())
                                                    .build());
                });
    }

    public Mono<ResponseEntity<SystemModeUpdateResponse>> updateSystemMode(
            SystemType systemType,
            UUID systemId,
            SystemModeUpdateRequest request,
            String callerId,
            String userId,
            Boolean instantControl) {
        String beaconRcpn = essSystemService.getSystemBySystemId(systemId).getBeaconRcpn();
        // Check if the result is in the cache
        if (instantControl) {
            systemSettingCacheService.throwOnRateLimit(
                    systemId,
                    RequestType.INSTANT_SYSTEM_MODE_PATCH,
                    List.of(
                            new DeviceCompositeKey(
                                    DeviceSetting.MODE.getFormattedSettingName(), beaconRcpn)));
        }
        return Mono.defer(
                        () -> {
                            switch (systemType) {
                                case ESS:
                                    return odinService.postSysModeControlMessages(
                                            systemId, request, callerId, userId, instantControl);
                                    // Add more cases for other SystemType values if needed
                                default:
                                    throw new IllegalArgumentException(
                                            "Unsupported SystemType: " + systemType);
                            }
                        })
                .flatMap(
                        responseEntity -> {
                            if (instantControl) {
                                ParameterTimestampValueMap<SystemMode> systemModeSettings =
                                        RedisUtil.generateSystemModeSetting(
                                                responseEntity.getBody().systemMode(),
                                                responseEntity.getBody().updatedTimestampUtc(),
                                                beaconRcpn);

                                systemSettingCacheService.saveSettings(
                                        responseEntity.getBody().systemId(),
                                        RequestType.INSTANT_SYSTEM_MODE_PATCH,
                                        systemModeSettings);
                            }
                            return Mono.just(responseEntity);
                        });
    }

    public Mono<ActiveSystemModeUpdateResponse> setActiveSystemModes(
            UUID systemId,
            ActiveSystemModeUpdateRequest activeSystemModeUpdateRequest,
            SystemType systemType,
            String callerId,
            String userId) {
        String beaconRcpn = essSystemService.getSystemBySystemId(systemId).getBeaconRcpn();
        systemSettingCacheService.throwOnRateLimit(
                systemId,
                RequestType.INSTANT_ACTIVE_SYSTEM_MODE_PATCH,
                List.of(
                        new DeviceCompositeKey(
                                DeviceSetting.ACTIVE_MODES.getFormattedSettingName(), beaconRcpn)));

        return Mono.defer(
                        () -> {
                            switch (systemType) {
                                case ESS:
                                    return odinService.setActiveSystemModes(
                                            systemId.toString(),
                                            activeSystemModeUpdateRequest,
                                            callerId,
                                            userId);
                                    // Add more cases for other SystemType values if needed
                                default:
                                    throw new IllegalArgumentException(
                                            "Unsupported SystemType: " + systemType);
                            }
                        })
                .flatMap(
                        responseEntity -> {
                            ParameterTimestampValueMap<SystemMode> systemModeSettings =
                                    RedisUtil.generateActiveModesSetting(
                                            responseEntity.activeModes(),
                                            responseEntity.updatedTimestampUtc(),
                                            beaconRcpn);

                            systemSettingCacheService.saveSettings(
                                    responseEntity.systemId(),
                                    RequestType.INSTANT_ACTIVE_SYSTEM_MODE_PATCH,
                                    systemModeSettings);
                            return Mono.just(responseEntity);
                        });
    }
}
