package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.Status;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionStatus {

    @NotNull private String subscriptionId;
    @NotNull private Status status;
    private String failureReason;
}
