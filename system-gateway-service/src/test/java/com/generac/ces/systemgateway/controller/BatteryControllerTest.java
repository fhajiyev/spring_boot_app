package com.generac.ces.systemgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.essdataprovider.model.BatteryPropertiesDto;
import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.controller.device.BatteryController;
import com.generac.ces.systemgateway.helper.DeviceSettingUpdateResponseConverter;
import com.generac.ces.systemgateway.model.BaseBatterySetting;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.common.Type;
import com.generac.ces.systemgateway.model.common.Unit;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettings;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.service.system.DeviceSettingsService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BatteryController.class)
@AutoConfigureMockMvc
public class BatteryControllerTest {

    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean private OdinService odinService;

    @MockBean private EssSystemService systemService;

    @MockBean private EssDataProviderService essDataProviderService;

    @MockBean private DeviceSettingsService deviceSettingsService;

    // =======================================================================================================
    //   GET BATTERY MIN/MAX
    // =======================================================================================================
    @Test
    public void testGetBatterySettings_happyCaseOk() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String deviceId = "000100080A99";
        String hostRcpn = "00010007A42A";
        DeviceSetting settingName = DeviceSetting.A_CHA_MAX;
        String settingLabel = "Maximum Charge Current";
        Double settingValue = 10.0;
        String settingType = String.valueOf(Type.FLOAT);
        String settingUnit = String.valueOf(Unit.AMPS);
        String settingDescription = "Instantaneous maximum DC charge current.";
        Double settingMinValue = 0.0;
        Double settingMaxValue = 35.0;

        DeviceSettings.DeviceSettingsMetadata deviceSettingsMetadata =
                DeviceSettings.DeviceSettingsMetadata.builder()
                        .name(settingName)
                        .label(settingLabel)
                        .value(settingValue)
                        .type(settingType)
                        .unit(settingUnit)
                        .description(settingDescription)
                        .constraints(
                                DeviceSettings.DeviceSettingsMetadata.MetadataConstraints.builder()
                                        .maxValue(settingMaxValue)
                                        .minValue(settingMinValue)
                                        .build())
                        .build();

        DeviceSettings batteryMetadata =
                new DeviceSettings(
                        deviceId,
                        DeviceTypeOuterClass.DeviceType.BATTERY,
                        List.of(deviceSettingsMetadata));

        com.generac.ces.systemgateway.model.device.DeviceSettingsResponse mockResponse =
                com.generac.ces.systemgateway.model.device.DeviceSettingsResponse.builder()
                        .hostDeviceId(hostRcpn)
                        .devices(List.of(batteryMetadata))
                        .build();

