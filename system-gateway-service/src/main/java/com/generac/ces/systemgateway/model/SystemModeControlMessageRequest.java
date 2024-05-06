package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemModeControlMessageRequest {
    private List<ControlMessage> controlMessages;

    @Data
    @Builder
    public static class ControlMessage extends OdinBaseControlMessage {
        private SystemMode systemMode;

        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        private OffsetDateTime startTime;

        private Long duration;
        private Boolean cancelIfOvershadowed;
        private Boolean cancelOnExternalChange;
    }

    public static SystemModeControlMessageRequest toInstance(
            SystemModeUpdateRequest systemModeUpdateRequest) {
        return SystemModeControlMessageRequest.builder()
                .controlMessages(
                        Collections.singletonList(
                                ControlMessage.builder()
                                        .systemMode(systemModeUpdateRequest.systemMode())
                                        .startTime(systemModeUpdateRequest.startTime())
                                        .duration(systemModeUpdateRequest.duration())
                                        .cancelOnExternalChange(
                                                systemModeUpdateRequest.cancelOnExternalChange())
                                        .cancelIfOvershadowed(
                                                systemModeUpdateRequest.cancelIfOvershadowed())
                                        .build()))
                .build();
    }
}
