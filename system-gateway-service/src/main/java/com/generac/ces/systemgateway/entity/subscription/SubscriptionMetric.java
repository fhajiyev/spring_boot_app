package com.generac.ces.systemgateway.entity.subscription;

import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription_metric")
public class SubscriptionMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "system_id")
    private String systemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")
    private SystemType systemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type")
    private SubscriberType subscriberType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "total_daily_duration_seconds")
    private Integer totalDailyDurationSec;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public static SubscriptionMetric create(
            String systemId,
            SystemType systemType,
            SubscriberType subscriberType,
            ResourceType resourceType,
            Integer durationSec,
            Timestamp expiresAt) {
        return SubscriptionMetric.builder()
                .systemId(systemId)
                .systemType(systemType)
                .subscriberType(subscriberType)
                .resourceType(resourceType)
                .totalDailyDurationSec(durationSec)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getId(), getSystemId(), getSystemType(), getSubscriberType(), getResourceType());
    }
}
