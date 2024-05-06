package com.generac.ces.systemgateway.model.device;

import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record DeviceSettingsUpdateRequest(@NotEmpty List<DeviceSettingChange> settings) {

    @Builder
    public record DeviceSettingChange(DeviceSetting name, Double value) {}
}
