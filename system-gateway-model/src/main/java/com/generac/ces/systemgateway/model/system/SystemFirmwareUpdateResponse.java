package com.generac.ces.systemgateway.model.system;

import java.util.UUID;

/**
 * Summaries the result of a successful system firmware update request.
 *
 * @param systemId The ID of the system being updated.
 * @param updateId The ID representing the particular update initiated by this API request.
 * @param updateVersion The specific version to which the system is being updated.
 * @param message If the update failed, a message which may contain extra details about the failure.
 */
public record SystemFirmwareUpdateResponse(
        UUID systemId, UUID updateId, String updateVersion, String message) {}
