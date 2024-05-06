package com.generac.ces.systemgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.controller.device.DeviceController;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.DeviceStateRequest;
import com.generac.ces.systemgateway.model.DeviceStateResponse;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.service.system.DeviceSettingsService;
import com.generac.ces.systemgateway.service.system.DevicesService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
@WebMvcTest(controllers = DeviceController.class)
@AutoConfigureMockMvc
public class DeviceControllerTest {
    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean private DeviceSettingsService deviceSettingsService;
    @MockBean private DevicesService devicesService;

    @BeforeAll
    public void beforeAll() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // =======================================================================================================
    //   SET DEVICE STATUS
    // =======================================================================================================
    @Test
    public void testSetDeviceEnable_happyCaseAccepted() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String deviceRcpn = "test_device_rcpn";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        UUID uuid = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;

        DeviceStateResponse deviceStateResponse =
                new DeviceStateResponse(
                        uuid,
                        systemId,
                        deviceRcpn,
                        true,
                        DeviceTypeOuterClass.DeviceType.BATTERY,
                        OffsetDateTime.now());
        DeviceStateRequest deviceStateRequest = new DeviceStateRequest();
        deviceStateRequest.setEnable(true);

        Mockito.when(
                        deviceSettingsService.setDeviceState(
                                systemId,
                                deviceRcpn,
                                callerId,
                                userId,
                                deviceStateRequest,
                                systemType))
                .thenReturn(Mono.just(deviceStateResponse));

        // Action & Assert
        MvcResult result =
                mvc.perform(
                                patch(
                                                String.format(
                                                        "/v1/systems/%s/devices/%s/state",
                                                        systemId, deviceRcpn))
                                        .queryParam("blocking", "true")
                                        .queryParam("instantControl", "true")
                                        .content(
                                                objectMapper.writeValueAsString(deviceStateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();

        String resultBody = objectMapper.writeValueAsString(result.getAsyncResult());
        JsonObject jsonResult = new Gson().fromJson(resultBody, JsonObject.class);
        assertEquals(
                deviceStateResponse.updateId().toString(),
                jsonResult.get("updateId").getAsString());
        assertEquals(
                deviceStateResponse.systemId().toString(),
                jsonResult.get("systemId").getAsString());

        verify(deviceSettingsService, times(1))
                .setDeviceState(
                        systemId, deviceRcpn, callerId, userId, deviceStateRequest, systemType);
    }

    @Test
    public void testSetDeviceDisable_happyCaseAccepted() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String deviceRcpn = "test_device_rcpn";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        UUID uuid = UUID.randomUUID();
        SystemType systemType = SystemType.ESS;

        DeviceStateResponse deviceStateResponse =
                new DeviceStateResponse(
                        uuid,
                        systemId,
                        deviceRcpn,
                        false,
                        DeviceTypeOuterClass.DeviceType.BATTERY,
                        OffsetDateTime.now());
        DeviceStateRequest deviceStateRequest = new DeviceStateRequest();
        deviceStateRequest.setEnable(false);
        Mockito.when(
                        deviceSettingsService.setDeviceState(
                                systemId,
                                deviceRcpn,
                                callerId,
                                userId,
                                deviceStateRequest,
                                systemType))
                .thenReturn(Mono.just(deviceStateResponse));

        // Action & Assert
        MvcResult result =
                mvc.perform(
                                patch(
                                                String.format(
                                                        "/v1/systems/%s/devices/%s/state",
                                                        systemId, deviceRcpn))
                                        .queryParam("blocking", "true")
                                        .queryParam("instantControl", "true")
                                        .content(
                                                objectMapper.writeValueAsString(deviceStateRequest))
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .CALLER_ID_HEADER_NAME,
                                                callerId)
                                        .header(
                                                CallerMetadataRequestHeaders.Constants
                                                        .USER_ID_HEADER_NAME,
                                                userId)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();

        String resultBody = objectMapper.writeValueAsString(result.getAsyncResult());
        JsonObject jsonResult = new Gson().fromJson(resultBody, JsonObject.class);
        assertEquals(
                deviceStateResponse.updateId().toString(),
                jsonResult.get("updateId").getAsString());
        assertEquals(
                deviceStateResponse.systemId().toString(),
                jsonResult.get("systemId").getAsString());

        verify(deviceSettingsService, times(1))
                .setDeviceState(
                        systemId, deviceRcpn, callerId, userId, deviceStateRequest, systemType);
    }

    @Test
    public void testsetDeviceState_nullRequestBodyBadRequest() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String deviceRcpn = "test_device_rcpn";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        SystemType systemType = SystemType.ESS;

        // Action & Assert
        mvc.perform(
                        patch(
                                        String.format(
                                                "/v1/systems/%s/devices/%s/state",
                                                systemId, deviceRcpn))
                                .queryParam("blocking", "true")
                                .queryParam("instantControl", "true")
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

        verify(deviceSettingsService, times(0))
                .setDeviceState(systemId, deviceRcpn, callerId, userId, null, systemType);
    }

    // =======================================================================================================
    //   GET DEVICE STATUS
    // =======================================================================================================

    @Test
    public void testGetDeviceState_Success() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceRcpn = "test_device_rcpn";
        DeviceStateResponse dummyResponse =
                new DeviceStateResponse(
                        UUID.randomUUID(),
                        systemId,
                        deviceRcpn,
                        true,
                        DeviceTypeOuterClass.DeviceType.BATTERY,
                        OffsetDateTime.now());

        given(deviceSettingsService.getDeviceState(systemId, deviceRcpn))
                .willReturn(Mono.just(dummyResponse));

        // Action & Assert
        mvc.perform(
                        get(String.format("/v1/systems/%s/devices/%s/state", systemId, deviceRcpn))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(deviceSettingsService, times(1)).getDeviceState(systemId, deviceRcpn);
    }

    @Test
    public void testGetDeviceState_NotFound() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceRcpn = "non_existent_device_rcpn";

        given(deviceSettingsService.getDeviceState(systemId, deviceRcpn))
                .willThrow(ResourceNotFoundException.class);

        // Action & Assert
        mvc.perform(
                        get(String.format("/v1/systems/%s/devices/%s/state", systemId, deviceRcpn))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(deviceSettingsService, times(1)).getDeviceState(systemId, deviceRcpn);
    }
}
