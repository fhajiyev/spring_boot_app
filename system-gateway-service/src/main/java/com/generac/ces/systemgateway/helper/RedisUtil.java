package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class RedisUtil {

    private RedisUtil() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static ParameterTimestampValueMap<SystemMode> generateSystemModeSetting(
            SystemMode systemMode, OffsetDateTime updatedTimestampUtc, String beaconRcpn) {
        ParameterTimestampValueMap<SystemMode> systemModeSettings =
                new ParameterTimestampValueMap<>();
        systemModeSettings.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.MODE.getFormattedSettingName(), beaconRcpn),
                systemMode,
                updatedTimestampUtc);
        return systemModeSettings;
    }

    public static ParameterTimestampValueMap<SystemMode> generateActiveModesSetting(
            List<SystemMode> systemModes, OffsetDateTime updatedTimestampUtc, String beaconRcpn) {
        ParameterTimestampValueMap<SystemMode> systemModeSettings =
                new ParameterTimestampValueMap<>();
        systemModeSettings.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.ACTIVE_MODES.getFormattedSettingName(), beaconRcpn),
                systemModes,
                updatedTimestampUtc);
        return systemModeSettings;
    }

    public static ParameterTimestampValueMap<DeviceSetting> generateDeviceSetting(
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
                    deviceSettingUpdated) {
        ParameterTimestampValueMap<DeviceSetting> deviceSettings =
                new ParameterTimestampValueMap<>();
        deviceSettingUpdated
                .keySet()
                .forEach(
                        deviceSettingKey ->
                                deviceSettings.addParameterTimestampValue(
                                        deviceSettingKey,
                                        deviceSettingUpdated.get(deviceSettingKey).getValue(),
                                        deviceSettingUpdated.get(deviceSettingKey).getTimestamp()));
        return deviceSettings;
    }
}
