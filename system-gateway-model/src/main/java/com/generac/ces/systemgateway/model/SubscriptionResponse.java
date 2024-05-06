package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponse {
    private Status subscriptionStatus;
    private String systemId;
    private String subscriptionId;
    private Long expirationTimestamp;
    private String failureReason;
}
