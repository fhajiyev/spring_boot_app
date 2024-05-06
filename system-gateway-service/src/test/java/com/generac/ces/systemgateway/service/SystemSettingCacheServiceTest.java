package com.generac.ces.systemgateway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.essdataprovider.model.ModeResponseDto;
import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.helper.DeviceCompositeKey;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.device.DeviceSettings;
import com.generac.ces.systemgateway.model.device.DeviceSettingsResponse;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.system.SystemModeGetResponse;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ReflectionUtils;

class SystemSettingCacheServiceTest {
    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, ParameterTimestampValueMap<SystemMode>> hashOperations;

    @InjectMocks private SystemSettingCacheService<SystemMode> systemSettingCacheService;

    private static int systemSettingsTtlSeconds;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        systemSettingCacheService.hashOperations = hashOperations;

        // Set the value for requestRateLimitSeconds using ReflectionTestUtils
        Field field =
                ReflectionUtils.findField(
                        SystemSettingCacheService.class, "requestRateLimitSeconds");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, systemSettingCacheService, 20);

        Field ttlSecondsField =
                SystemSettingCacheService.class.getDeclaredField("systemSettingsTtlSeconds");
        ttlSecondsField.setAccessible(true);
        systemSettingsTtlSeconds = (int) ttlSecondsField.get(systemSettingCacheService);
    }

    @Test
    void saveSettings() {
        UUID systemId = UUID.randomUUID();
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        ParameterTimestampValueMap<SystemMode> settings = new ParameterTimestampValueMap<>();
        String key = String.format("%s:%s:%s", systemId, requestType, "SystemSettings");

        systemSettingCacheService.saveSettings(systemId, requestType, settings);

        verify(hashOperations).put(key, requestType.toString(), settings);
        verify(redisTemplate).expire(key, systemSettingsTtlSeconds, TimeUnit.SECONDS);
    }

    @Test
    void getSettings() {
        UUID systemId = UUID.randomUUID();
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        String key = String.format("%s:%s:%s", systemId, requestType, "SystemSettings");

        ParameterTimestampValueMap<SystemMode> expectedSettings =
                new ParameterTimestampValueMap<>();
        when(hashOperations.get(key, requestType.toString())).thenReturn(expectedSettings);

        ParameterTimestampValueMap<SystemMode> actualSettings =
                systemSettingCacheService.getSettings(systemId, requestType);

        verify(hashOperations).get(key, requestType.toString());
        assertEquals(expectedSettings, actualSettings);
    }

    @Test
    void evictHashKey() {
        UUID systemId = UUID.randomUUID();
        String rcpn = "000100121057";
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        String key = String.format("%s:%s:%s", systemId, requestType, "SystemSettings");
        DeviceCompositeKey hashKey = new DeviceCompositeKey("mode", rcpn);

        systemSettingCacheService.evictHashKey(systemId, requestType, hashKey);

        verify(hashOperations).delete(key, hashKey);
    }

    @Test
    void validateForRateLimitBelowLimit() {
        UUID systemId = UUID.randomUUID();
        String rcpn = "000100121057";
        RequestType requestType = RequestType.INSTANT_BATTERY_SETTINGS_PATCH;
        List<DeviceCompositeKey> cacheKeyList = List.of(new DeviceCompositeKey("mode", rcpn));

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                cacheKeyList.get(0), "SELL", OffsetDateTime.now().minusSeconds(30));

        when(hashOperations.get(any(), any())).thenReturn(cache);

        assertDoesNotThrow(
                () ->
                        systemSettingCacheService.throwOnRateLimit(
                                systemId, requestType, cacheKeyList));
    }

    @Test
    void validateForRateLimitAboveLimit() {
        UUID systemId = UUID.randomUUID();
        String rcpn = "000100121057";
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        List<DeviceCompositeKey> cacheKeyList = List.of(new DeviceCompositeKey("mode", rcpn));

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                cacheKeyList.get(0), "SELL", OffsetDateTime.now().minusSeconds(10));

        when(hashOperations.get(any(), any())).thenReturn(cache);

        TooManyRequestsException exception =
                assertThrows(
                        TooManyRequestsException.class,
                        () ->
                                systemSettingCacheService.throwOnRateLimit(
                                        systemId, requestType, cacheKeyList));

        assertEquals(
                "Request limit has been reached for system id: "
                        + systemId
                        + " and request type: INSTANT_SYSTEM_MODE_PATCH. Please try again in 10"
                        + " SECONDS.",
                exception.getMessage());
    }

    @Test
    void validateForRateLimitDeviceSettingsBelowLimit() {
        UUID systemId = UUID.randomUUID();
        String rcpn = "000100084A5E";
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        List<DeviceCompositeKey> cacheKeyList = List.of(new DeviceCompositeKey("socMax", rcpn));

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                cacheKeyList.get(0), 100.0, OffsetDateTime.now().minusSeconds(30));

        when(hashOperations.get(any(), any())).thenReturn(cache);

        assertDoesNotThrow(
                () ->
                        systemSettingCacheService.throwOnRateLimit(
                                systemId, requestType, cacheKeyList));
    }

    @Test
    void validateForRateLimitDeviceSettingsAboveLimit() {
        UUID systemId = UUID.randomUUID();
        String rcpn = "000100084A5E";
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        List<DeviceCompositeKey> cacheKeyList = List.of(new DeviceCompositeKey("socMax", rcpn));

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                cacheKeyList.get(0), 100.0, OffsetDateTime.now().minusSeconds(10));

        when(hashOperations.get(any(), any())).thenReturn(cache);

        TooManyRequestsException exception =
                assertThrows(
                        TooManyRequestsException.class,
                        () ->
                                systemSettingCacheService.throwOnRateLimit(
                                        systemId, requestType, cacheKeyList));

        assertEquals(
                "Request limit has been reached for system id: "
                        + systemId
                        + " and request type: INSTANT_SYSTEM_MODE_PATCH. Please try again in"
                        + " 10 SECONDS.",
                exception.getMessage());
    }

    @Test
    void validateForRateLimitDeviceSettingsOneSettingAboveDurationLimit() {
        UUID systemId = UUID.randomUUID();
        String rcpn = "000100084A5E";
        RequestType requestType = RequestType.INSTANT_SYSTEM_MODE_PATCH;
        List<DeviceCompositeKey> cacheKeyList =
                List.of(
                        new DeviceCompositeKey("socMax", rcpn),
                        new DeviceCompositeKey("socMin", rcpn));

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                cacheKeyList.get(0), 1.0, OffsetDateTime.now().minusSeconds(20));
        cache.addParameterTimestampValue(
                cacheKeyList.get(1), 100.0, OffsetDateTime.now().minusSeconds(10));

        when(hashOperations.get(any(), any())).thenReturn(cache);

        assertDoesNotThrow(
                () ->
                        systemSettingCacheService.throwOnRateLimit(
                                systemId, requestType, cacheKeyList));
    }

    @Test
    void test_updateDeviceSettingsResponseWithCachedSettings_returnsInputResponseIfCacheIsEmpty() {
        String rcpn = "000100084A5E";
        String hostId = "000100075477";
        UUID systemId = UUID.fromString("1-2-3-4-5");
        OffsetDateTime updatedTime = OffsetDateTime.now();
        DeviceSettingsResponse input =
                DeviceSettingsResponse.builder()
                        .hostDeviceId(hostId)
                        .devices(
                                List.of(
                                        DeviceSettings.builder()
                                                .deviceId(rcpn)
                                                .settings(
                                                        List.of(
                                                                DeviceSettings
                                                                        .DeviceSettingsMetadata
                                                                        .builder()
                                                                        .updatedTimestampUtc(
                                                                                updatedTime)
                                                                        .name(DeviceSetting.SOC_MAX)
                                                                        .value(100.0)
                                                                        .build()))
                                                .build()))
                        .build();

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        when(hashOperations.get(any(), any())).thenReturn(cache);

        DeviceSettingsResponse actual =
                systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        input, systemId, RequestType.INSTANT_BATTERY_SETTINGS_PATCH);

        Assertions.assertEquals(input, actual);
    }

    @Test
    void
            test_updateDeviceSettingsResponseWithCachedSettings_returnsInputResponseIfCachedValueTimeIsBeforeResponseTime() {
        String rcpn = "000100084A5E";
        String hostId = "000100075477";
        UUID systemId = UUID.fromString("1-2-3-4-5");
        OffsetDateTime updatedTime = OffsetDateTime.now();
        DeviceSettingsResponse input =
                DeviceSettingsResponse.builder()
                        .hostDeviceId(hostId)
                        .devices(
                                List.of(
                                        DeviceSettings.builder()
                                                .deviceId(rcpn)
                                                .deviceType(DeviceTypeOuterClass.DeviceType.BATTERY)
                                                .settings(
                                                        List.of(
                                                                DeviceSettings
                                                                        .DeviceSettingsMetadata
                                                                        .builder()
                                                                        .updatedTimestampUtc(
                                                                                updatedTime)
                                                                        .name(DeviceSetting.SOC_MAX)
                                                                        .value(100.0)
                                                                        .build()))
                                                .build()))
                        .build();

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                new DeviceCompositeKey("socMax", rcpn), 99.0, updatedTime.minusSeconds(20));
        when(hashOperations.get(any(), any())).thenReturn(cache);

        DeviceSettingsResponse actual =
                systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        input, systemId, RequestType.INSTANT_BATTERY_SETTINGS_PATCH);

        Assertions.assertEquals(input, actual);
    }

    @Test
    void
            test_updateDeviceSettingsResponseWithCachedSettings_returnsCachedResponseIfCachedValueTimeIsAfterResponseTime() {
        String rcpn = "000100084A5E";
        String hostId = "000100075477";
        UUID systemId = UUID.fromString("1-2-3-4-5");
        OffsetDateTime updatedTime = OffsetDateTime.now();
        DeviceSettingsResponse input =
                DeviceSettingsResponse.builder()
                        .hostDeviceId(hostId)
                        .devices(
                                List.of(
                                        DeviceSettings.builder()
                                                .deviceId(rcpn)
                                                .settings(
                                                        List.of(
                                                                DeviceSettings
                                                                        .DeviceSettingsMetadata
                                                                        .builder()
                                                                        .updatedTimestampUtc(
                                                                                updatedTime)
                                                                        .name(DeviceSetting.SOC_MAX)
                                                                        .value(100.0)
                                                                        .build()))
                                                .build()))
                        .build();

        ParameterTimestampValueMap<SystemMode> cache = new ParameterTimestampValueMap<>();
        cache.addParameterTimestampValue(
                new DeviceCompositeKey("socMax", rcpn), 99.0, updatedTime.plusSeconds(20));

        when(hashOperations.get(any(), any())).thenReturn(cache);

        DeviceSettingsResponse expected =
                DeviceSettingsResponse.builder()
                        .hostDeviceId(hostId)
                        .devices(
                                List.of(
                                        DeviceSettings.builder()
                                                .deviceId(rcpn)
                                                .settings(
                                                        List.of(
                                                                DeviceSettings
                                                                        .DeviceSettingsMetadata
                                                                        .builder()
                                                                        .updatedTimestampUtc(
                                                                                updatedTime
                                                                                        .plusSeconds(
                                                                                                20))
                                                                        .name(DeviceSetting.SOC_MAX)
                                                                        .value(99.0)
                                                                        .build()))
                                                .build()))
                        .build();

        DeviceSettingsResponse actual =
                systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        input, systemId, RequestType.INSTANT_BATTERY_SETTINGS_PATCH);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testUpdateSystemModeWithCachedSettings_NoCachedSettings() {
        // Arrange
        ModeResponseDto currentSysMode = new ModeResponseDto();
        currentSysMode.setSysMode(SysModes.Sell);
        currentSysMode.setTimestampUtc(OffsetDateTime.now());
        List<SystemMode> activeModes = Arrays.asList(SystemMode.SELL);
        UUID systemId = UUID.randomUUID();
        String beaconId = "dummyBeaconId";
        OffsetDateTime availableModesTimestampUtc = OffsetDateTime.now();

        when(hashOperations.get(any(), any())).thenReturn(null);

        // Act
        SystemModeGetResponse result =
                systemSettingCacheService.updateSystemModeWithCachedSettings(
                        currentSysMode,
                        activeModes,
                        systemId,
                        beaconId,
                        availableModesTimestampUtc);

        // Assert
        assertNull(result);
        verify(hashOperations, times(2)).get(any(), any());
        verifyNoMoreInteractions(hashOperations);
    }

    @Test
    void
            testUpdateSystemModeWithCachedSettings_CacheHit_SystemModeOldTimestampInCache_ActiveModesUpdatedLast() {
        // Arrange
        ModeResponseDto currentSysMode = new ModeResponseDto();
        currentSysMode.setSysMode(SysModes.Sell);
        currentSysMode.setTimestampUtc(OffsetDateTime.now());
        List<SystemMode> activeModes = Arrays.asList(SystemMode.PRIORITY_BACKUP);
        UUID systemId = UUID.randomUUID();
        String beaconId = "dummyBeaconId";
        OffsetDateTime availableModesTimestampUtc = OffsetDateTime.now().plusMinutes(5);

        OffsetDateTime oldTimestamp = OffsetDateTime.now().minusHours(1);
        ParameterTimestampValueMap<SystemMode> systemModeSettings =
                new ParameterTimestampValueMap<>();
        systemModeSettings.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.MODE.getFormattedSettingName(), beaconId),
                SystemMode.SELF_SUPPLY,
                oldTimestamp);

        when(hashOperations.get(
                        String.format(
                                "%s:%s:%s",
                                systemId, RequestType.INSTANT_SYSTEM_MODE_PATCH, "SystemSettings"),
                        RequestType.INSTANT_SYSTEM_MODE_PATCH.toString()))
                .thenReturn(systemModeSettings);

        // Act
        SystemModeGetResponse result =
                systemSettingCacheService.updateSystemModeWithCachedSettings(
                        currentSysMode,
                        activeModes,
                        systemId,
                        beaconId,
                        availableModesTimestampUtc);

        // Assert
        assertNotNull(result);
        Assertions.assertEquals(availableModesTimestampUtc, result.updatedTimestampUtc());
        Assertions.assertEquals(SystemMode.SELL, result.mode());
        Assertions.assertEquals(activeModes, result.activeModes());
        verify(hashOperations, times(2)).get(any(), any());
        verify(hashOperations, times(1)).delete(any(), any());
        verifyNoMoreInteractions(hashOperations);
    }

    @Test
    void
            testUpdateSystemModeWithCachedSettings_CacheHit_SystemModeOldTimestampInCache_SystemModeUpdatedLast() {
        // Arrange
        ModeResponseDto currentSysMode = new ModeResponseDto();
        currentSysMode.setSysMode(SysModes.Sell);
        OffsetDateTime systemModeTimestamp = OffsetDateTime.now().plusMinutes(5);
        currentSysMode.setTimestampUtc(systemModeTimestamp);
        List<SystemMode> activeModes = Arrays.asList(SystemMode.PRIORITY_BACKUP);
        UUID systemId = UUID.randomUUID();
        String beaconId = "dummyBeaconId";
        OffsetDateTime availableModesTimestampUtc = OffsetDateTime.now();

        OffsetDateTime oldTimestamp = OffsetDateTime.now().minusHours(1);
        ParameterTimestampValueMap<SystemMode> systemModeSettings =
                new ParameterTimestampValueMap<>();
        systemModeSettings.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.MODE.getFormattedSettingName(), beaconId),
                SystemMode.SELL,
                oldTimestamp);

        when(hashOperations.get(
                        String.format(
                                "%s:%s:%s",
                                systemId, RequestType.INSTANT_SYSTEM_MODE_PATCH, "SystemSettings"),
                        RequestType.INSTANT_SYSTEM_MODE_PATCH.toString()))
                .thenReturn(systemModeSettings);

        // Act
        SystemModeGetResponse result =
                systemSettingCacheService.updateSystemModeWithCachedSettings(
                        currentSysMode,
                        activeModes,
                        systemId,
                        beaconId,
                        availableModesTimestampUtc);

        // Assert
        assertNotNull(result);
        Assertions.assertEquals(systemModeTimestamp, result.updatedTimestampUtc());
        Assertions.assertEquals(SystemMode.SELL, result.mode());
        Assertions.assertEquals(activeModes, result.activeModes());
        verify(hashOperations, times(2)).get(any(), any());
        verify(hashOperations, times(1)).delete(any(), any());
        verifyNoMoreInteractions(hashOperations);
    }

    @Test
    void testUpdateSystemModeWithCachedSettings_CacheHit_SystemModeNewerTimestampInCache() {
        // Arrange
        ModeResponseDto currentSysMode = new ModeResponseDto();
        currentSysMode.setSysMode(SysModes.Sell);
        currentSysMode.setTimestampUtc(OffsetDateTime.now());
        List<SystemMode> activeModes = Arrays.asList(SystemMode.REMOTE_ARBITRAGE);
        UUID systemId = UUID.randomUUID();
        String beaconId = "dummyBeaconId";
        OffsetDateTime availableModesTimestampUtc = OffsetDateTime.now().plusMinutes(5);

        OffsetDateTime newerTimestamp = OffsetDateTime.now().plusHours(1);
        ParameterTimestampValueMap<SystemMode> systemModeSettings =
                new ParameterTimestampValueMap<>();
        systemModeSettings.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.MODE.getFormattedSettingName(), beaconId),
                SystemMode.SELF_SUPPLY,
                newerTimestamp);

        // Stub the hashOperations.get to return the cached value
        when(hashOperations.get(
                        String.format(
                                "%s:%s:%s",
                                systemId, RequestType.INSTANT_SYSTEM_MODE_PATCH, "SystemSettings"),
                        RequestType.INSTANT_SYSTEM_MODE_PATCH.toString()))
                .thenReturn(systemModeSettings);

        // Act
        SystemModeGetResponse result =
                systemSettingCacheService.updateSystemModeWithCachedSettings(
                        currentSysMode,
                        activeModes,
                        systemId,
                        beaconId,
                        availableModesTimestampUtc);

        // Assert
        assertNotNull(result);
        verify(hashOperations, times(2)).get(any(), any());
        Assertions.assertEquals(newerTimestamp, result.updatedTimestampUtc());
        Assertions.assertEquals(SystemMode.SELF_SUPPLY, result.mode());
        Assertions.assertEquals(activeModes, result.activeModes());
        verifyNoMoreInteractions(hashOperations);
        verify(hashOperations, never()).delete(any(), any());
    }

    @Test
    void testUpdateSystemModeWithCachedSettings_CacheHit_ActiveModes_Valid() {
        // Arrange
        ModeResponseDto currentSysMode = new ModeResponseDto();
        currentSysMode.setSysMode(SysModes.Sell);
        currentSysMode.setTimestampUtc(OffsetDateTime.now());
        List<SystemMode> activeModes = Arrays.asList(SystemMode.PRIORITY_BACKUP);
        UUID systemId = UUID.randomUUID();
        String beaconId = "dummyBeaconId";
        OffsetDateTime availableModesTimestampUtc = OffsetDateTime.now();

        OffsetDateTime validTimestamp = OffsetDateTime.now().plusMinutes(30);
        ParameterTimestampValueMap<SystemMode> activeModesSettings =
                new ParameterTimestampValueMap<>();
        activeModesSettings.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.ACTIVE_MODES.getFormattedSettingName(), beaconId),
                Arrays.asList(SystemMode.SELL),
                validTimestamp);

        when(hashOperations.get(
                        String.format(
                                "%s:%s:%s",
                                systemId,
                                RequestType.INSTANT_ACTIVE_SYSTEM_MODE_PATCH,
                                "SystemSettings"),
                        RequestType.INSTANT_ACTIVE_SYSTEM_MODE_PATCH.toString()))
                .thenReturn(activeModesSettings);

        // Act
        SystemModeGetResponse result =
                systemSettingCacheService.updateSystemModeWithCachedSettings(
                        currentSysMode,
                        activeModes,
                        systemId,
                        beaconId,
                        availableModesTimestampUtc);

        // Assert
        assertNotNull(result);
        assertEquals(validTimestamp, result.updatedTimestampUtc());
        assertEquals(SystemMode.SELL, result.mode());
        assertEquals(Arrays.asList(SystemMode.SELL), result.activeModes());

        // Ensure cache was not evicted
        verify(hashOperations, times(2)).get(any(), any());
        verifyNoMoreInteractions(hashOperations);
    }
}
