package com.generac.ces.systemgateway.repository.subscription;

import java.sql.Timestamp;

public interface ISubscriptionMaxExpiry {
    String getSystemId();

    String getSystemType();

    Timestamp getMaxExpiry();
}
