package com.generac.ces.systemgateway.model.device;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceSettings(
        String deviceId,
        DeviceTypeOuterClass.DeviceType deviceType,
        List<DeviceSettingsMetadata> settings) {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DeviceSettingsMetadata(
            DeviceSetting name,
            String label,
            Double value,
            MetadataConstraints constraints,
            String type,
            String unit,
            String description,
            OffsetDateTime updatedTimestampUtc) {

        @Builder
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record MetadataConstraints(
                Double minValue,
                Double maxValue,
                List<?> allowedValues,
                List<DeviceSetting> lessOrEqualThan,
                List<DeviceSetting> greaterOrEqualThan) {}
    }
}
