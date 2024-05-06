package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.OdinBaseInstantControlMessage;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OdinSystemModeInstantRequest {
    ControlMessage controlMessage;

    @Data
    @Builder
    public static class ControlMessage extends OdinBaseInstantControlMessage {
        private SystemMode systemMode;
    }

    public static OdinSystemModeInstantRequest toInstance(
            SystemModeUpdateRequest systemModeUpdateRequest) {
        return OdinSystemModeInstantRequest.builder()
                .controlMessage(
                        ControlMessage.builder()
                                .systemMode(systemModeUpdateRequest.systemMode())
                                .build())
                .build();
    }
}
