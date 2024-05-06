package com.generac.ces.systemgateway.service.subscription;

import com.generac.ces.systemgateway.configuration.RateLimitConfiguration;
import com.generac.ces.systemgateway.entity.subscription.SubscriptionMetric;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.repository.subscription.SubscriptionMetricRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionMetricService {

    @Autowired private SubscriptionMetricRepository subscriptionMetricRepository;
    @Autowired private RateLimitConfiguration rateLimitConfiguration;

    @Transactional
    public boolean isRateLimitAcceptedAndOdinRequestedForSystem(
            String systemId,
            String systemType,
            String resourceType,
            String subscriberType,
            int durationSeconds) {

        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + durationSeconds;
        Timestamp expiry =
                Timestamp.valueOf(
                        SubscriptionService.getLocalDateTimeFromTimestampAndZoneId(
                                exprTimestamp, "UTC"));

        // select with lock
        List<SubscriptionMetric> smList =
                subscriptionMetricRepository.findByParameters(
                        systemId,
                        SystemType.valueOf(systemType),
                        ResourceType.valueOf(resourceType));

        SubscriptionMetric metric = null;
        Timestamp maxExpiry = null;

        // go through all selected entities
        for (SubscriptionMetric tmp : smList) {
            if (tmp.getSubscriberType().equals(SubscriberType.valueOf(subscriberType))) {
                metric = tmp; // found
            }
            if (maxExpiry == null || maxExpiry.getTime() < tmp.getExpiresAt().getTime()) {
                maxExpiry = tmp.getExpiresAt(); // update max expiry
            }
        }

        if (metric == null) { // insert new metric
            SubscriptionMetric ssNew =
                    SubscriptionMetric.create(
                            systemId,
                            SystemType.valueOf(systemType),
                            SubscriberType.valueOf(subscriberType),
                            ResourceType.valueOf(resourceType),
                            durationSeconds,
                            expiry);
            subscriptionMetricRepository.save(ssNew);

            // decide if needed to send odin-ms request or not
            return maxExpiry == null || maxExpiry.getTime() < expiry.getTime();
        }

        int sum = metric.getTotalDailyDurationSec();

        // need to check if last updated time belongs to current calendar day
        // if yes then need to check rate limit
        // otherwise accumulator needs to be reset
        LocalDateTime ldtLastUpdated = metric.getUpdatedAt().toLocalDateTime();
        LocalDateTime ldtCurrentTruncatedToDays =
                Instant.now()
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDateTime()
                        .truncatedTo(ChronoUnit.DAYS);

        // last updated time is still in current calendar day
        if (ldtLastUpdated.isAfter(ldtCurrentTruncatedToDays)) {
            int threshold =
                    getThreshold(
                            SubscriberType.valueOf(subscriberType),
                            SystemType.valueOf(systemType),
                            ResourceType.valueOf(resourceType));

            int durationLeftSeconds = threshold - sum;

            if (durationLeftSeconds < durationSeconds) {
                LocalDateTime ldtRefill = ldtCurrentTruncatedToDays.plusDays(1);

                throw new TooManyRequestsException(
                        "Duration limit for"
                                + " system type = "
                                + systemType
                                + ","
                                + " resource type = "
                                + resourceType
                                + " and system Id = "
                                + systemId
                                + " has been exceeded for current calendar day. You have "
                                + Math.max(durationLeftSeconds, 0)
                                + " second(s) left for subscription (out of "
                                + threshold
                                + "). Limit will be reset at "
                                + ldtRefill
                                + " UTC");
            }
        }
        // last updated time is not in current calendar day
        else {
            sum = 0;
        }

        // update total daily duration
        metric.setTotalDailyDurationSec(sum + durationSeconds);

        // update expiry only if it is larger than previous one
        if (metric.getExpiresAt().getTime() < expiry.getTime()) {
            metric.setExpiresAt(expiry);
        }

        // persist and unlock
        subscriptionMetricRepository.save(metric);

        // decide if needed to send odin-ms request or not
        return maxExpiry == null || maxExpiry.getTime() < expiry.getTime();
    }

    public int getUsedQuota(
            String systemId, String systemType, String resourceType, String subscriberType) {
        Optional<Integer> totalOpt =
                subscriptionMetricRepository.getUsedQuota(
                        systemId,
                        SystemType.valueOf(systemType),
                        ResourceType.valueOf(resourceType),
                        SubscriberType.valueOf(subscriberType));

        if (totalOpt.isEmpty()) {
            return 0;
        }

        return totalOpt.get();
    }

    private int getThreshold(
            SubscriberType subscriberType, SystemType systemType, ResourceType resourceType) {

        String key =
                getRateLimitMapKey(subscriberType.name(), systemType.name(), resourceType.name());
        return rateLimitConfiguration.getRateLimitMap().get(key);
    }

    private String getRateLimitMapKey(
            String subscriberType, String systemType, String resourceType) {
        return subscriberType + "-" + systemType + "-" + resourceType;
    }
}
