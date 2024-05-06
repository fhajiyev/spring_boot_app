package com.generac.ces.systemgateway.model.device;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * Device Setting update response.
 *
 * @param updateId The ID representing the update initiated by this API request.
 * @param systemId The ID of the system the affected device belongs to.
 * @param devices The updated devices of the affected device.
 * @param updatedTimestampUtc The timestamp of the update.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceSettingUpdateResponse(
        UUID updateId, UUID systemId, List<Device> devices, OffsetDateTime updatedTimestampUtc) {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Device(String deviceId, List<DeviceSettingChange> settings) {

        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record DeviceSettingChange(DeviceSetting name, Double value) {}
    }
}
