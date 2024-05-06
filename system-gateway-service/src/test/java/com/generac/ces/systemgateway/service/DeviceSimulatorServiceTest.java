package com.generac.ces.systemgateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.common.client.WebClientFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = ConfigDataApplicationContextInitializer.class,
        classes = {ObjectMapper.class})
public class DeviceSimulatorServiceTest {
    private MockWebServer mockDeviceSimulatorWebClient;

    @Autowired ObjectMapper objectMapper;

    private DeviceSimulatorService deviceSimulatorService;

    @Before
    public void setUp() throws IOException {
        mockDeviceSimulatorWebClient = new MockWebServer();
        mockDeviceSimulatorWebClient.start();
        objectMapper = new ObjectMapper();

        WebClient deviceSimulatorWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockDeviceSimulatorWebClient.getHostName(),
                                mockDeviceSimulatorWebClient.getPort()));

        deviceSimulatorService = new DeviceSimulatorService(deviceSimulatorWebClient);
    }

    @After
    public void tearDown() throws IOException {
        mockDeviceSimulatorWebClient.shutdown();
    }

    @Test
    public void streamRequest_test() throws Exception {
        mockDeviceSimulatorWebClient.enqueue(new MockResponse().setResponseCode(200));

        // Action & Assert
        deviceSimulatorService.startStreamingForSystemId("someSystemId");

        RecordedRequest request = mockDeviceSimulatorWebClient.takeRequest(1, TimeUnit.SECONDS);
        assertEquals(
                "someSystemId",
                objectMapper
                        .readValue(
                                request.getBody().readByteArray(),
                                DeviceSimulatorService.SimulatorStreamRequest.class)
                        .getSystemId());
    }
}
