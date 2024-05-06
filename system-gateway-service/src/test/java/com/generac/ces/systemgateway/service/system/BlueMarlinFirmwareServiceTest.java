package com.generac.ces.systemgateway.service.system;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generac.ces.bluemarlin.firmwaremanagement.dto.FirmwareUpdateForSystemHttpResponse;
import com.generac.ces.systemgateway.exception.InvalidFirmwareForSystemException;
import com.generac.ces.systemgateway.service.FirmwareManagementApiClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlueMarlinFirmwareServiceTest {
    private static final String INVALID_UPDATE_VERSION = "1.2.5";
    private static final List<String> VALID_UPDATE_VERSIONS = List.of("0.5.5", "0.9.1");
    private static final UUID SYSTEM_ID = UUID.randomUUID();
    private BlueMarlinFirmwareService firmwareService;

    @Mock private FirmwareManagementApiClient mockFirmwareApiClient;

    @BeforeEach
    public void setup() {
        firmwareService = new BlueMarlinFirmwareService(mockFirmwareApiClient);
    }

    @Test
    void updateSystemFirmwareVersion_validRequestParameters_expectedApiCallOccurs() {
        // Arrange
        var validSuccessResponse =
                new FirmwareUpdateForSystemHttpResponse(
                        UUID.randomUUID().toString(), null, null, null, null);
        when(mockFirmwareApiClient.updateSystemFirmwareVersion(any(), any(), any(), any(), any()))
                .thenReturn(validSuccessResponse);
        var expectedVersion = VALID_UPDATE_VERSIONS.get(0);
        var expectedNotes = "Hello world";
        var expectedUserId = UUID.randomUUID();
        var expectedApplicationName = "power feet";
        var expectedMetadata =
                new SystemFirmwareUpdateRequestMetadata(expectedUserId, expectedApplicationName);

        // Act
        firmwareService.updateSystemFirmwareVersion(
                SYSTEM_ID, expectedVersion, expectedNotes, expectedMetadata);

        // Assert
        verify(mockFirmwareApiClient, times(1))
                .updateSystemFirmwareVersion(
                        SYSTEM_ID,
                        expectedVersion,
                        expectedNotes,
                        expectedUserId,
                        expectedApplicationName);
    }

    @Test
    void
            throwIfFirmwareVersionIsInvalidForSystem_invalidVersion_methodThrowsInvalidFirmwareException() {
        // Arrange
        when(mockFirmwareApiClient.getAvailableFirmwareUpdateVersions())
                .thenReturn(VALID_UPDATE_VERSIONS);

        // Act / Assert
        assertThrows(
                InvalidFirmwareForSystemException.class,
                () ->
                        firmwareService.throwIfFirmwareVersionIsInvalidForSystem(
                                SYSTEM_ID, INVALID_UPDATE_VERSION));
    }

    @Test
    void throwIfFirmwareVersionIsInvalidForSystem_validVersion_noExceptionThrown() {
        // Arrange
        when(mockFirmwareApiClient.getAvailableFirmwareUpdateVersions())
                .thenReturn(VALID_UPDATE_VERSIONS);

        // Act / Assert
        assertDoesNotThrow(
                () ->
                        firmwareService.throwIfFirmwareVersionIsInvalidForSystem(
                                SYSTEM_ID, VALID_UPDATE_VERSIONS.get(1)));
    }
}
