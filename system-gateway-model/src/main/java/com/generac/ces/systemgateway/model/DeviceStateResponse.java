package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.system.DeviceTypeOuterClass;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/**
 * Device State update response.
 *
 * @param updateId The ID representing the update initiated by this API request.
 * @param systemId The ID of the system the affected device belongs to.
 * @param deviceId The ID of the device undergoing state change.
 * @param enable The updated boolean state of the device.
 * @param deviceType The type of the device.
 * @param eventSettingsChangedTime The time the update occurred.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record DeviceStateResponse(
        UUID updateId,
        UUID systemId,
        String deviceId,
        Boolean enable,
        DeviceTypeOuterClass.DeviceType deviceType,
        OffsetDateTime eventSettingsChangedTime) {}
