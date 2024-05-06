package com.generac.ces.systemgateway.model.system;

import com.generac.ces.systemgateway.model.common.SystemMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * Returns current system mode and a list of available modes on a system.
 *
 * @param systemId The ID of the system being updated.
 * @param mode Current system mode set on the requested system ID.
 * @param activeModes List of currently active modes set on the requested system ID.
 * @param availableModes List of available modes on the inverter.
 * @param updatedTimestampUtc The timestamp of the update.
 */
@Builder
public record SystemModeGetResponse(
        UUID systemId,
        SystemMode mode,
        List<SystemMode> activeModes,
        List<SystemMode> availableModes,
        OffsetDateTime updatedTimestampUtc) {}
