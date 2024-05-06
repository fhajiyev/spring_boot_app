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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription_audit")
public class SubscriptionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "subscription_id")
    private String subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")
    private SystemType systemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type")
    private SubscriberType subscriberType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "system_id")
    private String systemId;

    @Column(name = "host_id")
    private String hostId;

    @Column(name = "duration_seconds")
    private Integer durationSec;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @Column(name = "client_id")
    private String clientId;

    @CreationTimestamp
    @Column(name = "created_at")
    private Timestamp createdAt;

    public static SubscriptionAudit create(
            String subscriptionId,
            SystemType systemType,
            SubscriberType subscriberType,
            ResourceType resourceType,
            String systemId,
            String hostId,
            Integer durationSec,
            Timestamp expiresAt,
            String clientId) {
        return SubscriptionAudit.builder()
                .subscriptionId(subscriptionId)
                .systemType(systemType)
                .subscriberType(subscriberType)
                .resourceType(resourceType)
                .systemId(systemId)
                .hostId(hostId)
                .durationSec(durationSec)
                .expiresAt(expiresAt)
                .clientId(clientId)
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSubscriptionId());
    }
}
