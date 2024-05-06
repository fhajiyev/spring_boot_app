package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.control.subcontrol.PvLinkControl;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.EnablePvrss;
import com.generac.ces.systemgateway.model.enums.Islanding;
import com.generac.ces.systemgateway.model.enums.NumberOfSubstrings;
import com.generac.ces.systemgateway.model.enums.Valuable;
import com.google.protobuf.ProtocolMessageEnum;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PvlSettingsResponseConverter {

    private PvlSettingsResponseConverter() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static List<DeviceSettingUpdateResponse.Device.DeviceSettingChange> mapDevices(
            OdinResponse.EventSettingsChanged eventSettingsChanged) {
        return Stream.of(
                        mapDeviceSettingChange(
                                DeviceSetting.ENABLE_PVRSS,
                                eventSettingsChanged.getPvLink().getPvrssState()),
                        List.of(
                                new DeviceSettingUpdateResponse.Device.DeviceSettingChange(
                                        DeviceSetting.VIN_STARTUP,
                                        eventSettingsChanged.getPvLink().getMinimumInputVoltage())),
                        mapDeviceSettingChange(
                                DeviceSetting.NUM_STRING,
                                eventSettingsChanged.getPvLink().getNumberOfSubstrings()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static <T extends Enum<T> & ProtocolMessageEnum>
            List<DeviceSettingUpdateResponse.Device.DeviceSettingChange> mapDeviceSettingChange(
                    DeviceSetting settingEnum, T protoEnum) {
        return Optional.ofNullable(protoEnum)
                .map(
                        value -> {
                            double numericValue = getValueFromEnum(value, settingEnum);
                            return List.of(
                                    DeviceSettingUpdateResponse.Device.DeviceSettingChange.builder()
                                            .name(settingEnum)
                                            .value(numericValue)
                                            .build());
                        })
                .orElse(Collections.emptyList());
    }

    private static double getValueFromEnum(Enum<?> protoEnum, DeviceSetting settingEnum) {
        Enum<?> customEnum = mapToCustomEnum(protoEnum, settingEnum);
        return getValueFromCustomEnum(customEnum);
    }

    private static Enum<?> mapToCustomEnum(Enum<?> protoEnum, DeviceSetting settingEnum) {
        if (protoEnum instanceof PvLinkControl.PVLinkSetting.PVRSSState pvrssState
                && settingEnum == DeviceSetting.ENABLE_PVRSS) {
            return mapEnablePvrssEnum(pvrssState);
        } else if (protoEnum
                        instanceof PvLinkControl.PVLinkSetting.NumberOfSubstrings numberOfSubstrings
                && settingEnum == DeviceSetting.NUM_STRING) {
            return mapNumberOfSubstrings(numberOfSubstrings);
        }

        return protoEnum;
    }

    private static Enum<?> mapEnablePvrssEnum(PvLinkControl.PVLinkSetting.PVRSSState pvrssState) {
        return switch (pvrssState) {
            case PVRSS_STATE_OFF -> EnablePvrss.PVRSS_STATE_OFF;
            case PVRSS_STATE_ON -> EnablePvrss.PVRSS_STATE_ON;
            default -> EnablePvrss.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapNumberOfSubstrings(
            PvLinkControl.PVLinkSetting.NumberOfSubstrings numberOfSubstrings) {
        return switch (numberOfSubstrings) {
            case NUMBER_OF_SUBSTRINGS_UNSPECIFIED -> NumberOfSubstrings
                    .NUMBER_OF_SUBSTRINGS_UNSPECIFIED;
            case NUMBER_OF_SUBSTRINGS_ONE -> NumberOfSubstrings.NUMBER_OF_SUBSTRINGS_ONE;
            case NUMBER_OF_SUBSTRINGS_TWO -> NumberOfSubstrings.NUMBER_OF_SUBSTRINGS_TWO;
            default -> Islanding.UNRECOGNIZED;
        };
    }

    private static double getValueFromCustomEnum(Enum<?> customEnum) {
        if (customEnum instanceof Valuable) {
            return ((Valuable) customEnum).getValue();
        } else {
            throw new IllegalArgumentException("Enum must implement Valuable interface");
        }
    }
}
