package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.helper.EnumHelper;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.EnablePvrss;
import com.generac.ces.systemgateway.model.enums.NumberOfSubstrings;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PvLinkSettingControlMessageRequest {
    ControlMessage controlMessage;

    @Data
    @Builder
    public static class ControlMessage extends OdinBaseControlMessage {
        private String rcpn;
        private DeviceState state;
        private EnablePvrss pvrssState;
        private Double minimumInputVoltage;
        private NumberOfSubstrings numberOfSubstrings;
    }

    public static PvLinkSettingControlMessageRequest toInstance(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest, String pvlRcpn) {
        ControlMessage.ControlMessageBuilder controlMessageBuilder = ControlMessage.builder();

        mapSettingEnum(deviceSettingsUpdateRequest, EnablePvrss.class, DeviceSetting.ENABLE_PVRSS)
                .ifPresent(controlMessageBuilder::pvrssState);

        mapSetting(deviceSettingsUpdateRequest, NumberOfSubstrings.class, DeviceSetting.NUM_STRING)
                .ifPresent(controlMessageBuilder::numberOfSubstrings);

        deviceSettingsUpdateRequest.settings().stream()
                .filter(change -> change.name() == DeviceSetting.VIN_STARTUP)
                .findFirst()
                .ifPresent(
                        vinStartup ->
                                controlMessageBuilder.minimumInputVoltage(vinStartup.value()));

        return PvLinkSettingControlMessageRequest.builder()
                .controlMessage(controlMessageBuilder.rcpn(pvlRcpn).build())
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

                            if (enumValue == EnablePvrss.UNRECOGNIZED
                                    || enumValue == NumberOfSubstrings.UNRECOGNIZED) {
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
