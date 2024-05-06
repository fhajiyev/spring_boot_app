package com.generac.ces.systemgateway.integrationtest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.generac.ces.systemgateway.entity.subscription.SubscriptionAudit;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.service.subscription.SubscriptionAsyncUtil;
import com.generac.ces.systemgateway.service.subscription.SubscriptionService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationAsyncTest extends IntegrationTestBase {

    @MockBean private SubscriptionAsyncUtil asyncUtil;
    @Autowired SubscriptionService subscriptionService;

    @Test
    public void createSubscriptionTest_SuccessOnAsyncDbWriteFailure() {
        // Arrange
        int requestedDurationSecs = 3;
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        long currTimestamp = Instant.now().getEpochSecond();
        long exprTimestamp = currTimestamp + requestedDurationSecs;
        String clientId = "sample_client_id";
        SubscriberType subscriberType = SubscriberType.PWRVIEW;
        SystemType systemType = SystemType.ESS;
        ResourceType resourceType = ResourceType.ENERGY_RECORDSET_1HZ;

        LocalDateTime expDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(exprTimestamp), ZoneId.of("UTC"));
        SubscriptionAudit subscrAudit =
                SubscriptionAudit.create(
                        "sample_subscr_id",
                        systemType,
                        subscriberType,
                        resourceType,
                        systemId,
                        "000100073534",
                        requestedDurationSecs,
                        Timestamp.valueOf(expDateTime),
                        clientId);

        when(asyncUtil.saveSubscription(any()))
                .thenThrow(new RuntimeException("failed to persist subscription record"));

        // Action & Assert
        ThrowableAssert.ThrowingCallable methodCall =
                () -> subscriptionService.saveSubscriptionAsync(subscrAudit);

        assertThatNoException().isThrownBy(methodCall);
    }
}