        when(systemService.getBatterySettings(systemId)).thenReturn(Mono.just(mockResponse));

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                get(
                                        "/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/batteries/settings"))
                        .andExpect(status().isOk())
                        .andReturn();

        verify(systemService, times(1)).getBatterySettings(systemId);

        String expected = String.valueOf(Mono.just(mockResponse).block());
        String actual = mvcResult.getAsyncResult().toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetBatterySettings_nullSystemIdNotFound() throws Exception {
        // Action & Assert
        mvc.perform(get("/v1/systems/batteries/settings")).andExpect(status().isNotFound());

        verify(systemService, times(0)).getSystemBySystemId(null);
        verify(systemService, times(0)).getBatterySettings(null);
    }

    // =======================================================================================================
    //   GET FULL BATTERY PROPERTIES
    // =======================================================================================================
    @Test
    public void testGetFullBatteryProperties_happyCaseAccepted() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String testDeviceRcpn = "000100080A99";
        SystemType testSystemType = SystemType.ESS;

        BatteryPropertiesDto mockResponse =
                BatteryPropertiesDto.builder().deviceId(testDeviceRcpn).build();

        when(systemService.getBatteryProperties(testSystemId, testDeviceRcpn))
                .thenReturn(Mono.just(mockResponse));

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                get("/v1/systems/"
                                                + testSystemId
                                                + "/batteries/"
                                                + testDeviceRcpn
                                                + "/settings/full")
                                        .queryParam("systemType", String.valueOf(testSystemType)))
                        .andExpect(status().isOk())
                        .andReturn();

        String expected = String.valueOf(Mono.just(mockResponse).block());
        String actual = mvcResult.getAsyncResult().toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetFullBatteryProperties_InvalidSystemIdThrowsBadRequest() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String testDeviceRcpn = "000100080A99";
        SystemType testSystemType = SystemType.PWRMICRO;

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                get("/v1/systems/"
                                                + testSystemId
                                                + "/batteries/"
                                                + testDeviceRcpn
                                                + "/settings/full")
                                        .queryParam("systemType", String.valueOf(testSystemType)))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        verify(systemService, times(0)).getBatteryProperties(testSystemId, testDeviceRcpn);

        String expected = "Invalid System Type";
        JsonNode jsonNodeActual =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String actual = jsonNodeActual.get("errorMsg").asText();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetFullBatteryProperties_nullSystemIdNotFound() throws Exception {
        // Arrange
        String testDeviceRcpn = "000100080A99";

        // Action & Assert
        mvc.perform(get("/v1/systems/batteries/" + testDeviceRcpn + "/settings/full"))
                .andExpect(status().isNotFound());

        verify(systemService, times(0)).getBatteryProperties(any(), any());
    }

    @Test
    public void testGetFullBatteryProperties_nullDeviceRcpnNotFound() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");

        // Action & Assert
        mvc.perform(get("/v1/systems/" + testSystemId + "/batteries/settings/full"))
                .andExpect(status().isNotFound());

        verify(systemService, times(0)).getBatteryProperties(any(), any());
    }

    // =======================================================================================================
    //   SET SINGLE BATTERY SETTINGS
    // =======================================================================================================

    @Test
    public void testSetBatterySettings_SettingNotEnabled() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "testRcpn";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.LOAD_SHEDDING)
                                                .value(10.2)
                                                .build()))
                        .build();

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + testSystemId
                                                        + "/batteries/"
                                                        + providedRcpn
                                                        + "/settings")
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        deviceSettingsUpdateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        verify(odinService, times(0)).setBatterySettings(any(), any(), any(), any(), any());

        String expected =
                "[\"Invalid settings: LOAD_SHEDDING. Valid settings:"
                        + " SOC_MAX, SOC_MIN, SOC_RSV_MAX,"
                        + " SOC_RSV_MIN, A_CHA_MAX, A_DISCHA_MAX\"]";
        JsonNode jsonNodeActual =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String actual = jsonNodeActual.get("errorMsgs").toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetBatterySettings_happyCaseAccepted() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "testRcpn";
        Double aChaMax = 12.0;
        Double aDisChaMax = 7.0;
        Double socRsvMax = 98.0;
        Double socRsvMin = 2.0;
        Double socMax = 99.0;
        Double socMin = 1.0;

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(aChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_DISCHA_MAX)
                                                .value(aDisChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MAX)
                                                .value(socRsvMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MIN)
                                                .value(socRsvMin)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(socMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(socMin)
                                                .build()))
                        .build();

        OdinResponse expectedOdin = new OdinResponse();
        expectedOdin.setId("a400a7bc-7dcc-40c8-b28b-448a3c87cbf3");
        BatterySettingResponseDto.OdinBatterySetting expectedSetting =
                BatterySettingResponseDto.OdinBatterySetting.builder()
                        .batterySetting(
                                BaseBatterySetting.builder()
                                        .aChaMax(aChaMax)
                                        .aDisChaMax(aDisChaMax)
                                        .socMax(socMax)
                                        .socMin(socMin)
                                        .socRsvMax(socRsvMax)
                                        .socRsvMin(socRsvMin)
                                        .build())
                        .rcpn(providedRcpn)
                        .build();
        expectedOdin.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(List.of(expectedSetting))
                                        .build())
                        .build());

        DeviceSettingUpdateResponse expected =
                DeviceSettingUpdateResponseConverter.toInstance(expectedOdin, testSystemId);

        when(deviceSettingsService.setDeviceSettings(
                        testSystemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        providedRcpn,
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        SystemType.ESS))
                .thenReturn(Mono.just(expected));

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + testSystemId
                                                        + "/batteries/"
                                                        + providedRcpn
                                                        + "/settings")
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        deviceSettingsUpdateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn();

        verify(deviceSettingsService, times(1))
                .setDeviceSettings(
                        testSystemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        providedRcpn,
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        SystemType.ESS);
        assertEquals(expected, mvcResult.getAsyncResult());
    }

    @Test
    public void testSetBatterySettings_nullRequestBodyBadRequest() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "testRcpn";

        // Action & Assert
        mvc.perform(
                        patch(
                                        "/v1/systems/"
                                                + testSystemId
                                                + "/batteries/"
                                                + providedRcpn
                                                + "/settings")
                                .content(objectMapper.writeValueAsString(null))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(odinService, times(0))
                .setBatterySettings(testSystemId, null, callerId, userId, List.of(providedRcpn));
    }

    @Test
    public void testSetBatterySettings_missingSystemIdNotFound() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "testRcpn";
        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder().build();

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/batteries/testRcpn/settings")
                                .content(
                                        objectMapper.writeValueAsString(
                                                deviceSettingsUpdateRequest))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(odinService, times(0))
                .setBatterySettings(
                        testSystemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        List.of(providedRcpn));
    }

    @Test
    public void testSetBatterySettings_outOfRangeSocMax_throwsBadRequest() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "testRcpn";
        Double socMax = 101.0;

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(socMax)
                                                .build()))
                        .build();

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + testSystemId
                                                        + "/batteries/"
                                                        + providedRcpn
                                                        + "/settings")
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        deviceSettingsUpdateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        String expected = "[\"SOC_MAX must be between 0 and 100.\"]";
        JsonNode jsonNodeActual =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String actual = String.valueOf(jsonNodeActual.get("errorMsgs"));

        assertEquals(expected, actual);
    }

    @Test
    public void testSetBatterySettings_outOfRangeSocMin_throwsBadRequest() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "testRcpn";
        Double socMin = -1.0;

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(socMin)
                                                .build()))
                        .build();

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + testSystemId
                                                        + "/batteries/"
                                                        + providedRcpn
                                                        + "/settings")
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        deviceSettingsUpdateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        String expected = "[\"SOC_MIN must be between 0 and 100.\"]";
        JsonNode jsonNodeActual =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String actual = String.valueOf(jsonNodeActual.get("errorMsgs"));

        assertEquals(expected, actual);
    }

    // =======================================================================================================
    //   SET ALL BATTERIES SETTINGS
    // =======================================================================================================

    @Test
    public void testSetAllBatteriesSettings_SettingNotEnabled() throws Exception {
        // Arrange
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.LOAD_SHEDDING)
                                                .value(10.2)
                                                .build()))
                        .build();

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/batteries/settings")
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        deviceSettingsUpdateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        verify(odinService, times(0)).setBatterySettings(any(), any(), any(), any(), any());

        String expected =
                "[\"Invalid settings: LOAD_SHEDDING. Valid settings:"
                        + " SOC_MAX, SOC_MIN, SOC_RSV_MAX,"
                        + " SOC_RSV_MIN, A_CHA_MAX, A_DISCHA_MAX\"]";
        JsonNode jsonNodeActual =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String actual = jsonNodeActual.get("errorMsgs").toString();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetAllBatteriesSettings_happyCaseAccepted() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String returnRcpn = "testRcpn";
        String id = "a400a7bc-7dcc-40c8-b28b-448a3c87cbf3";
        Double aChaMax = 12.0;
        Double aDisChaMax = 7.0;
        Double socRsvMax = 98.0;
        Double socRsvMin = 2.0;
        Double socMax = 99.0;
        Double socMin = 1.0;

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(aChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_DISCHA_MAX)
                                                .value(aDisChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MAX)
                                                .value(socRsvMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MIN)
                                                .value(socRsvMin)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(socMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(socMin)
                                                .build()))
                        .build();

        OdinResponse expectedOdin = new OdinResponse();
        expectedOdin.setId(id);
        BatterySettingResponseDto.OdinBatterySetting expectedSetting =
                BatterySettingResponseDto.OdinBatterySetting.builder()
                        .batterySetting(
                                BaseBatterySetting.builder()
                                        .aChaMax(aChaMax)
                                        .aDisChaMax(aDisChaMax)
                                        .socMax(socMax)
                                        .socMin(socMin)
                                        .socRsvMax(socRsvMax)
                                        .socRsvMin(socRsvMin)
                                        .build())
                        .rcpn(returnRcpn)
                        .build();
        expectedOdin.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(List.of(expectedSetting))
                                        .build())
                        .build());

        DeviceSettingUpdateResponse expected =
                DeviceSettingUpdateResponseConverter.toInstance(expectedOdin, testSystemId);
        when(deviceSettingsService.setDeviceSettings(
                        testSystemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        null,
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        SystemType.ESS))
                .thenReturn(Mono.just(expected));

        // Action & Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/batteries/settings")
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        deviceSettingsUpdateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn();

        verify(deviceSettingsService, times(1))
                .setDeviceSettings(
                        testSystemId,
                        deviceSettingsUpdateRequest,
                        callerId,
                        userId,
                        null,
                        RequestType.INSTANT_BATTERY_SETTINGS_PATCH,
                        SystemType.ESS);
        assertEquals(expected.systemId(), testSystemId);
        assertEquals(expected.updateId(), UUID.fromString(id));
        assertEquals(expected, mvcResult.getAsyncResult());
    }

    @Test
    public void testSetAllBatteriesSettings_nullRequestBodyBadRequest() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/" + testSystemId + "/batteries/settings")
                                .content(objectMapper.writeValueAsString(null))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(odinService, times(0))
                .setBatterySettings(testSystemId, null, callerId, userId, null);
    }

    @Test
    public void testSetAllBatteriesSettings_missingSystemIdNotFound() throws Exception {
        // Arrange
        UUID testSystemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder().build();

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems//batteries/settings")
                                .content(
                                        objectMapper.writeValueAsString(
                                                deviceSettingsUpdateRequest))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(odinService, times(0))
                .setBatterySettings(
                        testSystemId, deviceSettingsUpdateRequest, callerId, userId, null);
    }
}
