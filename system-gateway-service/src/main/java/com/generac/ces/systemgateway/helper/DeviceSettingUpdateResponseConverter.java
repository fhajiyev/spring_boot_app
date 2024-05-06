package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.model.BaseBatterySetting;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DeviceSettingUpdateResponseConverter {

    private DeviceSettingUpdateResponseConverter() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static DeviceSettingUpdateResponse toInstance(OdinResponse odinResponse, UUID systemId) {
        OdinResponse.EventSettingsChanged settingsChanged =
                (OdinResponse.EventSettingsChanged) odinResponse.getEventDetails();
        return DeviceSettingUpdateResponse.builder()
                .updateId(UUID.fromString(odinResponse.getId()))
                .systemId(systemId)
                .devices(toDevices(settingsChanged))
                .updatedTimestampUtc(settingsChanged.getEventSettingsChangedTime())
                .build();
    }

    private static List<DeviceSettingUpdateResponse.Device> toDevices(
            OdinResponse.EventSettingsChanged responseDto) {
        List<BatterySettingResponseDto.OdinBatterySetting> responseSettings =
                responseDto.getBattery().getSetting();
        if (responseSettings == null) {
            return Collections.emptyList();
        }
        return responseSettings.stream()
                .map(
                        setting ->
                                DeviceSettingUpdateResponse.Device.builder()
                                        .deviceId(setting.getRcpn())
                                        .settings(
                                                mapBatterySettingToDeviceSettingChanges(
                                                        setting.getBatterySetting()))
                                        .build())
                .toList();
    }

    private static List<DeviceSettingUpdateResponse.Device.DeviceSettingChange>
            mapBatterySettingToDeviceSettingChanges(BaseBatterySetting batterySetting) {
        List<DeviceSettingUpdateResponse.Device.DeviceSettingChange> changes = new ArrayList<>();

        addSettingIfNotNull(changes, DeviceSetting.SOC_MAX, batterySetting.getSocMax());
        addSettingIfNotNull(changes, DeviceSetting.SOC_MIN, batterySetting.getSocMin());
        addSettingIfNotNull(changes, DeviceSetting.A_CHA_MAX, batterySetting.getAChaMax());
        addSettingIfNotNull(changes, DeviceSetting.A_DISCHA_MAX, batterySetting.getADisChaMax());
        addSettingIfNotNull(changes, DeviceSetting.SOC_RSV_MAX, batterySetting.getSocRsvMax());
        addSettingIfNotNull(changes, DeviceSetting.SOC_RSV_MIN, batterySetting.getSocRsvMin());

        return changes;
    }

    private static void addSettingIfNotNull(
            List<DeviceSettingUpdateResponse.Device.DeviceSettingChange> changes,
            DeviceSetting setting,
            Double value) {
        if (value != null) {
            changes.add(
                    DeviceSettingUpdateResponse.Device.DeviceSettingChange.builder()
                            .name(setting)
                            .value(value)
                            .build());
        }
    }
}
