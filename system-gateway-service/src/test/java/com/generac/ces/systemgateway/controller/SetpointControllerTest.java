package com.generac.ces.systemgateway.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.controller.setpoint.SetpointController;
import com.generac.ces.systemgateway.model.ExportLimitRequest;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.service.odin.OdinService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = SetpointController.class)
@AutoConfigureMockMvc
public class SetpointControllerTest {

    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean private OdinService odinService;

    // =======================================================================================================
    //   SET EXPORT LIMIT
    // =======================================================================================================
    @Test
    public void testSetExportLimit_happyCaseAccepted() throws Exception {
        // Arrange
        String testSystemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String testInverterId = "00010007A42A";
        ExportLimitRequest request = new ExportLimitRequest();
        request.setExportOverride(Boolean.TRUE);
        request.setInverterId(testInverterId);

        when(odinService.setExportLimit(testSystemId, request, callerId))
                .thenReturn(Mono.just(new OdinResponse()));

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/" + testSystemId + "/export-limit")
                                .content(objectMapper.writeValueAsString(request))
                                .header("x-caller-company-id", callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(odinService, times(1)).setExportLimit(testSystemId, request, callerId);
    }

    @Test
    public void testSetExportLimit_nullExportOverrideBadRequest() throws Exception {
        // Arrange
        String testSystemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String testInverterId = "00010007A42A";
        ExportLimitRequest request = new ExportLimitRequest();
        request.setInverterId(testInverterId);

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/" + testSystemId + "/export-limit")
                                .content(objectMapper.writeValueAsString(request))
                                .header("x-caller-company-id", callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(odinService, times(0)).setExportLimit(testSystemId, request, callerId);
    }

    @Test
    public void testSetExportLimit_nullInverterIdBadRequest() throws Exception {
        // Arrange
        String testSystemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        ExportLimitRequest request = new ExportLimitRequest();
        request.setExportOverride(Boolean.TRUE);

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/" + testSystemId + "/export-limit")
                                .content(objectMapper.writeValueAsString(request))
                                .header("x-caller-company-id", callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(odinService, times(0)).setExportLimit(testSystemId, request, callerId);
    }

    @Test
    public void testSetExportLimit_missingSystemIdNotFound() throws Exception {
        // Arrange
        String testSystemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String testInverterId = "00010007A42A";
        ExportLimitRequest request = new ExportLimitRequest();
        request.setExportOverride(Boolean.TRUE);
        request.setInverterId(testInverterId);

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems//export-limit")
                                .content(objectMapper.writeValueAsString(request))
                                .header("x-caller-company-id", callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(odinService, times(0)).setExportLimit(testSystemId, request, callerId);
    }

    @Test
    public void testSetExportLimit_missingRequestHeaderBadRequest() throws Exception {
        // Arrange
        String testSystemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String testInverterId = "00010007A42A";
        ExportLimitRequest request = new ExportLimitRequest();
        request.setExportOverride(Boolean.TRUE);
        request.setInverterId(testInverterId);

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/" + testSystemId + "/export-limit")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(odinService, times(0)).setExportLimit(testSystemId, request, null);
    }
}
