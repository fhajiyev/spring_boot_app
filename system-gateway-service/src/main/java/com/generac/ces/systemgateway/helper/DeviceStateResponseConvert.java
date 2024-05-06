package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.DeviceStateResponse;
import com.generac.ces.systemgateway.model.OdinResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DeviceStateResponseConvert {
    public static DeviceStateResponse toInstance(
            OdinResponse odinResponse, UUID systemId, DeviceTypeOuterClass.DeviceType deviceType) {
        OdinResponse.EventSettingsChanged settingsChanged =
                (OdinResponse.EventSettingsChanged) odinResponse.getEventDetails();
        return DeviceStateResponse.builder()
                .updateId(UUID.fromString(odinResponse.getId()))
                .systemId(systemId)
                .deviceId(getDeviceId(settingsChanged))
                .enable(getState(settingsChanged))
                .deviceType(deviceType)
                .eventSettingsChangedTime(settingsChanged.getEventSettingsChangedTime())
                .build();
    }

    private static String getDeviceId(OdinResponse.EventSettingsChanged responseDto) {
        if (responseDto.getInverter() != null) {
            return responseDto.getInverter().getRcpn();
        }

        List<BatterySettingResponseDto.OdinBatterySetting> responseSettings =
                (responseDto.getBattery() != null && responseDto.getBattery().getSetting() != null)
                        ? responseDto.getBattery().getSetting()
                        : null;
        return (responseSettings != null)
                ? responseSettings.stream()
                        .map(BatterySettingResponseDto.OdinBatterySetting::getRcpn)
                        .findFirst()
                        .orElse(null)
                : null;
    }

    private static Boolean getState(OdinResponse.EventSettingsChanged responseDto) {
        if (responseDto.getInverter() != null) {
            return responseDto.getInverter().getState().getStateBool();
        }

        List<BatterySettingResponseDto.OdinBatterySetting> responseSettings =
                (responseDto.getBattery().getSetting() != null)
                        ? responseDto.getBattery().getSetting()
                        : null;
        return (responseSettings != null)
                ? Objects.requireNonNull(
                                responseSettings.stream()
                                        .map(BatterySettingResponseDto.OdinBatterySetting::getState)
                                        .findFirst()
                                        .orElse(null))
                        .getStateBool()
                : null;
    }
}
