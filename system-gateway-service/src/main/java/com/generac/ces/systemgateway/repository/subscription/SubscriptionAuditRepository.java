package com.generac.ces.systemgateway.repository.subscription;

import com.generac.ces.systemgateway.entity.subscription.SubscriptionAudit;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionAuditRepository extends JpaRepository<SubscriptionAudit, Long> {

    List<ISubscriptionMaxExpiry> maxExpirationTimesForSystems(
            @Param("resourceType") String resourceType,
            @Param("startTime") Timestamp startTime,
            @Param("systemIds") List<String> systemIds);
}
