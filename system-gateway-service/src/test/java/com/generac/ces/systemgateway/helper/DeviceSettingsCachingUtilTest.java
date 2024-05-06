package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode;
import com.generac.ces.systemgateway.model.BaseBatterySetting;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.InverterSettingResponseDto;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest.DeviceSettingChange;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeviceSettingsCachingUtilTest {

    @Test
    void
            testGetValueMapOfSettingsToBeCached_battery_unCachedSettingReturnsSuppliedRequestSetting() {
        String rcpn = String.valueOf(UUID.randomUUID());
        DeviceSettingsUpdateRequest request =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        new DeviceSettingsUpdateRequest.DeviceSettingChange(
                                                DeviceSetting.SOC_MAX, 100.0)))
                        .build();
        OdinResponse response = new OdinResponse();
        OffsetDateTime eventSettingsChangedTime = OffsetDateTime.now();
        response.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(
                                                List.of(
                                                        BatterySettingResponseDto.OdinBatterySetting
                                                                .builder()
                                                                .batterySetting(
                                                                        BaseBatterySetting.builder()
                                                                                .socMax(100.0)
                                                                                .build())
                                                                .rcpn(rcpn)
                                                                .build()))
                                        .build())
                        .eventSettingsChangedTime(eventSettingsChangedTime)
                        .build());

        ParameterTimestampValueMap<DeviceSetting> expected = new ParameterTimestampValueMap<>();
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.SOC_MAX.getFormattedSettingName(), rcpn),
                100.0,
                eventSettingsChangedTime);
        ParameterTimestampValueMap<DeviceSetting> actual =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        request,
                        response,
                        new ParameterTimestampValueMap<>(),
                        List.of(rcpn),
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        DeviceTypeOuterClass.DeviceType.BATTERY);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testGetValueMapOfSettingsToBeCached_battery_cachedSettingIncludedInResultMap() {
        String rcpn = String.valueOf(UUID.randomUUID());
        DeviceSettingsUpdateRequest request =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        new DeviceSettingsUpdateRequest.DeviceSettingChange(
                                                DeviceSetting.SOC_MAX, 100.0)))
                        .build();
        OdinResponse response = new OdinResponse();
        OffsetDateTime eventSettingsChangedTime = OffsetDateTime.now();
        response.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(
                                                List.of(
                                                        BatterySettingResponseDto.OdinBatterySetting
                                                                .builder()
                                                                .batterySetting(
                                                                        BaseBatterySetting.builder()
                                                                                .socMax(100.0)
                                                                                .build())
                                                                .rcpn(rcpn)
                                                                .build()))
                                        .build())
                        .eventSettingsChangedTime(eventSettingsChangedTime)
                        .build());
        ParameterTimestampValueMap<DeviceSetting> cacheContents =
                new ParameterTimestampValueMap<>();
        cacheContents.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.SOC_MIN.getFormattedSettingName(), rcpn),
                1.0,
                eventSettingsChangedTime);

        ParameterTimestampValueMap<DeviceSetting> expected = new ParameterTimestampValueMap<>();
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.SOC_MAX.getFormattedSettingName(), rcpn),
                100.0,
                eventSettingsChangedTime);
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.SOC_MIN.getFormattedSettingName(), rcpn),
                1.0,
                eventSettingsChangedTime);
        ParameterTimestampValueMap<DeviceSetting> actual =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        request,
                        response,
                        cacheContents,
                        List.of(rcpn),
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        DeviceTypeOuterClass.DeviceType.BATTERY);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void
            testGetValueMapOfSettingsToBeCached_battery_requestValueMatchingCachedValueOverridesCachedValue() {
        String rcpn = String.valueOf(UUID.randomUUID());
        DeviceSettingsUpdateRequest request =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        new DeviceSettingsUpdateRequest.DeviceSettingChange(
                                                DeviceSetting.SOC_MAX, 100.0)))
                        .build();
        OdinResponse response = new OdinResponse();
        OffsetDateTime responseTime = OffsetDateTime.now();
        OffsetDateTime cachedTime = OffsetDateTime.now().minusSeconds(10);
        response.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(
                                                List.of(
                                                        BatterySettingResponseDto.OdinBatterySetting
                                                                .builder()
                                                                .batterySetting(
                                                                        BaseBatterySetting.builder()
                                                                                .socMax(99.0)
                                                                                .build())
                                                                .rcpn(rcpn)
                                                                .build()))
                                        .build())
                        .eventSettingsChangedTime(responseTime)
                        .build());
        ParameterTimestampValueMap<DeviceSetting> cacheContents =
                new ParameterTimestampValueMap<>();
        cacheContents.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.SOC_MAX.getFormattedSettingName(), rcpn),
                100.0,
                cachedTime);

        ParameterTimestampValueMap<DeviceSetting> expected = new ParameterTimestampValueMap<>();
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.SOC_MAX.getFormattedSettingName(), rcpn),
                99.0,
                responseTime);
        ParameterTimestampValueMap<DeviceSetting> actual =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        request,
                        response,
                        cacheContents,
                        List.of(rcpn),
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        DeviceTypeOuterClass.DeviceType.BATTERY);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void
            testGetValueMapOfSettingsToBeCached_inverter_unCachedSettingReturnsSuppliedRequestSetting() {
        String rcpn = String.valueOf(UUID.randomUUID());
        DeviceSettingsUpdateRequest request =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        new DeviceSettingsUpdateRequest.DeviceSettingChange(
                                                DeviceSetting.ISLANDING, 0.0),
                                        new DeviceSettingChange(
                                                DeviceSetting.GENERATOR_CONTROL_MODE, 0.0)))
                        .build();
        OdinResponse response = new OdinResponse();
        OffsetDateTime eventSettingsChangedTime = OffsetDateTime.now();
        response.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .inverter(
                                InverterSettingResponseDto.builder()
                                        .islanding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .Islanding.ISLANDING_DISABLED)
                                        .generatorControlMode(
                                                GeneratorControlMode
                                                        .GENERATOR_CONTROL_MODE_SINGLE_TRANSFER)
                                        .build())
                        .eventSettingsChangedTime(eventSettingsChangedTime)
                        .build());

        ParameterTimestampValueMap<DeviceSetting> expected = new ParameterTimestampValueMap<>();
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.ISLANDING.getFormattedSettingName(), rcpn),
                0.0,
                eventSettingsChangedTime);
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.GENERATOR_CONTROL_MODE.getFormattedSettingName(), rcpn),
                0.0,
                eventSettingsChangedTime);
        ParameterTimestampValueMap<DeviceSetting> actual =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        request,
                        response,
                        new ParameterTimestampValueMap<>(),
                        List.of(rcpn),
                        RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                        DeviceTypeOuterClass.DeviceType.INVERTER);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testGetValueMapOfSettingsToBeCached_inverter_cachedSettingIncludedInResultMap() {
        String rcpn = String.valueOf(UUID.randomUUID());
        DeviceSettingsUpdateRequest request =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        new DeviceSettingsUpdateRequest.DeviceSettingChange(
                                                DeviceSetting.ISLANDING, 0.0),
                                        new DeviceSettingChange(
                                                DeviceSetting.GENERATOR_CONTROL_MODE, 0.0)))
                        .build();
        OdinResponse response = new OdinResponse();
        OffsetDateTime eventSettingsChangedTime = OffsetDateTime.now();
        response.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .inverter(
                                InverterSettingResponseDto.builder()
                                        .islanding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .Islanding.ISLANDING_DISABLED)
                                        .generatorControlMode(
                                                GeneratorControlMode
                                                        .GENERATOR_CONTROL_MODE_SINGLE_TRANSFER)
                                        .build())
                        .eventSettingsChangedTime(eventSettingsChangedTime)
                        .build());
        ParameterTimestampValueMap<DeviceSetting> cacheContents =
                new ParameterTimestampValueMap<>();
        cacheContents.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES.getFormattedSettingName(), rcpn),
                2.0,
                eventSettingsChangedTime);
        cacheContents.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.CT_CALIBRATION.getFormattedSettingName(), rcpn),
                0.0,
                eventSettingsChangedTime);
        ParameterTimestampValueMap<DeviceSetting> expected = new ParameterTimestampValueMap<>();
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.ISLANDING.getFormattedSettingName(), rcpn),
                0.0,
                eventSettingsChangedTime);
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.GENERATOR_CONTROL_MODE.getFormattedSettingName(), rcpn),
                0.0,
                eventSettingsChangedTime);
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES.getFormattedSettingName(), rcpn),
                2.0,
                eventSettingsChangedTime);
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(
                        DeviceSetting.CT_CALIBRATION.getFormattedSettingName(), rcpn),
                0.0,
                eventSettingsChangedTime);
        ParameterTimestampValueMap<DeviceSetting> actual =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        request,
                        response,
                        cacheContents,
                        List.of(rcpn),
                        RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                        DeviceTypeOuterClass.DeviceType.INVERTER);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void
            testGetValueMapOfSettingsToBeCached_inverter_requestValueMatchingCachedValueOverridesCachedValue() {
        String rcpn = String.valueOf(UUID.randomUUID());
        DeviceSettingsUpdateRequest request =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        new DeviceSettingsUpdateRequest.DeviceSettingChange(
                                                DeviceSetting.ISLANDING, 1.0)))
                        .build();
        OdinResponse response = new OdinResponse();
        OffsetDateTime responseTime = OffsetDateTime.now();
        OffsetDateTime cachedTime = OffsetDateTime.now().minusSeconds(10);
        response.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .inverter(
                                InverterSettingResponseDto.builder()
                                        .islanding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .Islanding.ISLANDING_ENABLED)
                                        .build())
                        .eventSettingsChangedTime(responseTime)
                        .build());
        ParameterTimestampValueMap<DeviceSetting> cacheContents =
                new ParameterTimestampValueMap<>();
        cacheContents.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.ISLANDING.getFormattedSettingName(), rcpn),
                1.0,
                cachedTime);

        ParameterTimestampValueMap<DeviceSetting> expected = new ParameterTimestampValueMap<>();
        expected.addParameterTimestampValue(
                new DeviceCompositeKey(DeviceSetting.ISLANDING.getFormattedSettingName(), rcpn),
                1.0,
                responseTime);
        ParameterTimestampValueMap<DeviceSetting> actual =
                DeviceSettingsCachingUtil.getValueMapOfDeviceSettingsToBeCached(
                        request,
                        response,
                        cacheContents,
                        List.of(rcpn),
                        RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                        DeviceTypeOuterClass.DeviceType.INVERTER);
        Assertions.assertEquals(expected, actual);
    }
}
