package com.generac.ces.systemgateway.model.system;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.generac.ces.systemgateway.model.OffsetDateTimeSerializer;
import com.generac.ces.systemgateway.model.common.SystemMode;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import lombok.Builder;

/**
 * The request entity used to trigger an update for current system mode on the system.
 *
 * @param systemMode The specific mode the system is being updated to.
 * @param startTime The start time of the scheduled request.
 * @param duration The duration of the scheduled request.
 * @param cancelOnExternalChange When set to true, any external change originating outside the
 *     system controller will be cancelled.
 * @param cancelIfOvershadowed
 */
@Builder
public record SystemModeUpdateRequest(
        @NotNull SystemMode systemMode,
        @JsonSerialize(using = OffsetDateTimeSerializer.class) OffsetDateTime startTime,
        Long duration,
        Boolean cancelOnExternalChange,
        Boolean cancelIfOvershadowed) {

    public SystemModeUpdateRequest {
        cancelOnExternalChange = true;
        cancelIfOvershadowed = false;
    }
}
