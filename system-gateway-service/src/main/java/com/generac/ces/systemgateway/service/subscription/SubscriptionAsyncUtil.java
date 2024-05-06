package com.generac.ces.systemgateway.service.subscription;

import com.generac.ces.systemgateway.entity.subscription.SubscriptionAudit;
import com.generac.ces.systemgateway.repository.subscription.SubscriptionAuditRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class SubscriptionAsyncUtil {

    SubscriptionAuditRepository subscriptionAuditRepository;

    @Autowired
    public SubscriptionAsyncUtil(SubscriptionAuditRepository subscriptionAuditRepository) {
        this.subscriptionAuditRepository = subscriptionAuditRepository;
    }

    @Async
    public Void saveSubscription(SubscriptionAudit audit) {
        subscriptionAuditRepository.save(audit);

        log.info(
                "Created subscription for "
                        + "client Id = "
                        + audit.getClientId()
                        + ", "
                        + "subscriber = "
                        + audit.getSubscriberType().name()
                        + ", "
                        + "system type = "
                        + audit.getSystemType().name()
                        + ", "
                        + "resource type = "
                        + audit.getResourceType().name()
                        + " "
                        + "and system Id = "
                        + audit.getSystemId());
        return null;
    }
}
