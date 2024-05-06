package com.generac.ces.systemgateway.model.system;

import javax.validation.constraints.NotEmpty;

/**
 * The request entity used to trigger a firmware update to a specific or latest version for a
 * specific system by its ID.
 *
 * @param updateVersion The version to which the system's firmware should be updated. Must exactly
 *     match a valid firmware version or contain the special value "LATEST".
 * @param notes Any additional information the caller wishes to provide with the update request.
 *     This data will be stored as part of the firmware update history by the Firmware Management
 *     service.
 */
public record SystemFirmwareUpdateRequest(@NotEmpty String updateVersion, String notes) {}
