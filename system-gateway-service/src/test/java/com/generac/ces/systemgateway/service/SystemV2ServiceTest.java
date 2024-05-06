package com.generac.ces.systemgateway.service;

import static org.junit.Assert.fail;

import com.generac.ces.common.client.WebClientFactory;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.service.system.SystemV2Service;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = ConfigDataApplicationContextInitializer.class,
        classes = {ObjectMapper.class})
public class SystemV2ServiceTest {

    private MockWebServer mockSystemV2ServiceWebClient;

    @Autowired ObjectMapper objectMapper;

    private SystemV2Service systemV2Service;
    private WebClient systemV2ServiceWebClient;

    @Before
    public void setUp() throws IOException {
        mockSystemV2ServiceWebClient = new MockWebServer();
        mockSystemV2ServiceWebClient.start();

        CacheStore<SystemResponse> cache =
                new CacheStore<SystemResponse>(10000, 5, TimeUnit.SECONDS);

        objectMapper = new ObjectMapper();

        systemV2ServiceWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockSystemV2ServiceWebClient.getHostName(),
                                mockSystemV2ServiceWebClient.getPort()));

        systemV2Service = new SystemV2Service(systemV2ServiceWebClient, cache);
    }

    @After
    public void tearDown() throws IOException {
        mockSystemV2ServiceWebClient.shutdown();
    }

    @Test
    public void testGetSystem_systemIdNotFoundInSystemServiceCallReturnsCorrectError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String expectedResponse = String.format("Penguin system id = %s not found.", systemId);
        mockSystemV2ServiceWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            systemV2Service.getSystemBySystemId(systemId);
            fail("Expected to throw '404: " + expectedResponse + "'");
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains(expectedResponse));
        }
    }

    @Test
    public void testGetSystem_internalServerExceptionInSystemServiceCallReturnsCorrectError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String expectedResponse =
                String.format(
                        "Error occurred on Penguin metadata retrieval for system id = %s.",
                        systemId);
        mockSystemV2ServiceWebClient.enqueue(new MockResponse().setResponseCode(500));

        // Action & Assert
        try {
            systemV2Service.getSystemBySystemId(systemId);
            fail("Expected to throw '500: " + expectedResponse + "'");
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains(expectedResponse));
        }
    }
}
