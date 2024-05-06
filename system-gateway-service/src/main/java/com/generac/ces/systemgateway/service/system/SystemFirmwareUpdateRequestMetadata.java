package com.generac.ces.systemgateway.service.system;

import java.util.UUID;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Details which are required to be provided to the Firmware Management service, but are not part of
 * the entities (request bodies) sent to that service.
 *
 * @param initiatingUserId The user ID who has initiated a particular action.
 * @param initiatingApplicationName The name of the application a user has used to initiate a
 *     particular action.
 */
public record SystemFirmwareUpdateRequestMetadata(
        @NotNull UUID initiatingUserId, @NotEmpty String initiatingApplicationName) {}
