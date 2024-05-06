package com.generac.ces.systemgateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass.InverterSetting.CTCalibration;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass.InverterSetting.ExportLimitState;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode;
import com.generac.ces.system.control.subcontrol.PvLinkControl;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.configuration.TestConfig;
import com.generac.ces.systemgateway.exception.GatewayTimeoutException;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.helper.DeviceCompositeKey;
import com.generac.ces.systemgateway.model.BaseBatterySetting;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.DeviceStateRequest;
import com.generac.ces.systemgateway.model.DeviceStateResponse;
import com.generac.ces.systemgateway.model.InverterSettingResponseDto;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.ParameterTimestampMap;
import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.PvlSettingResponseDto;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.NotificationType;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.service.system.DeviceSettingsService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = {TestConfig.class})
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:test.properties")
public class DeviceSettingsServiceTest {

    @InjectMocks private DeviceSettingsService deviceSettingsService;
    @Mock private OdinService odinService;

    @SpyBean private CacheStore<ParameterTimestampMap> remoteSettingsCache;

    @Mock private EssSystemService essSystemService;

    @Mock private SystemSettingCacheService<DeviceSetting> systemSettingCacheService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        deviceSettingsService =
                new DeviceSettingsService(
                        remoteSettingsCache,
                        odinService,
                        essSystemService,
                        systemSettingCacheService);
    }

    @Test
    public void testSetDeviceState_EssSystemTypeValidRequest() throws InterruptedException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        UUID updateId = UUID.randomUUID();
        OffsetDateTime eventSettingsChangedTime = OffsetDateTime.now();
        String deviceRcpn = "000100080A99";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        SystemType systemType = SystemType.ESS;

        DeviceStateResponse expected =
                DeviceStateResponse.builder()
                        .updateId(updateId)
                        .systemId(systemId)
                        .deviceId(deviceRcpn)
                        .enable(true)
                        .deviceType(DeviceTypeOuterClass.DeviceType.INVERTER)
                        .eventSettingsChangedTime(eventSettingsChangedTime)
                        .build();

        when(essSystemService.getDeviceType(any(), any()))
                .thenReturn(DeviceTypeOuterClass.DeviceType.INVERTER);

        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("SUCCESS");
        odinResponse.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .inverter(
                                InverterSettingResponseDto.builder()
                                        .rcpn(deviceRcpn)
                                        .state(DeviceState.STATE_ENABLED)
                                        .build())
                        .eventSettingsChangedTime(eventSettingsChangedTime)
                        .build());

        when(odinService.odinInverterSettingsRequest(any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        Thread.sleep(3000); // simulate expiry of ttl
        DeviceStateResponse actual =
                deviceSettingsService
                        .setDeviceState(
                                systemId,
                                deviceRcpn,
                                callerId,
                                userId,
                                new DeviceStateRequest(true),
                                systemType)
                        .block();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testSetDeviceState_InvalidSystemTypeThrowsIllegalArgumentException() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String deviceRcpn = "000100080A99";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        SystemType systemType = SystemType.HEM;

        String expected = "Unsupported SystemType: " + systemType;

        // Action & Assert
        try {
            Thread.sleep(3000); // simulate expiry of ttl
            deviceSettingsService
                    .setDeviceState(
                            systemId,
                            deviceRcpn,
                            callerId,
                            userId,
                            new DeviceStateRequest(),
                            systemType)
                    .block();
            fail("expected to throw '400: " + expected + "'");
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void testSetDeviceState_CacheHit() throws InterruptedException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        OffsetDateTime eventSettingsChangedTime = OffsetDateTime.now();
        String deviceRcpn = "000100080A99";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        SystemType systemType = SystemType.ESS;

        when(essSystemService.getDeviceType(any(), any()))
                .thenReturn(DeviceTypeOuterClass.DeviceType.INVERTER);

        ParameterTimestampValueMap<DeviceSetting> cacheContents =
                new ParameterTimestampValueMap<>();
        cacheContents.addParameterTimestampValue(
                new DeviceCompositeKey("isInverterEnabled", deviceRcpn),
                2.0,
                eventSettingsChangedTime);
        when(systemSettingCacheService.getSettings(any(), any())).thenReturn(cacheContents);

        doThrow(new TooManyRequestsException(""))
                .when(systemSettingCacheService)
                .throwOnRateLimit(any(), any(), any());

        // Action
        try {
            deviceSettingsService.setDeviceState(
                    systemId,
                    deviceRcpn,
                    callerId,
                    userId,
                    new DeviceStateRequest(true),
                    systemType);
            Assert.fail("Expected TooManyRequestsException to be thrown");
        } catch (TooManyRequestsException ignored) {
        }
        verify(systemSettingCacheService, times(1)).throwOnRateLimit(any(), any(), any());
    }

    // =======================================================================================================
    //   PATCH INVERTER SETTINGS
    // =======================================================================================================

    @Test
    public void setInverterSettings_ValidRequest_Success() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        UUID updateId = UUID.randomUUID();
        OffsetDateTime updatedTimestamp = OffsetDateTime.now();

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.ISLANDING)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_OVERRIDE)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES)
                                                .value(3.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.LOAD_SHEDDING)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.CT_CALIBRATION)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.GENERATOR_CONTROL_MODE)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_LIMITING)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_POWER_LIMIT)
                                                .value(10.0)
                                                .build()))
                        .build();
        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("SUCCESS");
        odinResponse.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .eventSettingsChangedTime(updatedTimestamp)
                        .inverter(
                                InverterSettingResponseDto.builder()
                                        .rcpn(rcpId)
                                        .islanding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .Islanding.ISLANDING_ENABLED)
                                        .exportOverride(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .ExportOverride.EXPORT_OVERRIDE_DISABLED)
                                        .numberOfTransferSwitches(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .NumberOfTransferSwitches
                                                        .NUMBER_OF_TRANSFER_SWITCHES_TWO)
                                        .loadShedding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .LoadShedding.LOAD_SHEDDING_UNSPECIFIED)
                                        .ctCalibration(CTCalibration.CT_CALIBRATION_TRIGGER)
                                        .generatorControlMode(
                                                GeneratorControlMode
                                                        .GENERATOR_CONTROL_MODE_SINGLE_TRANSFER)
                                        .exportLimitState(
                                                ExportLimitState.EXPORT_LIMIT_STATE_DISABLED)
                                        .exportLimit(10)
                                        .build())
                        .build());

        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId(rcpId);

        when(essSystemService.getSystemBySystemId(systemId)).thenReturn(systemResponse);
        when(odinService.setInverterSettings(any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        DeviceSettingUpdateResponse result =
                deviceSettingsService
                        .setDeviceSettings(
                                systemId,
                                deviceSettingsUpdateRequest,
                                callerId,
                                userId,
                                null,
                                RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                                systemType)
                        .block();

        // Assert
        assertThat(result.devices().get(0).settings())
                .extracting(
                        DeviceSettingUpdateResponse.Device.DeviceSettingChange::name,
                        DeviceSettingUpdateResponse.Device.DeviceSettingChange::value)
                .containsExactlyInAnyOrder(
                        tuple(DeviceSetting.ISLANDING, 1.0),
                        tuple(DeviceSetting.EXPORT_OVERRIDE, 0.0),
                        tuple(DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES, 2.0),
                        tuple(DeviceSetting.LOAD_SHEDDING, -1.0),
                        tuple(DeviceSetting.CT_CALIBRATION, 1.0),
                        tuple(DeviceSetting.GENERATOR_CONTROL_MODE, 0.0),
                        tuple(DeviceSetting.EXPORT_LIMITING, 0.0),
                        tuple(DeviceSetting.EXPORT_POWER_LIMIT, 10.0));

        // Verify that the necessary methods were called
        verify(essSystemService, times(1)).getSystemBySystemId(systemId);
        verify(odinService, times(1)).setInverterSettings(any(), any(), any(), any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInverterSettings_InvalidSystemType_ThrowsException() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.HEM; // Unsupported system type
        String callerId = "testCaller";
        String userId = "testUser";
        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.ISLANDING)
                                                .value(2.0)
                                                .build()))
                        .build();

        // Action
        deviceSettingsService
                .setDeviceSettings(
                        systemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        null,
                        RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                        systemType)
                .block();
    }

    @Test
    public void setInverterSettings_OdinServiceFailure_ReturnsErrorResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        UUID updateId = UUID.fromString("75c2871a-b28e-4466-8f46-748d4dc77b14");

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.ISLANDING)
                                                .value(2.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_OVERRIDE)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES)
                                                .value(3.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.LOAD_SHEDDING)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_POWER_LIMIT)
                                                .value(10.0)
                                                .build()))
                        .build();

        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("TIMEOUT");
        odinResponse.setErrorMessage(
                "No response from the Beacon was found for controlMessageId: 24794127530. It may be"
                        + " offline.");

        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId(rcpId);

        when(systemSettingCacheService.getSettings(any(), any())).thenReturn(null);
        when(essSystemService.getSystemBySystemId(systemId)).thenReturn(systemResponse);
        when(odinService.setInverterSettings(any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        try {
            deviceSettingsService
                    .setDeviceSettings(
                            systemId,
                            deviceSettingsUpdateRequest,
                            callerId,
                            userId,
                            null,
                            RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                            systemType)
                    .block();
            fail("Expected GatewayTimeoutException was not thrown");
        } catch (GatewayTimeoutException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo(
                            "Did not receive a response from the device in time, id: "
                                    + updateId
                                    + ".");
        }
    }

    // =======================================================================================================
    //   PATCH BATTERY SETTINGS
    // =======================================================================================================
    @Test
    public void setBatterySettings_ValidRequest_Success() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        String providedRcpn = "000100080BBE";
        UUID updateId = UUID.randomUUID();
        OffsetDateTime updatedTimestamp = OffsetDateTime.now();

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(99.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_DISCHA_MAX)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MAX)
                                                .value(98.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MIN)
                                                .value(2.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(12.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(7.0)
                                                .build()))
                        .build();

        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("SUCCESS");
        odinResponse.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .eventSettingsChangedTime(updatedTimestamp)
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(
                                                List.of(
                                                        BatterySettingResponseDto.OdinBatterySetting
                                                                .builder()
                                                                .batterySetting(
                                                                        BaseBatterySetting.builder()
                                                                                .aChaMax(99.0)
                                                                                .aDisChaMax(1.0)
                                                                                .socRsvMin(2.0)
                                                                                .socRsvMax(98.0)
                                                                                .socMax(12.0)
                                                                                .socMin(7.0)
                                                                                .build())
                                                                .build()))
                                        .build())
                        .build());

        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId(rcpId);

        when(odinService.setBatterySettings(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        DeviceSettingUpdateResponse result =
                deviceSettingsService
                        .setDeviceSettings(
                                systemId,
                                deviceSettingsUpdateRequest,
                                callerId,
                                userId,
                                providedRcpn,
                                RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                                systemType)
                        .block();

        // Assert
        assertThat(result.devices().get(0).settings())
                .extracting(
                        DeviceSettingUpdateResponse.Device.DeviceSettingChange::name,
                        DeviceSettingUpdateResponse.Device.DeviceSettingChange::value)
                .containsExactlyInAnyOrder(
                        tuple(DeviceSetting.A_CHA_MAX, 99.0),
                        tuple(DeviceSetting.A_DISCHA_MAX, 1.0),
                        tuple(DeviceSetting.SOC_RSV_MIN, 2.0),
                        tuple(DeviceSetting.SOC_RSV_MAX, 98.0),
                        tuple(DeviceSetting.SOC_MAX, 12.0),
                        tuple(DeviceSetting.SOC_MIN, 7.0));

        // Verify that the necessary methods were called
        verify(odinService, times(1)).setBatterySettings(any(), any(), any(), any(), any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBatterySettings_InvalidSystemType_ThrowsException() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.HEM; // Unsupported system type
        String callerId = "testCaller";
        String userId = "testUser";
        String providedRcpn = "000100080BBE";
        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(100.0)
                                                .build()))
                        .build();

        // Action
        deviceSettingsService
                .setDeviceSettings(
                        systemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        providedRcpn,
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        systemType)
                .block();
    }

    @Test
    public void setBatterySettings_OdinServiceFailure_ReturnsErrorResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        String providedRcpn = "000100080BBE";
        UUID updateId = UUID.fromString("75c2871a-b28e-4466-8f46-748d4dc77b14");

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(99.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_DISCHA_MAX)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MAX)
                                                .value(98.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MIN)
                                                .value(2.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(12.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(7.0)
                                                .build()))
                        .build();

        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("TIMEOUT");
        odinResponse.setErrorMessage(
                "No response from the Beacon was found for controlMessageId: 24794127530. It may be"
                        + " offline.");

        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId(rcpId);

        when(systemSettingCacheService.getSettings(any(), any())).thenReturn(null);
        when(odinService.setBatterySettings(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        try {
            deviceSettingsService
                    .setDeviceSettings(
                            systemId,
                            deviceSettingsUpdateRequest,
                            callerId,
                            userId,
                            providedRcpn,
                            RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                            systemType)
                    .block();
            fail("Expected GatewayTimeoutException was not thrown");
        } catch (GatewayTimeoutException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo(
                            "Did not receive a response from the device in time, id: "
                                    + updateId
                                    + ".");
        }
    }

    // =======================================================================================================
    //   PATCH PVL SETTINGS
    // =======================================================================================================
    @Test
    public void setPvlSettings_ValidRequest_Success() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        String pvLinkRcpn = "00010033001F";
        UUID updateId = UUID.randomUUID();
        OffsetDateTime updatedTimestamp = OffsetDateTime.now();

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.ENABLE_PVRSS)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.VIN_STARTUP)
                                                .value(120.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.NUM_STRING)
                                                .value(2.0)
                                                .build()))
                        .build();

        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("SUCCESS");
        odinResponse.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .eventSettingsChangedTime(updatedTimestamp)
                        .pvLink(
                                PvlSettingResponseDto.builder()
                                        .pvrssState(
                                                PvLinkControl.PVLinkSetting.PVRSSState
                                                        .PVRSS_STATE_ON)
                                        .minimumInputVoltage(120.0)
                                        .numberOfSubstrings(
                                                PvLinkControl.PVLinkSetting.NumberOfSubstrings
                                                        .NUMBER_OF_SUBSTRINGS_TWO)
                                        .build())
                        .build());

        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId(rcpId);

        when(odinService.setPvLinkSettings(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        DeviceSettingUpdateResponse result =
                deviceSettingsService
                        .setDeviceSettings(
                                systemId,
                                deviceSettingsUpdateRequest,
                                callerId,
                                userId,
                                pvLinkRcpn,
                                RequestType.INSTANT_PVLINK_SETTINGS_PATCH,
                                systemType)
                        .block();

        // Assert
        assertThat(result.devices().get(0).settings())
                .extracting(
                        DeviceSettingUpdateResponse.Device.DeviceSettingChange::name,
                        DeviceSettingUpdateResponse.Device.DeviceSettingChange::value)
                .containsExactlyInAnyOrder(
                        tuple(DeviceSetting.ENABLE_PVRSS, 1.0),
                        tuple(DeviceSetting.VIN_STARTUP, 120.0),
                        tuple(DeviceSetting.NUM_STRING, 2.0));

        // Verify that the necessary methods were called
        verify(odinService, times(1)).setPvLinkSettings(any(), any(), any(), any(), any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setPvlSettings_InvalidSystemType_ThrowsException() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.HEM; // Unsupported system type
        String callerId = "testCaller";
        String userId = "testUser";
        String pvLinkRcpn = "00010033001F";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.ENABLE_PVRSS)
                                                .value(1.0)
                                                .build()))
                        .build();

        // Action
        deviceSettingsService
                .setDeviceSettings(
                        systemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        pvLinkRcpn,
                        RequestType.INSTANT_PVLINK_SETTINGS_PATCH,
                        systemType)
                .block();
    }

    @Test
    public void setPvlSettings_OdinServiceFailure_ReturnsErrorResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        String pvLinkRcpn = "00010033001F";
        UUID updateId = UUID.fromString("75c2871a-b28e-4466-8f46-748d4dc77b14");

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.ENABLE_PVRSS)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.VIN_STARTUP)
                                                .value(120.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.NUM_STRING)
                                                .value(2.0)
                                                .build()))
                        .build();

        OdinResponse odinResponse = new OdinResponse();
        odinResponse.setId(updateId.toString());
        odinResponse.setCode("TIMEOUT");
        odinResponse.setErrorMessage(
                "No response from the Beacon was found for controlMessageId: 24794127530. It may be"
                        + " offline.");

        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId(rcpId);

        when(systemSettingCacheService.getSettings(any(), any())).thenReturn(null);
        when(odinService.setPvLinkSettings(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(odinResponse));

        // Action
        try {
            deviceSettingsService
                    .setDeviceSettings(
                            systemId,
                            deviceSettingsUpdateRequest,
                            callerId,
                            userId,
                            pvLinkRcpn,
                            RequestType.INSTANT_PVLINK_SETTINGS_PATCH,
                            systemType)
                    .block();
            fail("Expected GatewayTimeoutException was not thrown");
        } catch (GatewayTimeoutException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo(
                            "Did not receive a response from the device in time, id: "
                                    + updateId
                                    + ".");
        }
    }
}
