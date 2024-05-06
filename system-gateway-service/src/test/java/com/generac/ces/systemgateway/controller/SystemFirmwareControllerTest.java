package com.generac.ces.systemgateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.controller.system.SystemFirmwareController;
import com.generac.ces.systemgateway.exception.InvalidFirmwareForSystemException;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.system.SystemFirmwareUpdateRequest;
import com.generac.ces.systemgateway.service.system.BlueMarlinFirmwareService;
import com.generac.ces.systemgateway.service.system.SystemFirmwareUpdateRequestMetadata;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = SystemFirmwareController.class)
@AutoConfigureMockMvc
class SystemFirmwareControllerTest {
    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean
    @SuppressWarnings("unused")
    private BlueMarlinFirmwareService firmwareService;

    @Test
    void updateSystemFirmwareDetails_validRequest_expectedServiceLogicCalled() throws Exception {
        // Arrange
        var expectedSystemId = UUID.randomUUID();
        var expectedUpdateVersion = "1.2.3";
        var expectedNotes = "hello world";
        var request = new SystemFirmwareUpdateRequest(expectedUpdateVersion, expectedNotes);
        var expectedUserId = UUID.randomUUID();
        var expectedApplicationName = "pwrfleet";
        var expectedRequestMetadata =
                new SystemFirmwareUpdateRequestMetadata(expectedUserId, expectedApplicationName);

        // Act
        mvc.perform(
                        patch("/v1/systems/%s/firmware".formatted(expectedSystemId))
                                .header(
                                        CallerMetadataRequestHeaders.USER_ID.getHeaderName(),
                                        expectedUserId)
                                .header(
                                        CallerMetadataRequestHeaders.APPLICATION_NAME
                                                .getHeaderName(),
                                        expectedApplicationName)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        // Assert
        /*
        Comment out firmware version validation expectation until firmware-management endpoint to support this exists.
        verify(firmwareService, times(1))
                .throwIfFirmwareVersionIsInvalidForSystem(expectedSystemId, expectedUpdateVersion);
        */
        verify(firmwareService, times(1))
                .updateSystemFirmwareVersion(
                        expectedSystemId,
                        expectedUpdateVersion,
                        expectedNotes,
                        expectedRequestMetadata);
    }

    @Test
    void updateSystemFirmwareDetails_invalidSystemId_updateLogicIsNotCalled() throws Exception {
        // Arrange
        var expectedSystemId = "not a uuid";
        var expectedUpdateVersion = "1.2.3";
        var expectedNotes = "hello world";
        var request = new SystemFirmwareUpdateRequest(expectedUpdateVersion, expectedNotes);
        var expectedUserId = UUID.randomUUID();
        var expectedApplicationName = "pwrfleet";

        // Act
        mvc.perform(
                        patch("/v1/systems/%s/firmware".formatted(expectedSystemId))
                                .header(
                                        CallerMetadataRequestHeaders.USER_ID.getHeaderName(),
                                        expectedUserId)
                                .header(
                                        CallerMetadataRequestHeaders.APPLICATION_NAME
                                                .getHeaderName(),
                                        expectedApplicationName)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(firmwareService, never()).throwIfFirmwareVersionIsInvalidForSystem(any(), any());
        verify(firmwareService, never()).updateSystemFirmwareVersion(any(), any(), any(), any());
    }

    @Disabled(
            "Cannot verify invalid version validation logic until firmware-management endpoint to"
                    + " support this exists.")
    @Test
    void updateSystemFirmwareDetails_invalidUpdateVersion_updateLogicIsNotCalled()
            throws Exception {
        // Arrange
        var expectedSystemId = UUID.randomUUID();
        var expectedUpdateVersion = "1.2.3";
        var expectedNotes = "hello world";
        var request = new SystemFirmwareUpdateRequest(expectedUpdateVersion, expectedNotes);
        var expectedUserId = UUID.randomUUID();
        var expectedApplicationName = "pwrfleet";

        doThrow(new InvalidFirmwareForSystemException(expectedSystemId, expectedUpdateVersion))
                .when(firmwareService)
                .throwIfFirmwareVersionIsInvalidForSystem(expectedSystemId, expectedUpdateVersion);

        // Act
        mvc.perform(
                        patch("/v1/systems/%s/firmware".formatted(expectedSystemId))
                                .header(
                                        CallerMetadataRequestHeaders.USER_ID.getHeaderName(),
                                        expectedUserId)
                                .header(
                                        CallerMetadataRequestHeaders.APPLICATION_NAME
                                                .getHeaderName(),
                                        expectedApplicationName)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(firmwareService, times(1)).throwIfFirmwareVersionIsInvalidForSystem(any(), any());
        verify(firmwareService, never()).updateSystemFirmwareVersion(any(), any(), any(), any());
    }

    @Test
    void updateSystemFirmwareDetails_missingRequestMetadata_updateLogicIsNotCalled()
            throws Exception {
        // Arrange
        var expectedSystemId = UUID.randomUUID();
        var expectedUpdateVersion = "1.2.3";
        var expectedNotes = "hello world";
        var request = new SystemFirmwareUpdateRequest(expectedUpdateVersion, expectedNotes);

        // Act
        mvc.perform(
                        patch("/v1/systems/%s/firmware".formatted(expectedSystemId))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(firmwareService, never()).throwIfFirmwareVersionIsInvalidForSystem(any(), any());
        verify(firmwareService, never()).updateSystemFirmwareVersion(any(), any(), any(), any());
    }

    @Test
    void updateSystemFirmwareDetails_missingUpdateVersion_updateLogicIsNotCalled()
            throws Exception {
        // Arrange
        var expectedSystemId = UUID.randomUUID();
        var expectedNotes = "hello world";
        var request = new SystemFirmwareUpdateRequest(null, expectedNotes);
        var expectedUserId = UUID.randomUUID();
        var expectedApplicationName = "pwrfleet";

        // Act
        mvc.perform(
                        patch("/v1/systems/%s/firmware".formatted(expectedSystemId))
                                .header(
                                        CallerMetadataRequestHeaders.USER_ID.getHeaderName(),
                                        expectedUserId)
                                .header(
                                        CallerMetadataRequestHeaders.APPLICATION_NAME
                                                .getHeaderName(),
                                        expectedApplicationName)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(firmwareService, never()).throwIfFirmwareVersionIsInvalidForSystem(any(), any());
        verify(firmwareService, never()).updateSystemFirmwareVersion(any(), any(), any(), any());
    }
}
