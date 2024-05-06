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
import com.generac.ces.system.DeviceTypeOuterClass.DeviceType;
import com.generac.ces.systemgateway.controller.device.PVLController;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettings;
import com.generac.ces.systemgateway.model.device.DeviceSettingsResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.system.DeviceSettingsService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
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
@WebMvcTest(controllers = PVLController.class)
@AutoConfigureMockMvc
public class PvlControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean private DeviceSettingsService deviceSettingsService;
    @MockBean private EssSystemService essSystemService;
    @MockBean private EssDataProviderService essDataProviderService;

    // =======================================================================================================
    //   GET PVL SETTINGS
    // =======================================================================================================

    @Test
    public void getPvlSettings_returnsOk() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String deviceId = "000100078C8D";
        String hostRcpn = "00010007A42A";
        String settingLabel = "Enable/Disable PVLs";
        Double settingValue = 1.0;
        String metadataType = "BOOLEAN";
        String settingDescription =
                "This setting allows users to remotely disable and re-enable pvls";
        DeviceSettings.DeviceSettingsMetadata deviceMetadata =
                DeviceSettings.DeviceSettingsMetadata.builder()
                        .name(DeviceSetting.PVLINK_STATE)
                        .label(settingLabel)
                        .value(settingValue)
                        .type(metadataType)
                        .description(settingDescription)
                        .build();

        DeviceSettings pvlMetadata =
                new DeviceSettings(deviceId, DeviceType.PVLINK, List.of(deviceMetadata));

        DeviceSettingsResponse mockResponse =
                DeviceSettingsResponse.builder()
                        .hostDeviceId(hostRcpn)
                        .devices(List.of(pvlMetadata))
                        .build();

        when(essSystemService.getPvlSettings(systemId)).thenReturn(Mono.just(mockResponse));

        // Action and Assert
        MvcResult mvcResult =
                mvc.perform(get("/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/pvl/settings"))
                        .andExpect(status().isOk())
                        .andReturn();

        verify(essSystemService, times(1)).getPvlSettings(systemId);

        String expected = String.valueOf(Mono.just(mockResponse).block());
        String actual = mvcResult.getAsyncResult().toString();

        assertEquals(expected, actual);
    }

    @Test
    public void getPvlSettings_noSystemIdFound() throws Exception {
        mvc.perform(get("/v1/systems/pvl/settings")).andExpect(status().isNotFound());

        verify(essSystemService, times(0)).getSystemBySystemId(null);
        verify(essSystemService, times(0)).getPvlSettings(null);
    }

    // =======================================================================================================
    //   PATCH PVL SETTINGS
    // =======================================================================================================
    @Test
    public void setPvlSettings_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "pwrfleet";
        String userId = "dummyUserId";
        String pvLinkRcpn = "00010033001F";

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

        when(deviceSettingsService.setDeviceSettings(
                        any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(dummyResponse()));

        // Action and Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + systemId
                                                        + "/pvlink/"
                                                        + pvLinkRcpn
                                                        + "/settings")
                                        .queryParam("pvLinkRcpn", pvLinkRcpn)
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

        // Verify that the service method was called
        verify(deviceSettingsService, times(1))
                .setDeviceSettings(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void setPvlSettings_ValidRequest_InvalidEnum_ReturnsUnknownName() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "pwrfleet";
        String userId = "dummyUserId";
        String pvLinkRcpn = "00010033001F";

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

        String jsonResponse =
                "{ \"updateId\": \"fae7d4a7-a74c-418b-a61a-433d92747358\", \"systemId\":"
                    + " \"fae7d4a7-a74c-418b-a61a-433d92747358\", \"devices\": [ { \"deviceId\":"
                    + " \"dummyDeviceId\", \"settings\": [ { \"name\": \"SOME_INVALID_ENUM\","
                    + " \"value\": 1.0 }, { \"name\": \"ENABLE_PVRSS\", \"value\": 1.0 } ] } ],"
                    + " \"updatedTimestampUtc\": \"2023-12-19T10:30:00Z\" }";

        when(deviceSettingsService.setDeviceSettings(
                        any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(
                        Mono.just(
                                objectMapper.readValue(
                                        jsonResponse, DeviceSettingUpdateResponse.class)));

        // Action and Assert
        int mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + systemId
                                                        + "/pvlink/"
                                                        + pvLinkRcpn
                                                        + "/settings")
                                        .queryParam("pvLinkRcpn", pvLinkRcpn)
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
                        .andReturn()
                        .getResponse()
                        .getContentLength();

        // Verify that the service method was called
        verify(deviceSettingsService, times(1))
                .setDeviceSettings(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void setPvlSettings_InvalidEnum_ReturnsBadRequest() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "pwrfleet";
        String userId = "dummyUserId";
        String pvLinkRcpn = "00010033001F";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(100.0)
                                                .build()))
                        .build();

        when(deviceSettingsService.setDeviceSettings(
                        any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(dummyResponse()));

        // Action and Assert
        MvcResult mvcResult =
                mvc.perform(
                                patch(
                                                "/v1/systems/"
                                                        + systemId
                                                        + "/pvlink/"
                                                        + pvLinkRcpn
                                                        + "/settings")
                                        .queryParam("pvLinkRcpn", pvLinkRcpn)
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

        verify(deviceSettingsService, times(0))
                .setDeviceSettings(any(), any(), any(), any(), any(), any(), any());
        String expected =
                "[\"Invalid settings: SOC_MAX. Valid settings: VIN_STARTUP, ENABLE_PVRSS,"
                        + " NUM_STRING, PLM_CHANNEL, SNAP_RS_INSTALLED_CNT,"
                        + " SNAP_RS_DETECTED_CNT, OVERRIDE_PVRSS\"]";
        JsonNode jsonNodeActual =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String actual = jsonNodeActual.get("errorMsgs").toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void setPvlSettings_MissingCallerId_ReturnsBadRequest() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String userId = "dummyUserId";
        String pvLinkRcpn = "00010033001F";

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

        // Action and Assert
        mvc.perform(
                        patch("/v1/systems/" + systemId + "/pvlink/" + pvLinkRcpn + "/settings")
                                .content(
                                        objectMapper.writeValueAsString(
                                                deviceSettingsUpdateRequest))
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(deviceSettingsService, times(0))
                .setDeviceSettings(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void setPvlSettings_MissingUserId_ReturnsBadRequest() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "pwrfleet";
        String pvLinkRcpn = "00010033001F";

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

        // Action and Assert
        mvc.perform(
                        patch("/v1/systems/" + systemId + "/pvlink/" + pvLinkRcpn + "/settings")
                                .content(
                                        objectMapper.writeValueAsString(
                                                deviceSettingsUpdateRequest))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(deviceSettingsService, times(0))
                .setDeviceSettings(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void setInverterSettings_EmptySettingsList_ReturnsBadRequest() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "pwrfleet";
        String userId = "dummyUserId";
        String pvLinkRcpn = "00010033001F";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder().settings(Collections.emptyList()).build();

        // Action and Assert
        mvc.perform(
                        patch("/v1/systems/" + systemId + "/pvlink/" + pvLinkRcpn + "/settings")
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
                .andExpect(status().isBadRequest());

        verify(deviceSettingsService, times(0))
                .setDeviceSettings(any(), any(), any(), any(), any(), any(), any());
    }

    private DeviceSettingUpdateResponse dummyResponse() {
        return DeviceSettingUpdateResponse.builder()
                .updateId(UUID.randomUUID())
                .systemId(UUID.randomUUID())
                .devices(
                        List.of(
                                DeviceSettingUpdateResponse.Device.builder()
                                        .deviceId("dummyDeviceId")
                                        .settings(
                                                List.of(
                                                        DeviceSettingUpdateResponse.Device
                                                                .DeviceSettingChange.builder()
                                                                .name(DeviceSetting.ENABLE_PVRSS)
                                                                .value(1.0)
                                                                .build(),
                                                        DeviceSettingUpdateResponse.Device
                                                                .DeviceSettingChange.builder()
                                                                .name(DeviceSetting.NUM_STRING)
                                                                .value(2.0)
                                                                .build(),
                                                        DeviceSettingUpdateResponse.Device
                                                                .DeviceSettingChange.builder()
                                                                .name(DeviceSetting.VIN_STARTUP)
                                                                .value(120.0)
                                                                .build()))
                                        .build()))
                .updatedTimestampUtc(OffsetDateTime.now())
                .build();
    }
}
