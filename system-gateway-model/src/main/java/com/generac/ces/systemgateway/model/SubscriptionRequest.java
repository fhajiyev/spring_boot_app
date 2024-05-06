package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequest {

    @NotNull private SubscriberType subscriberType;
    @NotNull private ResourceType resourceType;
    private int durationSeconds;
    @NotNull private List<System> systems;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class System {
        @NotNull private SystemType systemType;
        @NotNull private String systemId;
    }
}
