package com.generac.ces.systemgateway.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generac.ces.systemgateway.configuration.RateLimitConfiguration;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.Quota;
import com.generac.ces.systemgateway.model.SiteResponse;
import com.generac.ces.systemgateway.model.SubscriptionRequest;
import com.generac.ces.systemgateway.model.SubscriptionResponse;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.repository.subscription.ISubscriptionDuration;
import com.generac.ces.systemgateway.repository.subscription.ISubscriptionMaxExpiry;
import com.generac.ces.systemgateway.repository.subscription.SubscriptionAuditRepository;
import com.generac.ces.systemgateway.service.kinesis.KinesisService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.service.subscription.SubscriptionAsyncUtil;
import com.generac.ces.systemgateway.service.subscription.SubscriptionMetricService;
import com.generac.ces.systemgateway.service.subscription.SubscriptionService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.generac.ces.systemgateway.service.system.SiteService;
import com.generac.ces.systemgateway.service.system.SystemV2Service;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SubscriptionServiceTest {

    @InjectMocks private SubscriptionService subscriptionService;

    @Mock private SubscriptionAuditRepository subscriptionAuditRepository;

    @Mock private SubscriptionMetricService subscriptionMetricsService;

    @Mock private SubscriptionAsyncUtil subscriptionAsyncUtil;

    @Mock private KinesisService kinesisService;

    @Mock private EssSystemService systemService;

    @Mock private SystemV2Service systemV2Service;

    @Mock private SiteService siteService;

    @Mock private OdinService odinService;

    @Mock private DeviceSimulatorService deviceSimulatorService;

    @Mock private RateLimitConfiguration rateLimitConfiguration;

    @Before
    public void setup() {
        subscriptionService.setActive(
                new ArrayList<>() {
                    {
                        add("prd");
                    }
                });
    }

    @Test
    public void createSubscriptionForEssTest_Success() {
        // Arrange
        int requestedDurationSecs = 3;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String sampleSiteId = UUID.randomUUID().toString();
        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + requestedDurationSecs;
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs - 1;

        int thresholdSeconds = 10;
        int maxDurationSeconds = 300;
        LocalDateTime expDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of("UTC"));
        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));
        SubscriptionRequest reqDto =
                new SubscriptionRequest(
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        requestedDurationSecs,
                        Collections.singletonList(
                                new SubscriptionRequest.System(SystemType.ESS, systemId)));
        SystemResponse sysRespDto =
                new SystemResponse(
                        UUID.fromString(systemId),
                        sampleSiteId,
                        "000100073534",
                        "000100120934",
                        null);
        OdinResponse odinRespDto =
                new OdinResponse(UUID.randomUUID().toString(), "SUCCESS", null, null);

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);
        Map<String, Integer> maxDurationMap = new HashMap<>();
        maxDurationMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                maxDurationSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricsService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        eq(systemId),
                        any(),
                        eq(ResourceType.ENERGY_RECORDSET_1HZ.name()),
                        eq(SubscriberType.PWRVIEW.name()),
                        eq(requestedDurationSecs)))
                .thenReturn(true);

        when(systemService.getSystemBySystemId(UUID.fromString(systemId))).thenReturn(sysRespDto);
        when(odinService.startStreamingForSystemId(systemId, requestedDurationSecs, SystemType.ESS))
                .thenReturn(odinRespDto);

        // Action & Assert
        List<SubscriptionResponse> respDtos =
                subscriptionService.createSubscription(reqDto, "sample_client_id");

        verify(subscriptionMetricsService, times(1))
                .isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);
        verify(subscriptionAsyncUtil, times(1)).saveSubscription(any());
        verify(kinesisService, times(1)).upload(any());

        verify(systemService, times(1)).getSystemBySystemId(UUID.fromString(systemId));
        verify(systemV2Service, times(0))
                .getSystemBySystemId(UUID.fromString(systemId)); // not invoked for ESS
        verify(siteService, times(0))
                .getSiteBySystemId(UUID.fromString(systemId)); // not invoked for ESS

        verify(odinService, times(1))
                .startStreamingForSystemId(systemId, requestedDurationSecs, SystemType.ESS);
        assertEquals(1, respDtos.size());
        assertEquals(Long.valueOf(exprTimestamp), respDtos.get(0).getExpirationTimestamp());
    }

    @Test
    public void createSubscriptionForEs2Test_Success() {
        // Arrange
        int requestedDurationSecs = 3;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String sampleSiteId = UUID.randomUUID().toString();
        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + requestedDurationSecs;
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs - 1;

        int thresholdSeconds = 10;
        int maxDurationSeconds = 300;
        LocalDateTime expDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of("UTC"));
        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));
        SubscriptionRequest reqDto =
                new SubscriptionRequest(
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        requestedDurationSecs,
                        Collections.singletonList(
                                new SubscriptionRequest.System(SystemType.ES2, systemId)));
        SystemResponse sysRespDto =
                SystemResponse.builder()
                        .systemId(UUID.fromString(systemId))
                        .hostDeviceId("000100073534")
                        .build();
        SiteResponse siteRespDto = SiteResponse.builder().siteId("sample_site_id").build();
        OdinResponse odinRespDto =
                new OdinResponse(UUID.randomUUID().toString(), "SUCCESS", null, null);

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ES2
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);
        Map<String, Integer> maxDurationMap = new HashMap<>();
        maxDurationMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ES2
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                maxDurationSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricsService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        eq(systemId),
                        any(),
                        eq(ResourceType.ENERGY_RECORDSET_1HZ.name()),
                        eq(SubscriberType.PWRVIEW.name()),
                        eq(requestedDurationSecs)))
                .thenReturn(true);

        when(systemV2Service.getSystemBySystemId(UUID.fromString(systemId))).thenReturn(sysRespDto);
        when(siteService.getSiteBySystemId(UUID.fromString(systemId))).thenReturn(siteRespDto);
        when(odinService.startStreamingForSystemId(systemId, requestedDurationSecs, SystemType.ES2))
                .thenReturn(odinRespDto);

        // Action & Assert
        List<SubscriptionResponse> respDtos =
                subscriptionService.createSubscription(reqDto, "sample_client_id");

        verify(subscriptionMetricsService, times(1))
                .isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ES2.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);
        verify(subscriptionAsyncUtil, times(1)).saveSubscription(any());
        verify(kinesisService, times(1)).upload(any());

        verify(systemService, times(0))
                .getSystemBySystemId(UUID.fromString(systemId)); // not invoked for ES2
        verify(systemV2Service, times(1)).getSystemBySystemId(UUID.fromString(systemId));
        verify(siteService, times(1)).getSiteBySystemId(UUID.fromString(systemId));

        verify(odinService, times(1))
                .startStreamingForSystemId(systemId, requestedDurationSecs, SystemType.ES2);
        assertEquals(1, respDtos.size());
        assertEquals(Long.valueOf(exprTimestamp), respDtos.get(0).getExpirationTimestamp());
    }

    @Test
    public void createSubscriptionTest_Simulator_Success() {
        subscriptionService.setActive(
                new ArrayList<>() {
                    {
                        add("stg");
                    }
                });
        // Arrange
        int requestedDurationSecs = 3;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String sampleSiteId = UUID.randomUUID().toString();
        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + requestedDurationSecs;
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs - 1;

        int thresholdSeconds = 10;
        int maxDurationSeconds = 300;
        LocalDateTime expDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of("UTC"));
        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));
        SubscriptionRequest reqDto =
                new SubscriptionRequest(
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        requestedDurationSecs,
                        Collections.singletonList(
                                new SubscriptionRequest.System(SystemType.ESS, systemId)));
        SystemResponse sysRespDto =
                new SystemResponse(
                        UUID.fromString(systemId),
                        sampleSiteId,
                        "000100073534",
                        "000100120934_DS",
                        null);

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);
        Map<String, Integer> maxDurationMap = new HashMap<>();
        maxDurationMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                maxDurationSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricsService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        eq(systemId),
                        any(),
                        eq(ResourceType.ENERGY_RECORDSET_1HZ.name()),
                        eq(SubscriberType.PWRVIEW.name()),
                        eq(requestedDurationSecs)))
                .thenReturn(true);

        when(systemService.getSystemBySystemId(UUID.fromString(systemId))).thenReturn(sysRespDto);

        // Action & Assert
        List<SubscriptionResponse> respDtos =
                subscriptionService.createSubscription(reqDto, "sample_client_id");

        verify(subscriptionMetricsService, times(1))
                .isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);
        verify(subscriptionAsyncUtil, times(1)).saveSubscription(any());
        verify(kinesisService, times(1)).upload(any());
        verify(systemService, times(1)).getSystemBySystemId(UUID.fromString(systemId));
        verify(odinService, times(0))
                .startStreamingForSystemId(
                        systemId, requestedDurationSecs, SystemType.ESS); // odin-ms not called
        verify(deviceSimulatorService, times(1)).startStreamingForSystemId(systemId);
        assertEquals(1, respDtos.size());
        assertEquals(Long.valueOf(exprTimestamp), respDtos.get(0).getExpirationTimestamp());
    }

    @Test
    public void createSubscriptionEssTest_RequestedExpirationIsLessThanExisting() {
        // Arrange
        int requestedDurationSecs = 3;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String sampleSiteId = UUID.randomUUID().toString();
        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + requestedDurationSecs;
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs + 1;

        int thresholdSeconds = 10;
        int maxDurationSeconds = 300;
        LocalDateTime expDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of("UTC"));
        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));
        SubscriptionRequest reqDto =
                new SubscriptionRequest(
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        requestedDurationSecs,
                        Collections.singletonList(
                                new SubscriptionRequest.System(SystemType.ESS, systemId)));
        SystemResponse sysRespDto =
                new SystemResponse(
                        UUID.fromString(systemId),
                        sampleSiteId,
                        "000100073534",
                        "000100120934",
                        null);

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);
        Map<String, Integer> maxDurationMap = new HashMap<>();
        maxDurationMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                maxDurationSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricsService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        eq(systemId),
                        any(),
                        eq(ResourceType.ENERGY_RECORDSET_1HZ.name()),
                        eq(SubscriberType.PWRVIEW.name()),
                        eq(requestedDurationSecs)))
                .thenReturn(false);

        when(systemService.getSystemBySystemId(UUID.fromString(systemId))).thenReturn(sysRespDto);

        // Action & Assert
        List<SubscriptionResponse> respDtos =
                subscriptionService.createSubscription(reqDto, "sample_client_id");

        verify(subscriptionMetricsService, times(1))
                .isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);
        verify(subscriptionAsyncUtil, times(1)).saveSubscription(any());
        verify(kinesisService, times(1)).upload(any());

        verify(systemService, times(1)).getSystemBySystemId(UUID.fromString(systemId));
        verify(systemV2Service, times(0)).getSystemBySystemId(UUID.fromString(systemId));
        verify(siteService, times(0)).getSiteBySystemId(UUID.fromString(systemId));

        verify(odinService, times(0))
                .startStreamingForSystemId(systemId, requestedDurationSecs, SystemType.ESS);
        assertEquals(1, respDtos.size());
        assertEquals(Long.valueOf(exprTimestamp), respDtos.get(0).getExpirationTimestamp());
    }

    @Test
    public void createSubscriptionEs2Test_RequestedExpirationIsLessThanExisting() {
        // Arrange
        int requestedDurationSecs = 3;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String sampleSiteId = UUID.randomUUID().toString();
        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + requestedDurationSecs;
        long exprExistingTimestamp = currTimestamp + requestedDurationSecs + 1;

        int thresholdSeconds = 10;
        int maxDurationSeconds = 300;
        LocalDateTime expDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of("UTC"));
        LocalDateTime expExistingDateTime =
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(exprExistingTimestamp), ZoneId.of("UTC"));
        SubscriptionRequest reqDto =
                new SubscriptionRequest(
                        SubscriberType.PWRVIEW,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        requestedDurationSecs,
                        Collections.singletonList(
                                new SubscriptionRequest.System(SystemType.ES2, systemId)));
        SystemResponse sysRespDto =
                SystemResponse.builder()
                        .systemId(UUID.fromString(systemId))
                        .hostDeviceId("000100073534")
                        .build();
        SiteResponse siteRespDto = SiteResponse.builder().siteId("sample_site_id").build();

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ES2
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);
        Map<String, Integer> maxDurationMap = new HashMap<>();
        maxDurationMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ES2
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                maxDurationSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricsService.isRateLimitAcceptedAndOdinRequestedForSystem(
                        eq(systemId),
                        any(),
                        eq(ResourceType.ENERGY_RECORDSET_1HZ.name()),
                        eq(SubscriberType.PWRVIEW.name()),
                        eq(requestedDurationSecs)))
                .thenReturn(false);

        when(systemV2Service.getSystemBySystemId(UUID.fromString(systemId))).thenReturn(sysRespDto);
        when(siteService.getSiteBySystemId(UUID.fromString(systemId))).thenReturn(siteRespDto);

        // Action & Assert
        List<SubscriptionResponse> respDtos =
                subscriptionService.createSubscription(reqDto, "sample_client_id");

        verify(subscriptionMetricsService, times(1))
                .isRateLimitAcceptedAndOdinRequestedForSystem(
                        systemId,
                        SystemType.ES2.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name(),
                        requestedDurationSecs);
        verify(subscriptionAsyncUtil, times(1)).saveSubscription(any());
        verify(kinesisService, times(1)).upload(any());

        verify(systemService, times(0)).getSystemBySystemId(UUID.fromString(systemId));
        verify(systemV2Service, times(1)).getSystemBySystemId(UUID.fromString(systemId));
        verify(siteService, times(1)).getSiteBySystemId(UUID.fromString(systemId));

        verify(odinService, times(0))
                .startStreamingForSystemId(systemId, requestedDurationSecs, SystemType.ES2);
        assertEquals(1, respDtos.size());
        assertEquals(Long.valueOf(exprTimestamp), respDtos.get(0).getExpirationTimestamp());
    }

    @Test
    public void getRemainingQuotaInSecondsTest() {
        // Arrange
        int thresholdSeconds = 10;
        int maxDurationSeconds = 300;
        int quotaUsed = 7;
        long quotaRemaining = thresholdSeconds - quotaUsed;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String sampleSiteId = UUID.randomUUID().toString();
        SystemResponse sysRespDto =
                new SystemResponse(
                        UUID.fromString(systemId),
                        sampleSiteId,
                        "000100073534",
                        "000100120934",
                        null);

        Map<String, Integer> rateLimitMap = new HashMap<>();
        rateLimitMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                thresholdSeconds);
        Map<String, Integer> maxDurationMap = new HashMap<>();
        maxDurationMap.put(
                SubscriberType.PWRVIEW.name()
                        + "-"
                        + SystemType.ESS
                        + "-"
                        + ResourceType.ENERGY_RECORDSET_1HZ,
                maxDurationSeconds);

        when(rateLimitConfiguration.getRateLimitMap()).thenReturn(rateLimitMap);
        when(subscriptionMetricsService.getUsedQuota(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name()))
                .thenReturn(quotaUsed);
        when(systemService.getSystemBySystemId(UUID.fromString(systemId))).thenReturn(sysRespDto);

        // Action & Assert
        Quota respDto =
                subscriptionService.getRemainingQuotaInSeconds(
                        SubscriberType.PWRVIEW,
                        SystemType.ESS,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        systemId);

        verify(subscriptionMetricsService, times(1))
                .getUsedQuota(
                        systemId,
                        SystemType.ESS.name(),
                        ResourceType.ENERGY_RECORDSET_1HZ.name(),
                        SubscriberType.PWRVIEW.name());
        assertEquals(respDto.getRemainingQuotaSeconds(), quotaRemaining);
    }

    @Data
    @AllArgsConstructor
    private class TestSubscriptionDuration implements ISubscriptionDuration {

        private String systemId;
        private String systemType;
        private Long durationSecs;
    }

    @Data
    @AllArgsConstructor
    private class TestSubscriptionMaxExpiry implements ISubscriptionMaxExpiry {

        private String systemId;
        private String systemType;
        private Timestamp maxExpiry;
    }
}
