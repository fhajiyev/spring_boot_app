package com.generac.ces.systemgateway.service.subscription;

import com.generac.ces.SubscriptionProto;
import com.generac.ces.SystemShadowCommandsProto;
import com.generac.ces.system.SystemTypeOuterClass;
import com.generac.ces.systemgateway.configuration.RateLimitConfiguration;
import com.generac.ces.systemgateway.entity.subscription.SubscriptionAudit;
import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.exception.SubscriptionBadRequestException;
import com.generac.ces.systemgateway.exception.SubscriptionInternalServerException;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.exception.UnauthorizedException;
import com.generac.ces.systemgateway.model.Quota;
import com.generac.ces.systemgateway.model.SiteResponse;
import com.generac.ces.systemgateway.model.SubscriptionRequest;
import com.generac.ces.systemgateway.model.SubscriptionResponse;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.Status;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.repository.subscription.RateLimitRepository;
import com.generac.ces.systemgateway.repository.subscription.SubscriptionAuditRepository;
import com.generac.ces.systemgateway.service.DeviceSimulatorService;
import com.generac.ces.systemgateway.service.kinesis.KinesisService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.generac.ces.systemgateway.service.system.SiteService;
import com.generac.ces.systemgateway.service.system.SystemV2Service;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Setter
@ConfigurationProperties(prefix = "spring.profiles")
public class SubscriptionService {

    @Autowired private SubscriptionAsyncUtil asyncUtil;

    @Setter @Getter private List<String> active;

    @Autowired private KinesisService kinesisService;

    @Autowired private RateLimitConfiguration rateLimitConfiguration;

    @Autowired private SubscriptionAuditRepository subscriptionAuditRepository;

    @Autowired private RateLimitRepository rateLimitRepository;

    @Autowired private EssSystemService essSystemService;
    @Autowired private SystemV2Service systemV2Service;
    @Autowired private SiteService siteService;

    @Autowired private OdinService odinService;

    @Autowired private DeviceSimulatorService deviceSimulatorService;

    @Autowired private SubscriptionMetricService subscriptionMetricsService;

    private static final String SIMULATOR_BEACON_SUFFIX = "_DS";
    private static final String ENV_PRD = "prd";

    public List<SubscriptionResponse> createSubscription(SubscriptionRequest dto, String clientId) {

        int countBadRequests = 0;
        int countInternalServerErrors = 0;
        List<SubscriptionResponse> responseDtos = new ArrayList<>();

        for (int i = 0; i < dto.getSystems().size(); i++) {
            SubscriptionRequest.System system = dto.getSystems().get(i);

            try {
                responseDtos.add(
                        createSubscriptionForSystem(
                                dto, system.getSystemType(), system.getSystemId(), clientId));
            } catch (BadRequestException
                    | TooManyRequestsException
                    | UnauthorizedException
                    | ResourceNotFoundException e) {
                log.error("", e);
                countBadRequests++;
                responseDtos.add(
                        new SubscriptionResponse(
                                Status.FAILURE, system.getSystemId(), null, null, e.getMessage()));
            }
            // catch everything else as internal server error
            catch (RuntimeException e) {
                log.error("", e);
                countInternalServerErrors++;
                responseDtos.add(
                        new SubscriptionResponse(
                                Status.FAILURE, system.getSystemId(), null, null, e.getMessage()));
            }
        }

        if (countBadRequests > 0) {
            throw new SubscriptionBadRequestException(responseDtos);
        }
        if (countInternalServerErrors > 0) {
            throw new SubscriptionInternalServerException(responseDtos);
        }

        return responseDtos;
    }

    public SubscriptionResponse createSubscriptionForSystem(
            SubscriptionRequest dto, SystemType systemType, String systemId, String clientId) {

        // check rate limit
        boolean requestOdin =
                subscriptionMetricsService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        systemType.name(),
                        dto.getResourceType().name(),
                        dto.getSubscriberType().name(),
                        dto.getDurationSeconds());

        // call system-ms (and site-ms if applicable)
        SystemResponse systemDto;
        SiteResponse siteDto = null;

        if (systemType.equals(SystemType.ESS)) {
            systemDto = essSystemService.getSystemBySystemId(UUID.fromString(systemId));
        } else if (systemType.equals(SystemType.ES2)) {
            systemDto = systemV2Service.getSystemBySystemId(UUID.fromString(systemId));
            siteDto = siteService.getSiteBySystemId(UUID.fromString(systemId));
        } else {
            throw new BadRequestException("Unsupported system type: " + systemType.name());
        }

        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + dto.getDurationSeconds();

        LocalDateTime expDateTime = getLocalDateTimeFromTimestampAndZoneId(exprTimestamp, "UTC");

