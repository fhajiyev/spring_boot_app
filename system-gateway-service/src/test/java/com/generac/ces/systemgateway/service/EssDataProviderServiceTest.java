package com.generac.ces.systemgateway.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.common.client.WebClientFactory;
import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.essdataprovider.model.ModeResponseDto;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import java.io.IOException;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = ConfigDataApplicationContextInitializer.class,
        classes = {ObjectMapper.class})
public class EssDataProviderServiceTest {
    private MockWebServer mockEssDataProviderWebClient;

    private EssDataProviderService essDataProviderService;

    @Before
    public void setUp() throws IOException {
        mockEssDataProviderWebClient = new MockWebServer();
        mockEssDataProviderWebClient.start();

        WebClient essDataProviderWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockEssDataProviderWebClient.getHostName(),
                                mockEssDataProviderWebClient.getPort()));

        essDataProviderService = new EssDataProviderService(essDataProviderWebClient);
    }

    @After
    public void tearDown() throws IOException {
        mockEssDataProviderWebClient.shutdown();
    }

    @Test
    public void testgetAllowedSysModesByInverter_UnsuccessfulReturnsCorrectException()
            throws InterruptedException {
        // Arrange
        String testDeviceRcpn = "test_device_rcpn";
        mockEssDataProviderWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essDataProviderService.getAllowedSysModesByInverter(testDeviceRcpn).block();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains(testDeviceRcpn));
            Assertions.assertEquals(ResourceNotFoundException.class, e.getClass());
        }

        mockEssDataProviderWebClient.takeRequest();
    }

    @Test
    public void testCurrentSysMode_Successful() throws InterruptedException {
        mockEssDataProviderWebClient.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\n"
                                        + "    \"mode\": 3,\n"
                                        + "    \"modeName\": \"clean backup\",\n"
                                        + "    \"sysMode\": \"CleanBackup\"\n"
                                        + "}"));

        ModeResponseDto modeResponseDto =
                essDataProviderService.getCurrentSysMode(UUID.randomUUID()).block();

        assert modeResponseDto != null;
        Assertions.assertEquals(SysModes.CleanBackup, modeResponseDto.getSysMode());

        mockEssDataProviderWebClient.takeRequest();
    }

    @Test
    public void testCurrentSysMode_Error() throws InterruptedException {
        mockEssDataProviderWebClient.enqueue(new MockResponse().setResponseCode(404));

        UUID systemId = UUID.randomUUID();
        Mono<ModeResponseDto> resp = essDataProviderService.getCurrentSysMode(systemId);
        assertThrows(ResourceNotFoundException.class, resp::block);

        mockEssDataProviderWebClient.takeRequest();
    }
}
