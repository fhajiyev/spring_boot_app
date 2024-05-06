package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.OdinBaseInstantControlMessage;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActiveSystemModeOdinRequest {
    ControlMessage controlMessage;

    @Data
    @Builder
    public static class ControlMessage extends OdinBaseInstantControlMessage {
        private List<SystemMode> activeSystemModes;
    }

    public static ActiveSystemModeOdinRequest toInstance(
            ActiveSystemModeUpdateRequest activeSystemModeUpdateRequest) {
        return ActiveSystemModeOdinRequest.builder()
                .controlMessage(
                        ControlMessage.builder()
                                .activeSystemModes(activeSystemModeUpdateRequest.activeModes())
                                .build())
                .build();
    }
}
