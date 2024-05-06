package com.generac.ces.systemgateway.model.system;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.systemgateway.model.common.SystemMode;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/**
 * System Mode update response.
 *
 * @param systemId The ID of the system being updated.
 * @param updateId The ID representing the particular update initiated by this API request.
 * @param systemMode The specific mode the system is being updated to.
 * @param updatedTimestampUtc The timestamp of the update.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemModeUpdateResponse(
        UUID systemId, UUID updateId, SystemMode systemMode, OffsetDateTime updatedTimestampUtc) {}
