package com.generac.ces.systemgateway.service;

import com.generac.ces.essdataprovider.model.ModeResponseDto;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.helper.DeviceCompositeKey;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.device.DeviceSettings;
import com.generac.ces.systemgateway.model.device.DeviceSettingsResponse;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.system.SystemModeGetResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SystemSettingCacheService<T> {

    @Value("${cache.systemSettingsRedisCache.requestRateLimitSeconds}")
    private int requestRateLimitSeconds;

    @Value("${cache.systemSettingsRedisCache.systemSettingsTtlSeconds}")
    private int systemSettingsTtlSeconds;

    private static final String KEY_POSTFIX = "SystemSettings";

    private final RedisTemplate<String, Object> redisTemplate;
    HashOperations<String, Object, ParameterTimestampValueMap<T>> hashOperations;

    @Autowired
    public SystemSettingCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    public void saveSettings(
            UUID systemId, RequestType requestType, ParameterTimestampValueMap<T> settings) {
        String key = getKey(systemId, requestType);
        hashOperations.put(key, requestType.toString(), settings);
        redisTemplate.expire(key, systemSettingsTtlSeconds, TimeUnit.SECONDS);
    }

    public ParameterTimestampValueMap<T> getSettings(UUID systemId, RequestType requestType) {
        String key = getKey(systemId, requestType);
        return hashOperations.get(key, requestType.toString());
    }

    public void evictHashKey(UUID systemId, RequestType requestType, DeviceCompositeKey hashKey) {
        String key = getKey(systemId, requestType);
        hashOperations.delete(key, hashKey);
    }

    public void throwOnRateLimit(
            UUID systemId, RequestType requestType, List<DeviceCompositeKey> cacheKeyList) {
        ParameterTimestampValueMap<T> cache = getSettings(systemId, requestType);
        if (cache != null) {
            Duration duration = calculateDurationInCache(cache, cacheKeyList);

            if (duration.getSeconds() < requestRateLimitSeconds) {
                throw new TooManyRequestsException(
                        String.format(
                                "Request limit has been reached for system id: %s and request type:"
                                        + " %s. Please try again in %s %s.",
                                systemId,
                                requestType,
                                requestRateLimitSeconds - duration.getSeconds(),
                                TimeUnit.SECONDS),
                        duration);
            }
        }
    }

    private Duration calculateDurationInCache(
            ParameterTimestampValueMap cache, List<DeviceCompositeKey> cacheKeyList) {
        if (cacheKeyList.isEmpty()) {
            return Duration.ZERO;
        }
        List<Duration> durations =
                cacheKeyList.stream()
                        .map(
                                key -> {
                                    ParameterTimestampValueMap.ParameterTimestampValue timestamp =
                                            cache.getParameterTimestampValue(key);
                                    return Optional.ofNullable(timestamp)
                                            .map(
                                                    t ->
                                                            Duration.between(
                                                                    t.getTimestamp(),
                                                                    OffsetDateTime.now()))
                                            .orElse(null);
                                })
                        .filter(Objects::nonNull)
                        .toList();
        return durations.stream()
                .max(Duration::compareTo)
                .orElseGet(
                        () -> {
                            log.error("List of durations is empty.");
                            return Duration.ZERO;
                        });
    }

    public SystemModeGetResponse updateSystemModeWithCachedSettings(
            ModeResponseDto currentSysMode,
            List<SystemMode> activeModes,
            UUID systemId,
            String beaconId,
            OffsetDateTime availableModesTimestampUtc) {
        Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
                cachedSettingsMapSystemMode =
                        getSettingsMap(systemId, RequestType.INSTANT_SYSTEM_MODE_PATCH);
        Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
                cachedSettingsMapActiveSystemModes =
                        getSettingsMap(systemId, RequestType.INSTANT_ACTIVE_SYSTEM_MODE_PATCH);

        if (cachedSettingsMapSystemMode.isEmpty() && cachedSettingsMapActiveSystemModes.isEmpty()) {
            return null;
        }

        DeviceCompositeKey systemModeCompositeKey =
                new DeviceCompositeKey(DeviceSetting.MODE.getFormattedSettingName(), beaconId);
        DeviceCompositeKey activeModesCompositeKey =
                new DeviceCompositeKey(
                        DeviceSetting.ACTIVE_MODES.getFormattedSettingName(), beaconId);

        OffsetDateTime cachedSystemModeTimestamp =
                getCachedTimestamp(cachedSettingsMapSystemMode.get(systemModeCompositeKey));
        OffsetDateTime cachedActiveModesTimestamp =
                getCachedTimestamp(cachedSettingsMapActiveSystemModes.get(activeModesCompositeKey));

        List<SystemMode> resultActiveModes =
                processCachedValue(
                        cachedSettingsMapActiveSystemModes.get(activeModesCompositeKey),
                        availableModesTimestampUtc,
                        systemId,
                        RequestType.INSTANT_ACTIVE_SYSTEM_MODE_PATCH,
                        activeModesCompositeKey,
                        () ->
                                (List<SystemMode>)
                                        cachedSettingsMapActiveSystemModes
                                                .get(activeModesCompositeKey)
                                                .getValue(),
                        () -> activeModes);

        SystemMode resultMode =
                processCachedValue(
                        cachedSettingsMapSystemMode.get(systemModeCompositeKey),
                        currentSysMode.getTimestampUtc(),
                        systemId,
                        RequestType.INSTANT_SYSTEM_MODE_PATCH,
                        systemModeCompositeKey,
                        () ->
                                (SystemMode)
                                        cachedSettingsMapSystemMode
                                                .get(systemModeCompositeKey)
                                                .getValue(),
                        () ->
                                (currentSysMode.getSysMode() != null)
                                        ? SystemMode.fromEssDpSystemMode(
                                                currentSysMode.getSysMode())
                                        : null);

        OffsetDateTime updatedTimestampUtc =
                Collections.max(
                        Arrays.asList(
                                currentSysMode.getTimestampUtc(),
                                availableModesTimestampUtc,
                                cachedSystemModeTimestamp,
                                cachedActiveModesTimestamp));

        return SystemModeGetResponse.builder()
                .systemId(systemId)
                .updatedTimestampUtc(updatedTimestampUtc)
                .mode(resultMode)
                .activeModes(resultActiveModes)
                .availableModes(SystemMode.getAllModesExceptExcluded())
                .build();
    }

    private OffsetDateTime getCachedTimestamp(
            ParameterTimestampValueMap.ParameterTimestampValue cachedValue) {
        return Optional.ofNullable(cachedValue)
                .map(ParameterTimestampValueMap.ParameterTimestampValue::getTimestamp)
                .orElse(OffsetDateTime.MIN);
    }

    private Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
            getSettingsMap(UUID systemId, RequestType requestType) {
        return Optional.ofNullable(getSettings(systemId, requestType))
                .map(ParameterTimestampValueMap::getEntryMap)
                .orElse(Collections.emptyMap());
    }

    private <T> T processCachedValue(
            ParameterTimestampValueMap.ParameterTimestampValue cachedValue,
            OffsetDateTime timestamp,
            UUID systemId,
            RequestType requestType,
            DeviceCompositeKey compositeKey,
            Supplier<T> cachedSupplier,
            Supplier<T> defaultSupplier) {

        return Optional.ofNullable(cachedValue)
                .filter(value -> cachedTimestampIsMoreRecent(timestamp, value))
                .map(value -> cachedSupplier.get())
                .orElseGet(
                        () -> {
                            if (cachedValue != null) {
                                evictHashKey(systemId, requestType, compositeKey);
                            }
                            return defaultSupplier.get();
                        });
    }

    public DeviceSettingsResponse updateDeviceSettingsResponseWithCachedSettings(
            DeviceSettingsResponse deviceSettingsResponse, UUID systemId, RequestType requestType) {
        Optional<ParameterTimestampValueMap<T>> cachedSettingsMap =
                Optional.ofNullable(getSettings(systemId, requestType));
        if (cachedSettingsMap.isEmpty()) {
            return deviceSettingsResponse;
        }
        Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> cachedSettings =
                cachedSettingsMap.get().getEntryMap();
        List<DeviceSettings> updatedDeviceSettings =
                deviceSettingsResponse.devices().stream()
                        .map(
                                deviceSettings ->
                                        getUpdatedDeviceSettings(
                                                deviceSettings,
                                                cachedSettings,
                                                systemId,
                                                requestType))
                        .toList();
        return new DeviceSettingsResponse(
                deviceSettingsResponse.hostDeviceId(), updatedDeviceSettings);
    }

    private DeviceSettings getUpdatedDeviceSettings(
            DeviceSettings deviceSettings,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
                    cachedSettings,
            UUID systemId,
            RequestType requestType) {
        List<DeviceSettings.DeviceSettingsMetadata> updatedMetadataList =
                deviceSettings.settings().stream()
                        .map(
                                existingSetting -> {
                                    DeviceCompositeKey deviceCompositeKey =
                                            new DeviceCompositeKey(
                                                    existingSetting
                                                            .name()
                                                            .getFormattedSettingName(),
                                                    deviceSettings.deviceId());
                                    Optional<ParameterTimestampValueMap.ParameterTimestampValue>
                                            cachedSetting =
                                                    Optional.ofNullable(
                                                            cachedSettings.get(deviceCompositeKey));
                                    if (cachedSetting.isPresent()) {
                                        if (cachedTimestampIsMoreRecent(
                                                existingSetting.updatedTimestampUtc(),
                                                cachedSetting.get())) {
                                            return updatedDeviceSetting(
                                                    existingSetting, cachedSetting.get());
                                        } else {
                                            evictHashKey(systemId, requestType, deviceCompositeKey);
                                        }
                                    }
                                    return existingSetting;
                                })
                        .toList();
        return new DeviceSettings(
                deviceSettings.deviceId(), deviceSettings.deviceType(), updatedMetadataList);
    }

    private boolean cachedTimestampIsMoreRecent(
            OffsetDateTime updatedTimestampUtc,
            ParameterTimestampValueMap.ParameterTimestampValue cachedSetting) {
        return cachedSetting.getTimestamp().isAfter(updatedTimestampUtc);
    }

    private DeviceSettings.DeviceSettingsMetadata updatedDeviceSetting(
            DeviceSettings.DeviceSettingsMetadata existingSetting,
            ParameterTimestampValueMap.ParameterTimestampValue cachedSetting) {
        OffsetDateTime cachedUpdatedTime = cachedSetting.getTimestamp();
        Double cachedValue = (double) cachedSetting.getValue();
        return DeviceSettings.DeviceSettingsMetadata.builder()
                .name(existingSetting.name())
                .label(existingSetting.label())
                .value(cachedValue)
                .constraints(existingSetting.constraints())
                .type(existingSetting.type())
                .unit(existingSetting.unit())
                .description(existingSetting.description())
                .updatedTimestampUtc(cachedUpdatedTime)
                .build();
    }

    private String getKey(UUID systemId, RequestType requestType) {
        return String.format("%s:%s:%s", systemId, requestType, KEY_POSTFIX);
    }
}
