package com.generac.ces.systemgateway.model.system;

import com.generac.ces.systemgateway.model.common.SystemMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * System Mode update response.
 *
 * @param systemId The ID of the system being updated.
 * @param updateId The ID representing the particular update initiated by this API request.
 * @param activeModes List of currently active modes set on the requested system ID.
 * @param updatedTimestampUtc The timestamp of the update.
 */
@Builder
public record ActiveSystemModeUpdateResponse(
        UUID systemId,
        UUID updateId,
        List<SystemMode> activeModes,
        OffsetDateTime updatedTimestampUtc) {}
