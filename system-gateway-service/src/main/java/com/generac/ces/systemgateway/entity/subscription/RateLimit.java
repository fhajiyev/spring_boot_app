package com.generac.ces.systemgateway.entity.subscription;

import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rate_limit")
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")
    private SystemType systemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type")
    private SubscriberType subscriberType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "daily_limit_seconds")
    private Integer dailyLimitSec;

    @Column(name = "weekly_limit_seconds")
    private Integer weeklyLimitSec;

    @Column(name = "monthly_limit_seconds")
    private Integer monthlyLimitSec;

    @Column(name = "max_duration_seconds")
    private Integer maxDurationSec;
}
