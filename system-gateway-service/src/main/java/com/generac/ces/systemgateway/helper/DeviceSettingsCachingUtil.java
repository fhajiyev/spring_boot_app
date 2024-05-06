package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.model.BaseBatterySetting;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.InverterSettingResponseDto;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.PvlSettingResponseDto;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.CTCalibration;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.EnablePvrss;
import com.generac.ces.systemgateway.model.enums.ExportLimitState;
import com.generac.ces.systemgateway.model.enums.ExportOverride;
import com.generac.ces.systemgateway.model.enums.GeneratorControlMode;
import com.generac.ces.systemgateway.model.enums.Islanding;
import com.generac.ces.systemgateway.model.enums.LoadShedding;
import com.generac.ces.systemgateway.model.enums.NumberOfSubstrings;
import com.generac.ces.systemgateway.model.enums.NumberOfTransferSwitches;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class DeviceSettingsCachingUtil {
    private DeviceSettingsCachingUtil() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static ParameterTimestampValueMap<DeviceSetting> getValueMapOfDeviceSettingsToBeCached(
            DeviceSettingsUpdateRequest request,
            OdinResponse response,
            ParameterTimestampValueMap<DeviceSetting> currentCacheContents,
            List<String> rcpns,
            RequestType requestType,
            DeviceTypeOuterClass.DeviceType deviceType) {
        List<DeviceCompositeKey> requestSettings =
                rcpns.stream()
                        .flatMap(
                                rcpn ->
                                        request.settings().stream()
                                                .map(
                                                        setting ->
                                                                new DeviceCompositeKey(
                                                                        setting.name()
                                                                                .getFormattedSettingName(),
                                                                        rcpn)))
                        .toList();
        OffsetDateTime updatedTimestamp =
                Optional.ofNullable((OdinResponse.EventSettingsChanged) response.getEventDetails())
                        .map(OdinResponse.EventSettingsChanged::getEventSettingsChangedTime)
                        .orElse(OffsetDateTime.now());

        Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result =
                constructValueMapResult(
                        requestType, response, requestSettings, updatedTimestamp, deviceType);
        if (currentCacheContents != null) {
            updateResultWithCachedValuesIfApplicable(result, currentCacheContents);
        }
        return RedisUtil.generateDeviceSetting(result);
    }

    private static Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
            constructValueMapResult(
                    RequestType requestType,
                    OdinResponse response,
                    List<DeviceCompositeKey> requestSettings,
                    OffsetDateTime updatedTimestamp,
                    DeviceTypeOuterClass.DeviceType deviceType) {
        Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result =
                new HashMap<>();
        switch (requestType) {
            case INSTANT_BATTERY_SETTINGS_PATCH -> handleBattery(
                    response, result, requestSettings, updatedTimestamp);
            case INSTANT_INVERTER_SETTINGS_PATCH -> handleInverter(
                    response, result, requestSettings, updatedTimestamp);
            case INSTANT_PVLINK_SETTINGS_PATCH -> handlePvLink(
                    response, result, requestSettings, updatedTimestamp);
            case INSTANT_DEVICE_STATE_PATCH -> {
                switch (deviceType) {
                    case BATTERY -> handleBatteryState(
                            response, result, requestSettings, updatedTimestamp);
                    case INVERTER -> handleInverterState(
                            response, result, requestSettings, updatedTimestamp);
                }
            }
        }
        return result;
    }

    private static void handleBattery(
            OdinResponse response,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        Optional.ofNullable((OdinResponse.EventSettingsChanged) response.getEventDetails())
                .map(OdinResponse.EventSettingsChanged::getBattery)
                .map(BatterySettingResponseDto::getSetting)
                .ifPresent(
                        settings -> {
                            List<BaseBatterySetting> responseChangedSettings =
                                    settings.stream()
                                            .map(
                                                    BatterySettingResponseDto.OdinBatterySetting
                                                            ::getBatterySetting)
                                            .toList();
                            buildBatteryResult(
                                    result,
                                    responseChangedSettings,
                                    requestSettings,
                                    updatedTimestamp);
                        });
    }

    private static void handleInverter(
            OdinResponse response,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        Optional.ofNullable((OdinResponse.EventSettingsChanged) response.getEventDetails())
                .map(OdinResponse.EventSettingsChanged::getInverter)
                .ifPresent(
                        responseChangedSettings -> {
                            buildInverterResult(
                                    result,
                                    responseChangedSettings,
                                    requestSettings,
                                    updatedTimestamp);
                        });
    }

    private static void handlePvLink(
            OdinResponse response,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        Optional.ofNullable((OdinResponse.EventSettingsChanged) response.getEventDetails())
                .map(OdinResponse.EventSettingsChanged::getPvLink)
                .ifPresent(
                        responseChangedSettings -> {
                            buildPvlResult(
                                    result,
                                    responseChangedSettings,
                                    requestSettings,
                                    updatedTimestamp);
                        });
    }

    private static void handleBatteryState(
            OdinResponse response,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        Optional.ofNullable((OdinResponse.EventSettingsChanged) response.getEventDetails())
                .map(OdinResponse.EventSettingsChanged::getBattery)
                .map(BatterySettingResponseDto::getSetting)
                .ifPresent(
                        settings ->
                                settings.forEach(
                                        setting -> {
                                            validateStateVal(
                                                    setting.getState().getStateVal(),
                                                    setting.getState(),
                                                    DeviceTypeOuterClass.DeviceType.INVERTER);
                                            boolean stateBool = setting.getState().getStateBool();
                                            buildStateResult(
                                                    requestSettings,
                                                    updatedTimestamp,
                                                    DeviceState.toCacheableDouble(stateBool),
                                                    result);
                                        }));
    }

    private static void handleInverterState(
            OdinResponse response,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        Optional.ofNullable((OdinResponse.EventSettingsChanged) response.getEventDetails())
                .map(OdinResponse.EventSettingsChanged::getInverter)
                .ifPresent(
                        setting -> {
                            validateStateVal(
                                    setting.getState().getStateVal(),
                                    setting.getState(),
                                    DeviceTypeOuterClass.DeviceType.INVERTER);
                            boolean stateBool = setting.getState().getStateBool();
                            buildStateResult(
                                    requestSettings,
                                    updatedTimestamp,
                                    DeviceState.toCacheableDouble(stateBool),
                                    result);
                        });
    }

    private static void validateStateVal(
            double stateVal, DeviceState state, DeviceTypeOuterClass.DeviceType deviceType) {
        if (stateVal == 0 || stateVal == -1) {
            throw new IllegalStateException("Invalid value for " + deviceType + " State: " + state);
        }
    }

    private static void buildStateResult(
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp,
            double stateVal,
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result) {
        for (DeviceCompositeKey deviceSetting : requestSettings) {
            ParameterTimestampValueMap.ParameterTimestampValue parameterTimestampValue =
                    new ParameterTimestampValueMap.ParameterTimestampValue(
                            updatedTimestamp, stateVal);
            result.put(deviceSetting, parameterTimestampValue);
        }
    }

    private static void buildInverterResult(
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            InverterSettingResponseDto responseChangedSettings,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        for (DeviceCompositeKey deviceSetting : requestSettings) {
            String fieldName = deviceSetting.name();
            Double value = getInverterSettingValue(responseChangedSettings, fieldName);
            ParameterTimestampValueMap.ParameterTimestampValue parameterTimestampValue =
                    new ParameterTimestampValueMap.ParameterTimestampValue(updatedTimestamp, value);
            result.put(deviceSetting, parameterTimestampValue);
        }
    }

    private static void buildPvlResult(
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            PvlSettingResponseDto responseChangedSettings,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        for (DeviceCompositeKey deviceSetting : requestSettings) {
            String fieldName = deviceSetting.name();
            Double value = getPvlSettingValue(responseChangedSettings, fieldName);
            ParameterTimestampValueMap.ParameterTimestampValue parameterTimestampValue =
                    new ParameterTimestampValueMap.ParameterTimestampValue(updatedTimestamp, value);
            result.put(deviceSetting, parameterTimestampValue);
        }
    }

    private static void buildBatteryResult(
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            List<BaseBatterySetting> responseChangedSettings,
            List<DeviceCompositeKey> requestSettings,
            OffsetDateTime updatedTimestamp) {
        responseChangedSettings.forEach(
                responseSetting -> {
                    for (DeviceCompositeKey deviceSetting : requestSettings) {
                        String fieldName = deviceSetting.name();
                        Double value = getBatterySettingValue(responseSetting, fieldName);
                        ParameterTimestampValueMap.ParameterTimestampValue parameterTimestampValue =
                                new ParameterTimestampValueMap.ParameterTimestampValue(
                                        updatedTimestamp, value);
                        result.put(deviceSetting, parameterTimestampValue);
                    }
                });
    }

    private static Double getInverterSettingValue(
            InverterSettingResponseDto inverterSettingResponseDto, String fieldName) {
        switch (fieldName) {
            case "isIslandingEnabled" -> {
                return inverterSettingResponseDto.getIslanding() == null
                        ? null
                        : Islanding.fromEnum(inverterSettingResponseDto.getIslanding()).getValue();
            }
            case "zeroExportOverride" -> {
                return inverterSettingResponseDto.getExportOverride() == null
                        ? null
                        : ExportOverride.fromName(inverterSettingResponseDto.getExportOverride())
                                .getValue();
            }
            case "noOfTransferSwitches" -> {
                return inverterSettingResponseDto.getNumberOfTransferSwitches() == null
                        ? null
                        : NumberOfTransferSwitches.fromEnum(
                                        inverterSettingResponseDto.getNumberOfTransferSwitches())
                                .getValue();
            }
            case "loadSheddingSetting" -> {
                return inverterSettingResponseDto.getLoadShedding() == null
                        ? null
                        : LoadShedding.fromEnum(inverterSettingResponseDto.getLoadShedding())
                                .getValue();
            }
            case "ctCalibration" -> {
                return inverterSettingResponseDto.getCtCalibration() == null
                        ? null
                        : CTCalibration.fromEnum(inverterSettingResponseDto.getCtCalibration())
                                .getValue();
            }
            case "acGeneratorControlMode" -> {
                return inverterSettingResponseDto.getGeneratorControlMode() == null
                        ? null
                        : GeneratorControlMode.fromEnum(
                                        inverterSettingResponseDto.getGeneratorControlMode())
                                .getValue();
            }
            case "isZeroExportEnabled" -> {
                return inverterSettingResponseDto.getExportLimitState() == null
                        ? null
                        : ExportLimitState.fromEnum(
                                        inverterSettingResponseDto.getExportLimitState())
                                .getValue();
            }
            case "maxExportPower" -> {
                return inverterSettingResponseDto.getExportLimit() == null
                        ? null
                        : Double.valueOf(inverterSettingResponseDto.getExportLimit());
            }
            default -> log.info("Invalid inverter setting provided: " + fieldName);
        }
        return null;
    }

    private static Double getPvlSettingValue(
            PvlSettingResponseDto pvlSettingResponseDto, String fieldName) {
        switch (fieldName) {
            case "enablePVRSS" -> {
                return pvlSettingResponseDto.getPvrssState() == null
                        ? null
                        : EnablePvrss.fromEnum(pvlSettingResponseDto.getPvrssState()).getValue();
            }
            case "vinStartup" -> {
                return pvlSettingResponseDto.getMinimumInputVoltage();
            }
            case "numStrings" -> {
                return pvlSettingResponseDto.getNumberOfSubstrings() == null
                        ? null
                        : NumberOfSubstrings.fromEnum(pvlSettingResponseDto.getNumberOfSubstrings())
                                .getValue();
            }
            default -> log.info("Invalid PV Link setting provided: " + fieldName);
        }
        return null;
    }

    private static Double getBatterySettingValue(
            BaseBatterySetting baseBatterySetting, String fieldName) {
        switch (fieldName) {
            case "socMax" -> {
                return baseBatterySetting.getSocMax();
            }
            case "socMin" -> {
                return baseBatterySetting.getSocMin();
            }
            case "socRsvMax" -> {
                return baseBatterySetting.getSocRsvMax();
            }
            case "socRsvMin" -> {
                return baseBatterySetting.getSocRsvMin();
            }
            case "aChaMax" -> {
                return baseBatterySetting.getAChaMax();
            }
            case "aDisChaMax" -> {
                return baseBatterySetting.getADisChaMax();
            }
            default -> log.info("Invalid base battery setting provided: " + fieldName);
        }
        return null;
    }

    private static void updateResultWithCachedValuesIfApplicable(
            Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue> result,
            ParameterTimestampValueMap<DeviceSetting> currentCacheContents) {
        Set<DeviceCompositeKey> requestKeys = result.keySet();
        Set<DeviceCompositeKey> keysToBeAddedToResult =
                new HashSet<>(currentCacheContents.getEntryMap().keySet());
        keysToBeAddedToResult.removeAll(requestKeys);
        Map<DeviceCompositeKey, ParameterTimestampValueMap.ParameterTimestampValue>
                currentCacheMap = currentCacheContents.getEntryMap();
        keysToBeAddedToResult.forEach(
                currCompositeKey -> {
                    ParameterTimestampValueMap.ParameterTimestampValue cachedValue =
                            currentCacheMap.get(currCompositeKey);
                    if (cachedValue != null) {
                        result.put(
                                currCompositeKey,
                                new ParameterTimestampValueMap.ParameterTimestampValue(
                                        cachedValue.getTimestamp(), cachedValue.getValue()));
                    }
                });
    }
}
