package com.generac.ces.systemgateway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.generac.ces.systemgateway.configuration.RateLimitConfiguration;
import com.generac.ces.systemgateway.entity.subscription.SubscriptionMetric;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.repository.subscription.SubscriptionMetricRepository;
import com.generac.ces.systemgateway.service.subscription.SubscriptionMetricService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SubscriptionMetricServiceTest {

    @InjectMocks private SubscriptionMetricService subscriptionMetricService;

    @Mock private SubscriptionMetricRepository subscriptionMetricRepository;

    @Mock private RateLimitConfiguration rateLimitConfiguration;

    @Test
    public void ensumeRateLimitingAndDecideOnOdinRequest_RateLimitPassedAndOdinRequested() {
        // Arrange
        int requestedDurationSecs = 3;
        int totalDurationSecs = 5;
        int thresholdSeconds = 10;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        long currTimestamp = Instant.now().getEpochSecond();
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs - 1;

        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));

        SubscriptionMetric sm =
                new SubscriptionMetric(
                        1L,
                        systemId,
                        SystemType.ESS,
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        totalDurationSecs,
                        Timestamp.valueOf(expExistingDateTime),
                        Timestamp.from(Instant.now()));

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricRepository.findByParameters(
                        systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ))
                .thenReturn(Collections.singletonList(sm));

        // Action & Assert
        boolean ret =
                subscriptionMetricService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);

        verify(subscriptionMetricRepository, times(1))
                .findByParameters(systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ);
        assertEquals(true, ret);
    }

    @Test
    public void
            ensumeRateLimitingAndDecideOnOdinRequest_RateLimitPassedAndOdinNotRequested_SameSubscriberExpiryIsLarger() {
        // Arrange
        int requestedDurationSecs = 3;
        int totalDurationSecs = 5;
        int thresholdSeconds = 10;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        long currTimestamp = Instant.now().getEpochSecond();
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs + 1;

        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));

        SubscriptionMetric sm =
                new SubscriptionMetric(
                        1L,
                        systemId,
                        SystemType.ESS,
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        totalDurationSecs,
                        Timestamp.valueOf(expExistingDateTime),
                        Timestamp.from(Instant.now()));

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricRepository.findByParameters(
                        systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ))
                .thenReturn(Collections.singletonList(sm));

        // Action & Assert
        boolean ret =
                subscriptionMetricService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);

        verify(subscriptionMetricRepository, times(1))
                .findByParameters(systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ);
        assertEquals(false, ret);
    }

    @Test
    public void
            ensumeRateLimitingAndDecideOnOdinRequest_RateLimitPassedAndOdinNotRequested_OtherSubscriberExpiryIsLarger() {
        // Arrange
        int requestedDurationSecs = 3;
        int totalDurationSecs = 5;
        int thresholdSeconds = 10;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        long currTimestamp = Instant.now().getEpochSecond();
        long exprExistingTimestampPwrView = currTimestamp + requestedDurationSecs - 1;
        long exprExistingTimestampSunnova = currTimestamp + requestedDurationSecs + 1;

        LocalDateTime expExistingDateTimePwrView =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestampPwrView), ZoneId.of("UTC"));
        LocalDateTime expExistingDateTimeSunnova =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestampSunnova), ZoneId.of("UTC"));

        SubscriptionMetric smPwrView =
                new SubscriptionMetric(
                        1L,
                        systemId,
                        SystemType.ESS,
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        totalDurationSecs,
                        Timestamp.valueOf(expExistingDateTimePwrView),
                        Timestamp.from(Instant.now()));
        SubscriptionMetric smSunnova =
                new SubscriptionMetric(
                        1L,
                        systemId,
                        SystemType.ESS,
                        SubscriberType.SUNNOVA,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        totalDurationSecs,
                        Timestamp.valueOf(expExistingDateTimeSunnova),
                        Timestamp.from(Instant.now()));
        List<SubscriptionMetric> metrics = new ArrayList<>();
        metrics.add(smPwrView);
        metrics.add(smSunnova);
        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricRepository.findByParameters(
                        systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ))
                .thenReturn(metrics);

        // Action & Assert
        boolean ret =
                subscriptionMetricService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);

        verify(subscriptionMetricRepository, times(1))
                .findByParameters(systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ);
        assertEquals(false, ret);
    }

    @Test
    public void ensumeRateLimitingAndDecideOnOdinRequest_RateLimitNotPassed() {
        // Arrange
        int requestedDurationSecs = 7;
        int totalDurationSecs = 5;
        int thresholdSeconds = 10;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        long currTimestamp = Instant.now().getEpochSecond();
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs - 1;

        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));

        SubscriptionMetric sm =
                new SubscriptionMetric(
                        1L,
                        systemId,
                        SystemType.ESS,
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        totalDurationSecs,
                        Timestamp.valueOf(expExistingDateTime),
                        Timestamp.from(Instant.now()));

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricRepository.findByParameters(
                        systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ))
                .thenReturn(Collections.singletonList(sm));

        // Action and Assert
        assertThrows(
                TooManyRequestsException.class,
                () -> {
                    subscriptionMetricService.isRateLimitAcceptedAndOdinRequestedForSystem(
                            systemId,
                            SystemType.ESS.name(),
                            ResourceType.ENERGY_RECORDSET_1HZ.name(),
                            SubscriberType.PWRVIEW.name(),
                            requestedDurationSecs);
                });

        verify(subscriptionMetricRepository, times(1))
                .findByParameters(systemId, SystemType.ESS, ResourceType.ENERGY_RECORDSET_1HZ);
    }
}
