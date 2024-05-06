package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.helper.EnumHelper;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.CTCalibration;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.ExportLimitState;
import com.generac.ces.systemgateway.model.enums.ExportOverride;
import com.generac.ces.systemgateway.model.enums.GeneratorControlMode;
import com.generac.ces.systemgateway.model.enums.Islanding;
import com.generac.ces.systemgateway.model.enums.LoadShedding;
import com.generac.ces.systemgateway.model.enums.NumberOfTransferSwitches;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InverterSettingControlMessageRequest {
    ControlMessage controlMessage;

    @Data
    @Builder
    public static class ControlMessage extends OdinBaseControlMessage {
        private String rcpn;
        private DeviceState state;
        private Islanding islanding;
        private ExportOverride exportOverride;
        private NumberOfTransferSwitches numberOfTransferSwitches;
        private LoadShedding loadShedding;
        private CTCalibration ctCalibration;
        private GeneratorControlMode generatorControlMode;
        private ExportLimitState exportLimitState;
        private Double exportLimit;
    }

    public static InverterSettingControlMessageRequest toInstance(DeviceState state, String rcpn) {
        return InverterSettingControlMessageRequest.builder()
                .controlMessage(ControlMessage.builder().state(state).rcpn(rcpn).build())
                .build();
    }

    public static InverterSettingControlMessageRequest toInstance(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest) {
        ControlMessage.ControlMessageBuilder controlMessageBuilder = ControlMessage.builder();

        mapSettingEnum(
                        deviceSettingsUpdateRequest,
                        ExportOverride.class,
                        DeviceSetting.EXPORT_OVERRIDE)
                .ifPresent(controlMessageBuilder::exportOverride);

        mapSettingEnum(deviceSettingsUpdateRequest, Islanding.class, DeviceSetting.ISLANDING)
                .ifPresent(controlMessageBuilder::islanding);

        mapSetting(
                        deviceSettingsUpdateRequest,
                        NumberOfTransferSwitches.class,
                        DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES)
                .ifPresent(controlMessageBuilder::numberOfTransferSwitches);

        mapSetting(deviceSettingsUpdateRequest, LoadShedding.class, DeviceSetting.LOAD_SHEDDING)
                .ifPresent(controlMessageBuilder::loadShedding);

        mapSetting(deviceSettingsUpdateRequest, CTCalibration.class, DeviceSetting.CT_CALIBRATION)
                .ifPresent(controlMessageBuilder::ctCalibration);

        mapSetting(
                        deviceSettingsUpdateRequest,
                        GeneratorControlMode.class,
                        DeviceSetting.GENERATOR_CONTROL_MODE)
                .ifPresent(controlMessageBuilder::generatorControlMode);

        mapSetting(
                        deviceSettingsUpdateRequest,
                        ExportLimitState.class,
                        DeviceSetting.EXPORT_LIMITING)
                .ifPresent(controlMessageBuilder::exportLimitState);

        deviceSettingsUpdateRequest.settings().stream()
                .filter(change -> change.name() == DeviceSetting.EXPORT_POWER_LIMIT)
                .findFirst()
                .ifPresent(
                        exportLimitChange ->
                                controlMessageBuilder.exportLimit(exportLimitChange.value()));

        return InverterSettingControlMessageRequest.builder()
                .controlMessage(controlMessageBuilder.build())
                .build();
    }

    private static <T extends Enum<T>> Optional<T> mapSetting(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            Class<T> enumClass,
            DeviceSetting settingEnum) {
        return deviceSettingsUpdateRequest.settings().stream()
                .filter(setting -> setting.name() == settingEnum)
                .findFirst()
                .map(
                        setting -> {
                            int rawValue = (int) Math.round(setting.value());
                            T enumValue = EnumHelper.fromValueForValuable(enumClass, rawValue);

                            if (enumValue == LoadShedding.UNRECOGNIZED
                                    || enumValue == NumberOfTransferSwitches.UNRECOGNIZED) {
                                throw new BadRequestException(
                                        "Invalid value for "
                                                + enumClass.getSimpleName()
                                                + ": "
                                                + rawValue);
                            }

                            return enumValue;
                        });
    }

    private static <T extends Enum<T>> Optional<T> mapSettingEnum(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            Class<T> enumClass,
            DeviceSetting settingEnum) {
        return deviceSettingsUpdateRequest.settings().stream()
                .filter(setting -> setting.name() == settingEnum)
                .findFirst()
                .map(setting -> mapBooleanToEnum(enumClass, setting.value()));
    }

    private static <T extends Enum<T>> T mapBooleanToEnum(Class<T> enumClass, double rawValue) {
        if (rawValue == 0.0) {
            return EnumHelper.fromValueForValuable(enumClass, 0);
        } else if (rawValue == 1.0) {
            return EnumHelper.fromValueForValuable(enumClass, 1);
        } else {
            throw new BadRequestException(
                    "Invalid value for "
                            + enumClass.getSimpleName()
                            + ": "
                            + rawValue
                            + ". Value should be 0 or 1.");
        }
    }
}
