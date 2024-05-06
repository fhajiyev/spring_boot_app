package com.generac.ces.systemgateway.repository.subscription;

import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;

import com.generac.ces.systemgateway.entity.subscription.SubscriptionMetric;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionMetricRepository extends JpaRepository<SubscriptionMetric, Long> {

    @Lock(PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
    List<SubscriptionMetric> findByParameters(
            @Param("systemId") String systemId,
            @Param("systemType") SystemType systemType,
            @Param("resourceType") ResourceType resourceType);

    Optional<Timestamp> getMaxExpiryByParameters(
            @Param("systemId") String systemId,
            @Param("systemType") SystemType systemType,
            @Param("resourceType") ResourceType resourceType);

    Optional<Integer> getUsedQuota(
            @Param("systemId") String systemId,
            @Param("systemType") SystemType systemType,
            @Param("resourceType") ResourceType resourceType,
            @Param("subscriberType") SubscriberType subscriberType);
}
