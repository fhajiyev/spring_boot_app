package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.enums.CTCalibration;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.ExportLimitState;
import com.generac.ces.systemgateway.model.enums.ExportOverride;
import com.generac.ces.systemgateway.model.enums.GeneratorControlMode;
import com.generac.ces.systemgateway.model.enums.Islanding;
import com.generac.ces.systemgateway.model.enums.LoadShedding;
import com.generac.ces.systemgateway.model.enums.NumberOfTransferSwitches;
import com.generac.ces.systemgateway.model.enums.Valuable;
import com.google.protobuf.ProtocolMessageEnum;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class InverterSettingsResponseConverter {

    private InverterSettingsResponseConverter() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static List<DeviceSettingUpdateResponse.Device.DeviceSettingChange> mapDevices(
            OdinResponse.EventSettingsChanged eventSettingsChanged) {
        return Stream.of(
                        mapDeviceSettingChange(
                                DeviceSetting.LOAD_SHEDDING,
                                eventSettingsChanged.getInverter().getLoadShedding()),
                        mapDeviceSettingChange(
                                DeviceSetting.ISLANDING,
                                eventSettingsChanged.getInverter().getIslanding()),
                        mapDeviceSettingChange(
                                DeviceSetting.EXPORT_OVERRIDE,
                                eventSettingsChanged.getInverter().getExportOverride()),
                        mapDeviceSettingChange(
                                DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES,
                                eventSettingsChanged.getInverter().getNumberOfTransferSwitches()),
                        mapDeviceSettingChange(
                                DeviceSetting.CT_CALIBRATION,
                                eventSettingsChanged.getInverter().getCtCalibration()),
                        mapDeviceSettingChange(
                                DeviceSetting.GENERATOR_CONTROL_MODE,
                                eventSettingsChanged.getInverter().getGeneratorControlMode()),
                        mapDeviceSettingChange(
                                DeviceSetting.EXPORT_LIMITING,
                                eventSettingsChanged.getInverter().getExportLimitState()),
                        mapDeviceSettingChangeWithPair(
                                DeviceSetting.EXPORT_POWER_LIMIT,
                                eventSettingsChanged.getInverter().getExportLimit()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static List<DeviceSettingUpdateResponse.Device.DeviceSettingChange>
            mapDeviceSettingChangeWithPair(DeviceSetting settingEnum, Integer numericValue) {
        return Optional.ofNullable(settingEnum)
                .map(
                        setting ->
                                Optional.ofNullable(numericValue)
                                        .map(
                                                value ->
                                                        List.of(
                                                                DeviceSettingUpdateResponse.Device
                                                                        .DeviceSettingChange
                                                                        .builder()
                                                                        .name(setting)
                                                                        .value(
                                                                                Double.valueOf(
                                                                                        value))
                                                                        .build()))
                                        .orElse(Collections.emptyList()))
                .orElse(Collections.emptyList());
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
        if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.LoadShedding
                        loadSheddingEnum
                && settingEnum == DeviceSetting.LOAD_SHEDDING) {
            return mapLoadSheddingEnum(loadSheddingEnum);
        } else if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.Islanding
                        islandingEnum
                && settingEnum == DeviceSetting.ISLANDING) {
            return mapIslandingEnum(islandingEnum);
        } else if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.ExportOverride
                        exportOverrideEnum
                && settingEnum == DeviceSetting.EXPORT_OVERRIDE) {
            return mapExportOverrideEnum(exportOverrideEnum);
        } else if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.NumberOfTransferSwitches
                        transferSwitchesEnum
                && settingEnum == DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES) {
            return mapTransferSwitchesEnum(transferSwitchesEnum);
        } else if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.CTCalibration
                        ctCalibrationEnum
                && settingEnum == DeviceSetting.CT_CALIBRATION) {
            return mapCTCalibrationEnum(ctCalibrationEnum);
        } else if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode
                        generatorControlModeEnum
                && settingEnum == DeviceSetting.GENERATOR_CONTROL_MODE) {
            return mapGenerateControlModeEnum(generatorControlModeEnum);
        } else if (protoEnum
                        instanceof
                        InverterSettingControlOuterClass.InverterSetting.ExportLimitState
                        exportLimitStateEnum
                && settingEnum == DeviceSetting.EXPORT_LIMITING) {
            return mapExportLimitStateEnum(exportLimitStateEnum);
        }

        return protoEnum;
    }

    private static Enum<?> mapLoadSheddingEnum(
            InverterSettingControlOuterClass.InverterSetting.LoadShedding loadShedding) {
        return switch (loadShedding) {
            case LOAD_SHEDDING_DISABLED -> LoadShedding.LOAD_SHEDDING_DISABLED;
            case LOAD_SHEDDING_SMM_ONLY -> LoadShedding.LOAD_SHEDDING_SMM_ONLY;
            case LOAD_SHEDDING_ATS_AND_SMM -> LoadShedding.LOAD_SHEDDING_ATS_AND_SMM;
            default -> LoadShedding.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapIslandingEnum(
            InverterSettingControlOuterClass.InverterSetting.Islanding islandingEnum) {
        return switch (islandingEnum) {
            case ISLANDING_DISABLED -> Islanding.ISLANDING_DISABLED;
            case ISLANDING_ENABLED -> Islanding.ISLANDING_ENABLED;
            default -> Islanding.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapExportOverrideEnum(
            InverterSettingControlOuterClass.InverterSetting.ExportOverride exportOverrideEnum) {
        return switch (exportOverrideEnum) {
            case EXPORT_OVERRIDE_DISABLED -> ExportOverride.EXPORT_OVERRIDE_DISABLED;
            case EXPORT_OVERRIDE_ENABLED -> ExportOverride.EXPORT_OVERRIDE_ENABLED;
            default -> ExportOverride.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapTransferSwitchesEnum(
            InverterSettingControlOuterClass.InverterSetting.NumberOfTransferSwitches
                    transferSwitchesEnum) {
        return switch (transferSwitchesEnum) {
            case NUMBER_OF_TRANSFER_SWITCHES_ZERO -> NumberOfTransferSwitches
                    .NUMBER_OF_TRANSFER_SWITCHES_ZERO;
            case NUMBER_OF_TRANSFER_SWITCHES_ONE -> NumberOfTransferSwitches
                    .NUMBER_OF_TRANSFER_SWITCHES_ONE;
            case NUMBER_OF_TRANSFER_SWITCHES_TWO -> NumberOfTransferSwitches
                    .NUMBER_OF_TRANSFER_SWITCHES_TWO;
            default -> NumberOfTransferSwitches.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapCTCalibrationEnum(
            InverterSettingControlOuterClass.InverterSetting.CTCalibration ctCalibrationEnum) {
        return switch (ctCalibrationEnum) {
            case CT_CALIBRATION_AUTO -> CTCalibration.CT_CALIBRATION_AUTO;
            case CT_CALIBRATION_TRIGGER -> CTCalibration.CT_CALIBRATION_TRIGGER;
            default -> CTCalibration.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapGenerateControlModeEnum(
            InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode
                    generatorControlModeEnum) {
        return switch (generatorControlModeEnum) {
            case GENERATOR_CONTROL_MODE_SINGLE_TRANSFER -> GeneratorControlMode
                    .GENERATOR_CONTROL_MODE_SINGLE_TRANSFER;
            case GENERATOR_CONTROL_MODE_SOURCE_CYCLING -> GeneratorControlMode
                    .GENERATOR_CONTROL_MODE_SOURCE_CYCLING;
            case GENERATOR_CONTROL_MODE_ALWAYS_ON -> GeneratorControlMode
                    .GENERATOR_CONTROL_MODE_ALWAYS_ON;
            default -> GeneratorControlMode.UNRECOGNIZED;
        };
    }

    private static Enum<?> mapExportLimitStateEnum(
            InverterSettingControlOuterClass.InverterSetting.ExportLimitState
                    exportLimitStateEnum) {
        return switch (exportLimitStateEnum) {
            case EXPORT_LIMIT_STATE_DISABLED -> ExportLimitState.EXPORT_LIMIT_STATE_DISABLED;
            case EXPORT_LIMIT_STATE_ENABLED -> ExportLimitState.EXPORT_LIMIT_STATE_ENABLED;
            default -> ExportLimitState.UNRECOGNIZED;
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
