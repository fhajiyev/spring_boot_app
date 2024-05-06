package com.generac.ces.systemgateway.model.device;

import com.generac.ces.system.SystemFamilyOuterClass;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceListRequest {

    @Valid
    @NotEmpty(message = "systems is required")
    private List<DeviceListSystem> systems;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceListSystem {
        @NotEmpty(message = "SystemId is required")
        String systemId;

        @Valid SystemFamilyOuterClass.SystemFamily systemFamily;
    }
}
