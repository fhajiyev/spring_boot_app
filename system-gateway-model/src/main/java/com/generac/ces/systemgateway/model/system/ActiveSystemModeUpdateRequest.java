package com.generac.ces.systemgateway.model.system;

import com.generac.ces.systemgateway.model.common.SystemMode;
import java.util.List;
import javax.validation.constraints.NotEmpty;

/**
 * The request entity used to trigger an update for active system modes enabled on the system.
 *
 * @param activeModes List of currently active modes set on the requested system ID.
 */
public record ActiveSystemModeUpdateRequest(@NotEmpty List<SystemMode> activeModes) {}
