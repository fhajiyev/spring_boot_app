package com.generac.ces.systemgateway.model.helpers;

import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class ValidDeviceSettingValidator
        implements ConstraintValidator<ValidDeviceSettingChange, DeviceSettingsUpdateRequest> {

    private List<DeviceSetting> allowedSettings;

    @Override
    public void initialize(ValidDeviceSettingChange constraintAnnotation) {
        allowedSettings = Arrays.asList(constraintAnnotation.allowedSettings());
    }

    @Override
    public boolean isValid(
            DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            ConstraintValidatorContext context) {
        List<DeviceSettingsUpdateRequest.DeviceSettingChange> deviceSettings =
                deviceSettingsUpdateRequest.settings();
        if (deviceSettings == null) {
            return false;
        }

        boolean isValid = true;
        for (DeviceSettingsUpdateRequest.DeviceSettingChange deviceSettingChange : deviceSettings) {
            if (!allowedSettings.contains(deviceSettingChange.name())) {
                isValid = false;
                break;
            }
            if (!isValidSocSetting(deviceSettingChange)) {
                isValid = false;
                break;
            }
        }

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(getInvalidSettings(deviceSettings))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean isValidSetting(DeviceSetting setting) {
        return allowedSettings.contains(setting);
    }

    private String getInvalidSettings(
            List<DeviceSettingsUpdateRequest.DeviceSettingChange> deviceSettings) {
        List<String> invalidSettingNames = new ArrayList<>();

        for (DeviceSettingsUpdateRequest.DeviceSettingChange deviceSettingChange : deviceSettings) {
            if (deviceSettingChange.name() == null) {
                return "Setting does not belong to the enum. "
                        + "Valid settings: "
                        + allowedSettings.stream()
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));
            }

            if (!isValidSetting(deviceSettingChange.name())) {
                invalidSettingNames.add(deviceSettingChange.name().toString());
            }

            if (!isValidSocSetting(deviceSettingChange)) {
                return deviceSettingChange.name() + " must be between 0 and 100.";
            }
        }

        String invalidSettingsString = String.join(", ", invalidSettingNames);

        return "Invalid settings: "
                + invalidSettingsString
                + ". Valid settings: "
                + allowedSettings.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private boolean isValidSocSetting(
            DeviceSettingsUpdateRequest.DeviceSettingChange deviceSettingChange) {
        if (deviceSettingChange.name().equals(DeviceSetting.SOC_MIN)
                || deviceSettingChange.name().equals(DeviceSetting.SOC_MAX)) {
            return deviceSettingChange.value() >= 0 && deviceSettingChange.value() <= 100;
        }
        return true;
    }
}
