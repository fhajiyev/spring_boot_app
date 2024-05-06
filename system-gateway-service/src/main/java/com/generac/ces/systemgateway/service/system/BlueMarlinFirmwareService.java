package com.generac.ces.systemgateway.service.system;

import com.generac.ces.systemgateway.exception.InvalidFirmwareForSystemException;
import com.generac.ces.systemgateway.model.system.SystemFirmwareUpdateResponse;
import com.generac.ces.systemgateway.service.FirmwareManagementApiClient;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BlueMarlinFirmwareService {
    private final FirmwareManagementApiClient firmwareApiClient;

    public BlueMarlinFirmwareService(FirmwareManagementApiClient firmwareApiClient) {
        this.firmwareApiClient = firmwareApiClient;
    }

    private List<String> getAvailableFirmwareUpdateVersions(UUID systemId) {
        return firmwareApiClient.getAvailableFirmwareUpdateVersions();
    }

    public SystemFirmwareUpdateResponse updateSystemFirmwareVersion(
            UUID systemId,
            String version,
            String notes,
            SystemFirmwareUpdateRequestMetadata requestMetadata) {
        var firmwareManagementUpdateResponse =
                firmwareApiClient.updateSystemFirmwareVersion(
                        systemId,
                        version,
                        notes,
                        requestMetadata.initiatingUserId(),
                        requestMetadata.initiatingApplicationName());
        return new SystemFirmwareUpdateResponse(
                systemId,
                UUID.fromString(firmwareManagementUpdateResponse.getUpdateId()),
                version,
                firmwareManagementUpdateResponse.getMessage());
    }

    public void throwIfFirmwareVersionIsInvalidForSystem(UUID systemId, String updateVersion) {
        var availableFirmwareVersions = getAvailableFirmwareUpdateVersions(systemId);

        assert availableFirmwareVersions != null;
        var isValidVersionForSystem = availableFirmwareVersions.contains(updateVersion);
        if (!isValidVersionForSystem) {
            throw new InvalidFirmwareForSystemException(systemId, updateVersion);
        }
    }
}
