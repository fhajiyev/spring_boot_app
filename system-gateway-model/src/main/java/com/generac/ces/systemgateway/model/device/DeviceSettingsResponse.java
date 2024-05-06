package com.generac.ces.systemgateway.model.device;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceSettingsResponse(String hostDeviceId, List<DeviceSettings> devices) {}
