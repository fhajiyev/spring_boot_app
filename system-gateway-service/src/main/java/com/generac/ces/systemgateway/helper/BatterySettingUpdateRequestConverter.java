package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.model.OdinBatterySettingRequest;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class BatterySettingUpdateRequestConverter {

    private BatterySettingUpdateRequestConverter() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static OdinBatterySettingRequest mapToOdinBatterySetting(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest, List<String> rcpns) {
        if (deviceSettingsUpdateRequest.settings() == null
                || deviceSettingsUpdateRequest.settings().isEmpty()) {
            throw new BadRequestException("Need to include at least one setting in the request.");
        }

        OdinBatterySettingRequest odinBatterySettingRequest = new OdinBatterySettingRequest();
        OdinBatterySettingRequest.ControlMessage controlMessage =
                new OdinBatterySettingRequest.ControlMessage();

        controlMessage.setControlMessageId(UUID.randomUUID().toString());
        controlMessage.setRcpns(rcpns);

        for (DeviceSettingsUpdateRequest.DeviceSettingChange deviceSettingChange :
                deviceSettingsUpdateRequest.settings()) {
            DeviceSetting deviceSetting = deviceSettingChange.name();
            Double value = deviceSettingChange.value();

            mapDeviceSettingToControlMessageField(deviceSetting, value, controlMessage);
        }

        odinBatterySettingRequest.setControlMessage(controlMessage);
        return odinBatterySettingRequest;
    }

    private static void mapDeviceSettingToControlMessageField(
            DeviceSetting deviceSetting,
            Double value,
            OdinBatterySettingRequest.ControlMessage controlMessage) {
        switch (deviceSetting) {
            case SOC_MAX -> controlMessage.setSocMax(value);
            case SOC_MIN -> controlMessage.setSocMin(value);
            case SOC_RSV_MAX -> controlMessage.setSocRsvMax(value);
            case SOC_RSV_MIN -> controlMessage.setSocRsvMin(value);
            case A_CHA_MAX -> controlMessage.setAChaMax(value);
            case A_DISCHA_MAX -> controlMessage.setADisChaMax(value);
            default -> log.error("Unsupported DeviceSetting: {}", deviceSetting);
        }
    }
}
