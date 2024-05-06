package com.generac.ces.systemgateway.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.controller.system.SystemModeController;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateResponse;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.generac.ces.systemgateway.service.system.SystemModeService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = SystemModeController.class)
@AutoConfigureMockMvc
public class SystemModeControllerTest {

    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean private OdinService odinService;

    @MockBean private EssSystemService essSystemService;

    @MockBean private EssDataProviderService essDataProviderService;

    @MockBean private SystemModeService systemModeService;

    // =======================================================================================================
    //   SET ACTIVE SYSTEM MODES
    // =======================================================================================================
    @Test
    public void testSetActiveSystemModes_controllerRoutesSuccessfully() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELF_SUPPLY));

        Mockito.when(
                        systemModeService.setActiveSystemModes(
                                systemId, dto, SystemType.ESS, callerId, userId))
                .thenReturn(Mono.just(ActiveSystemModeUpdateResponse.builder().build()));

        // Action & Assert
        mvc.perform(
                        patch(String.format("/v1/systems/%s/active-modes", systemId))
                                .content(objectMapper.writeValueAsString(dto))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());

        verify(systemModeService, times(1))
                .setActiveSystemModes(systemId, dto, SystemType.ESS, callerId, userId);
    }

    @Test
    public void testSetActiveSystemModes_nullDtoReturnsBadRequest() throws Exception {
        // Arrange
        String callerId = "pwrview";
        String userId = "dummyUserId";
        UUID systemId = UUID.randomUUID();

        // Action & Assert
        mvc.perform(
                        patch(String.format("/v1/systems/%s/active-modes", systemId))
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
    }

    @Test
    public void testSetActiveSystemModes_nullActiveSysModesListReturnsBadRequest()
            throws Exception {
        // Arrange
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto = new ActiveSystemModeUpdateRequest(List.of());

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/test_system_id/active-modes")
                                .content(objectMapper.writeValueAsString(dto))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSetActiveSystemModes_emptyActiveSysModesListBadRequest() throws Exception {
        // Arrange
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto = new ActiveSystemModeUpdateRequest(List.of());

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/test_system_id/active-modes")
                                .content(objectMapper.writeValueAsString(dto))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSetActiveSystemModes_invalidSysModeBadRequest() throws Exception {
        // Arrange
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SAFETY_SHUTDOWN));

        // Action & Assert
        mvc.perform(
                        patch("/v1/systems/test_system_id/active-modes")
                                .content(objectMapper.writeValueAsString(dto))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSetActiveSystemModes_noCallerIdHeader() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELF_SUPPLY));

        // Action & Assert
        mvc.perform(
                        patch(String.format("/v1/systems/%s/active-modes", systemId))
                                .content(objectMapper.writeValueAsString(dto))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(systemModeService, times(0))
                .setActiveSystemModes(systemId, dto, SystemType.ESS, callerId, userId);
    }

    @Test
    public void testSetActiveSystemModes_noUserIdHeader() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELF_SUPPLY));

        // Action & Assert
        mvc.perform(
                        patch(String.format("/v1/systems/%s/active-modes", systemId))
                                .content(objectMapper.writeValueAsString(dto))
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(systemModeService, times(0))
                .setActiveSystemModes(systemId, dto, SystemType.ESS, callerId, userId);
    }

    @Test
    public void testSetActiveSystemModes_noHeaders() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "pwrview";
        String userId = "dummyUserId";
        ActiveSystemModeUpdateRequest dto =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELF_SUPPLY));

        // Action & Assert
        mvc.perform(
                        patch(String.format("/v1/systems/%s/active-modes", systemId))
                                .content(objectMapper.writeValueAsString(dto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(systemModeService, times(0))
                .setActiveSystemModes(systemId, dto, SystemType.ESS, callerId, userId);
    }

    // =======================================================================================================
    //   POST SYSTEM CONTROL MESSAGE FOR SYSTEM MODE
    // =======================================================================================================

    @Test
    public void testPostSysModeControl() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "pwrview";
        String userId = "dummyUserId";
        SystemType systemType = SystemType.ESS;
        SystemModeUpdateRequest request = dummyValidSystemModeUpdateRequest();
        Mockito.when(
                        systemModeService.updateSystemMode(
                                systemType, systemId, request, callerId, userId, true))
                .thenReturn(Mono.empty());

        // Action & Assert
        ResultActions actions =
                mvc.perform(
                        patch(String.format("/v1/systems/%s/mode", systemId))
                                .content(objectMapper.writeValueAsString(request))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON));

        actions.andExpect(status().isOk());
        verify(systemModeService, times(1))
                .updateSystemMode(systemType, systemId, request, callerId, userId, true);
    }

    @Test
    public void testPostSysModeControl_missingCallerId() throws Exception {
        // missing callerId
        String userId = "dummyUserId";
        mvc.perform(
                        patch(String.format("/v1/systems/%s/mode", UUID.randomUUID()))
                                .content(
                                        objectMapper.writeValueAsString(
                                                dummyValidSystemModeUpdateRequest()))
                                .header(
                                        CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME,
                                        userId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPostSysModeControl_missingUserId() throws Exception {
        // missing userId
        String callerId = "pwrview";
        mvc.perform(
                        patch(String.format("/v1/systems/%s/mode", UUID.randomUUID()))
                                .content(
                                        objectMapper.writeValueAsString(
                                                dummyValidSystemModeUpdateRequest()))
                                .header(
                                        CallerMetadataRequestHeaders.Constants
                                                .CALLER_ID_HEADER_NAME,
                                        callerId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPostSysModeControl_isBadRequest() throws Exception {
        mvc.perform(
                        patch(String.format("/v1/systems/%s/mode", UUID.randomUUID()))
                                .content(
                                        objectMapper.writeValueAsString(
                                                SystemModeUpdateRequest.builder().build()))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private SystemModeUpdateRequest dummyValidSystemModeUpdateRequest() {
        return SystemModeUpdateRequest.builder()
                .startTime(OffsetDateTime.now())
                .duration(5L)
                .systemMode(SystemMode.SELL)
                .build();
    }

    // =======================================================================================================
    //   GET SYSTEM MODE
    // =======================================================================================================
    @Test
    public void testGetSystemMode() throws Exception {
        // Arrange
        UUID systemId = UUID.randomUUID();

        Mockito.when(systemModeService.getSystemMode(systemId)).thenReturn(Mono.empty());

        // Action & Assert
        ResultActions actions =
                mvc.perform(
                        get(String.format("/v1/systems/%s/mode", systemId))
                                .contentType(MediaType.APPLICATION_JSON));

        actions.andExpect(status().isOk());
        verify(systemModeService, times(1)).getSystemMode(systemId);
    }
}