        // call odin-ms only if requested expiration time is larger than max(existing expiration
        // times across all subscribers) to prevent override
        if (requestOdin) {
            // call odinService in case prd env OR we do not have "_DS" postfix

            if (active.contains(ENV_PRD)
                    || systemDto.getBeaconRcpn() == null
                    || !systemDto.getBeaconRcpn().endsWith(SIMULATOR_BEACON_SUFFIX)) {

                if (dto.getResourceType().equals(ResourceType.ENERGY_RECORDSET_1HZ)) {
                    odinService.startStreamingForSystemId(
                            systemId, dto.getDurationSeconds(), systemType);
                } else {
                    throw new BadRequestException(
                            "Unsupported resource type: " + dto.getResourceType().name());
                }

            } else {
                deviceSimulatorService.startStreamingForSystemId(systemId);
            }
        }

        String subscriptionId = UUID.randomUUID().toString();
        String commandRequestId = UUID.randomUUID().toString();

        SubscriptionProto.Subscription subscription =
                SubscriptionProto.Subscription.newBuilder()
                        .setSubscriptionId(subscriptionId)
                        .setSubscriber(
                                SubscriptionProto.Subscriber.valueOf(
                                        dto.getSubscriberType().name()))
                        .setResourceType(
                                SubscriptionProto.ResourceType.valueOf(
                                        dto.getResourceType().name()))
                        .setDurationSeconds(dto.getDurationSeconds())
                        .setSystemType(SystemTypeOuterClass.SystemType.valueOf(systemType.name()))
                        .setHostId(
                                systemType.equals(SystemType.ESS)
                                        ? systemDto.getRcpId()
                                        : systemDto.getHostDeviceId())
                        .setSiteId(
                                systemType.equals(SystemType.ESS)
                                        ? systemDto.getSiteId()
                                        : siteDto.getSiteId())
                        .setSystemId(systemId)
                        .setSensorId(
                                systemType.equals(SystemType.ESS)
                                        ? systemDto.getBeaconRcpn()
                                        : systemDto.getHostDeviceId())
                        .setSubscriberClientId(clientId)
                        .build();

        SystemShadowCommandsProto.SystemShadowCommandRequest request =
                SystemShadowCommandsProto.SystemShadowCommandRequest.newBuilder()
                        .setTimestampUtc(currTimestamp)
                        .setCommandRequestId(commandRequestId)
                        .setSubscription(subscription)
                        .build();

        // send proto to system-shadow-service
        kinesisService.upload(request);

        SubscriptionAudit audit =
                SubscriptionAudit.create(
                        subscriptionId,
                        systemType,
                        dto.getSubscriberType(),
                        dto.getResourceType(),
                        systemId,
                        systemType.equals(SystemType.ESS)
                                ? systemDto.getRcpId()
                                : systemDto.getHostDeviceId(),
                        dto.getDurationSeconds(),
                        Timestamp.valueOf(expDateTime),
                        clientId);

        // persist audit asynchronously
        saveSubscriptionAsync(audit);

        return new SubscriptionResponse(
                Status.SUBMITTED, systemId, subscriptionId, exprTimestamp, null);
    }

    @Async
    public void saveSubscriptionAsync(SubscriptionAudit audit) {
        asyncUtil.saveSubscription(audit);
    }

    public Quota getRemainingQuotaInSeconds(
            SubscriberType subscriberType,
            SystemType systemType,
            ResourceType resourceType,
            String systemId) {
        if (!systemType.equals(SystemType.ESS)) {
            throw new BadRequestException("Unsupported system type: " + systemType.name());
        }

        essSystemService.getSystemBySystemId(UUID.fromString(systemId));

        int sum =
                subscriptionMetricsService.getUsedQuota(
                        systemId, systemType.name(), resourceType.name(), subscriberType.name());

        int threshold = getThreshold(subscriberType, systemType, resourceType);
        int durationLeftSeconds = threshold - sum;
        return new Quota(threshold, Math.max(durationLeftSeconds, 0));
    }

    private int getThreshold(
            SubscriberType subscriberType, SystemType systemType, ResourceType resourceType) {

        String key =
                getRateLimitMapKey(subscriberType.name(), systemType.name(), resourceType.name());
        return rateLimitConfiguration.getRateLimitMap().get(key);
    }

    public static LocalDateTime getLocalDateTimeFromTimestampAndZoneId(
            long exprTimestamp, String zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of(zoneId));
    }

    private String getRateLimitMapKey(
            String subscriberType, String systemType, String resourceType) {
        return subscriberType + "-" + systemType + "-" + resourceType;
    }
}
