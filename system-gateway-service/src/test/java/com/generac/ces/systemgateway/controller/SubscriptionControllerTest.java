package com.generac.ces.systemgateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.configuration.RateLimitConfiguration;
import com.generac.ces.systemgateway.controller.subscription.SubscriptionController;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.Quota;
import com.generac.ces.systemgateway.model.SubscriptionResponse;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.Status;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.service.subscription.SubscriptionService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = SubscriptionController.class)
@AutoConfigureMockMvc
class SubscriptionControllerTest {
    @BeforeAll
    static void beforeAll() {
        RateLimitConfiguration.maxDurationMap.put("PWRVIEW-ESS-ENERGY_RECORDSET_1HZ", 10);
    }

    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean private SubscriptionService subscriptionService;

    @Test
    void createSubscriptionTest() throws Exception {
        // Arrange
        List<SubscriptionResponse> responseDtos = new ArrayList<>();
        responseDtos.add(
                new SubscriptionResponse(
                        Status.SUBMITTED,
                        "sample_system_id1",
                        "sample_subscription_id1",
                        1663109445L,
                        null));
        responseDtos.add(
                new SubscriptionResponse(
                        Status.SUBMITTED,
                        "sample_system_id2",
                        "sample_subscription_id2",
                        1663109475L,
                        null));
        Mockito.when(subscriptionService.createSubscription(any(), eq("sample_client_id")))
                .thenReturn(responseDtos);

        JsonObject json = new JsonObject();
        json.addProperty("subscriberType", "PWRVIEW");
        json.addProperty("resourceType", "ENERGY_RECORDSET_1HZ");
        json.addProperty("durationSeconds", 5);
        JsonArray arr = new JsonArray();
        JsonObject systemJson1 = new JsonObject();
        systemJson1.addProperty("systemType", "ESS");
        systemJson1.addProperty("systemId", "sample_system_id1");
        arr.add(systemJson1);
        JsonObject systemJson2 = new JsonObject();
        systemJson2.addProperty("systemType", "ESS");
        systemJson2.addProperty("systemId", "sample_system_id2");
        arr.add(systemJson2);
        json.add("systems", arr);

        // Action & Assert
        mvc.perform(
                        post("/v1/subscriptions")
                                .content(json.toString())
                                .header("Content-Type", "application/json")
                                .header(
                                        CallerMetadataRequestHeaders.COGNITO_CLIENT_ID
                                                .getHeaderName(),
                                        "sample_client_id"))
                .andExpect(status().isAccepted());
        verify(subscriptionService, times(1)).createSubscription(any(), eq("sample_client_id"));
    }

    @Test
    void createSubscriptionTest_InvalidSubscriberType() throws Exception {
        // Arrange
        JsonObject json = new JsonObject();
        json.addProperty("subscriberType", "SUNNOVA"); // not in rate limit map
        json.addProperty("resourceType", "ENERGY_RECORDSET_1HZ");
        json.addProperty("durationSeconds", 5);
        JsonArray arr = new JsonArray();
        JsonObject systemJson1 = new JsonObject();
        systemJson1.addProperty("systemType", "ESS");
        systemJson1.addProperty("systemId", "sample_system_id1");
        arr.add(systemJson1);
        json.add("systems", arr);

        // Action & Assert
        mvc.perform(
                        post("/v1/subscriptions")
                                .content(json.toString())
                                .header("Content-Type", "application/json")
                                .header(
                                        CallerMetadataRequestHeaders.COGNITO_CLIENT_ID
                                                .getHeaderName(),
                                        "sample_client_id"))
                .andExpect(status().isBadRequest());
        verify(subscriptionService, times(0)).createSubscription(any(), any());
    }

    @Test
    void createSubscriptionTest_InvalidDuration() throws Exception {
        // Arrange
        JsonObject json = new JsonObject();
        json.addProperty("subscriberType", "PWRVIEW");
        json.addProperty("resourceType", "ENERGY_RECORDSET_1HZ");
        json.addProperty("durationSeconds", 0);
        JsonArray arr = new JsonArray();
        JsonObject systemJson1 = new JsonObject();
        systemJson1.addProperty("systemType", "ESS");
        systemJson1.addProperty("systemId", "sample_system_id1");
        arr.add(systemJson1);
        json.add("systems", arr);

        // Action & Assert
        mvc.perform(
                        post("/v1/subscriptions")
                                .content(json.toString())
                                .header("Content-Type", "application/json")
                                .header(
                                        CallerMetadataRequestHeaders.COGNITO_CLIENT_ID
                                                .getHeaderName(),
                                        "sample_client_id"))
                .andExpect(status().isBadRequest());
        verify(subscriptionService, times(0)).createSubscription(any(), any());
    }

    @Test
    void getRemainingQuotaInSecondsTest() throws Exception {
        // Arrange
        Mockito.when(
                        subscriptionService.getRemainingQuotaInSeconds(
                                SubscriberType.PWRVIEW,
                                SystemType.ESS,
                                ResourceType.ENERGY_RECORDSET_1HZ,
                                "sample_system_id"))
                .thenReturn(new Quota(10, 5));

        // Action & Assert
        mvc.perform(
                        get(
                                "/v1/quota?subscriberType=PWRVIEW&systemType=ESS&resourceType=ENERGY_RECORDSET_1HZ&systemId=sample_system_id"))
                .andExpect(status().isOk());
        verify(subscriptionService, times(1))
                .getRemainingQuotaInSeconds(
                        SubscriberType.PWRVIEW,
                        SystemType.ESS,
                        ResourceType.ENERGY_RECORDSET_1HZ,
                        "sample_system_id");
    }
}
