package com.generac.ces.systemgateway.service;

import static com.generac.ces.systemgateway.model.enums.DeviceSetting.ENABLE_PVRSS;
import static com.generac.ces.systemgateway.model.enums.DeviceSetting.EXPORT_OVERRIDE;
import static com.generac.ces.systemgateway.model.enums.DeviceSetting.GENERATOR_CONTROL_MODE;
import static com.generac.ces.systemgateway.model.enums.DeviceSetting.ISLANDING;
import static com.generac.ces.systemgateway.model.enums.DeviceSetting.NUM_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.generac.ces.common.client.WebClientFactory;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass.InverterSetting.CTCalibration;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode;
import com.generac.ces.system.control.subcontrol.PvLinkControl;
import com.generac.ces.system.control.subcontrol.Subcontrol;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.exception.GatewayTimeoutException;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.model.BaseBatterySetting;
import com.generac.ces.systemgateway.model.BatterySettingResponseDto;
import com.generac.ces.systemgateway.model.EventsListResponse;
import com.generac.ces.systemgateway.model.ExportLimitRequest;
import com.generac.ces.systemgateway.model.InverterSettingResponseDto;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.model.OdinSystemModeSettingResponse;
import com.generac.ces.systemgateway.model.PvlSettingResponseDto;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.Actor;
import com.generac.ces.systemgateway.model.common.CancellationType;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.common.EventStatus;
import com.generac.ces.systemgateway.model.common.SubControlType;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.enums.NotificationType;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateResponse;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateResponse;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = ConfigDataApplicationContextInitializer.class,
        classes = {ObjectMapper.class})
public class OdinServiceTest {
    private MockWebServer mockOdinWebClient;
    private MockWebServer mockEssSystemServiceWebClient;
    private MockWebServer mockEssDataProviderWebClient;
    @Autowired ObjectMapper objectMapper;
    @Mock private EssSystemService essSystemService;
    @Mock private EssDataProviderService essDataProviderService;
    @InjectMocks private OdinService odinService;
    @Mock SystemSettingCacheService<DeviceSetting> systemSettingCacheService;

    @Before
    public void setUp() throws IOException {
        mockOdinWebClient = new MockWebServer();
        mockEssSystemServiceWebClient = new MockWebServer();
        mockEssDataProviderWebClient = new MockWebServer();
        mockOdinWebClient.start();
        mockEssSystemServiceWebClient.start();
        mockEssDataProviderWebClient.start();

        CacheStore<SystemResponse> cache =
                new CacheStore<SystemResponse>(10000, 5, TimeUnit.SECONDS);

        objectMapper = new ObjectMapper();

        WebClient odinServiceWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockOdinWebClient.getHostName(), mockOdinWebClient.getPort()));

        WebClient essSystemServiceWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockEssSystemServiceWebClient.getHostName(),
                                mockEssSystemServiceWebClient.getPort()));

        WebClient essDataProviderWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockEssDataProviderWebClient.getHostName(),
                                mockEssDataProviderWebClient.getPort()));

        essSystemService =
                new EssSystemService(
                        essSystemServiceWebClient,
                        essDataProviderWebClient,
                        essDataProviderService,
                        cache,
                        null,
                        systemSettingCacheService);
        odinService =
                new OdinService(
                        odinServiceWebClient,
                        essSystemService,
                        new com.fasterxml.jackson.databind.ObjectMapper());

        systemSettingCacheService = new SystemSettingCacheService<>(new RedisTemplate<>());
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws IOException {
        mockOdinWebClient.shutdown();
        mockEssSystemServiceWebClient.shutdown();
        mockEssDataProviderWebClient.shutdown();
    }

    // =======================================================================================================
    //   SET ACTIVE SYSTEM MODES
    // =======================================================================================================
    @Test
    public void testSetActiveSystemModes_UnsuccessfulReturnsCorrectException() {
        // Arrange
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "pwrview";
        String userId = "dummyUserId";

        ActiveSystemModeUpdateRequest mockControllerRequest =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELF_SUPPLY));

        String expected =
                String.format(
                        "Failed to set Active System Mode Configuration for systemId %s.",
                        systemId);

        mockOdinWebClient.enqueue(new MockResponse().setResponseCode(500));

        // Action & Assert
        try {
            odinService
                    .setActiveSystemModes(systemId, mockControllerRequest, callerId, userId)
                    .block();
            fail("Expected to throw '500: " + expected + "'");
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(InternalServerException.class, e.getClass());
        }
    }

    @Test
    public void testSetActiveSystemModes_SuccessfulChangeReturnsActiveSystemModesInResponse()
            throws IOException {
        // Arrange
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "pwrview";
        String userId = "dummyUserId";
        String code = "SUCCESS";
        String updateId = UUID.randomUUID().toString();

        ArrayNode activeSystemModes = objectMapper.createArrayNode();
        activeSystemModes.add("SELF_SUPPLY");
        activeSystemModes.add("REMOTE_ARBITRAGE");

        ObjectNode inverter = objectMapper.createObjectNode();
        inverter.set("activeSystemModes", activeSystemModes);

        ObjectNode eventDetails = objectMapper.createObjectNode();
        eventDetails.put("eventType", "EVENT_SETTINGS_CHANGED");
        eventDetails.put("eventSettingsChangedTime", "2023-12-07T23:46:57.05176Z");
        eventDetails.set("inverter", inverter);

        ObjectNode odinResp = objectMapper.createObjectNode();
        odinResp.put("code", "SUCCESS");
        odinResp.put("id", updateId);
        odinResp.set("eventDetails", eventDetails);

        ActiveSystemModeUpdateRequest mockControllerRequest =
                new ActiveSystemModeUpdateRequest(
                        List.of(SystemMode.SELF_SUPPLY, SystemMode.REMOTE_ARBITRAGE));

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        ActiveSystemModeUpdateResponse expected =
                ActiveSystemModeUpdateResponse.builder()
                        .systemId(UUID.fromString(systemId))
                        .activeModes(List.of(SystemMode.SELF_SUPPLY, SystemMode.REMOTE_ARBITRAGE))
                        .build();

        // Action & Assert
        ActiveSystemModeUpdateResponse actual =
                odinService
                        .setActiveSystemModes(systemId, mockControllerRequest, callerId, userId)
                        .block();
        assertEquals(expected.activeModes(), actual.activeModes());
        assertEquals(UUID.fromString(systemId), actual.systemId());
        assertEquals(UUID.fromString(updateId), actual.updateId());
        assertNotNull(actual.updatedTimestampUtc());
    }

    // TODO: does odin really return 202 not 400 for "Bad request" sysMode change?
    @Test
    public void testSetActiveSystemModes_UnsuccessfulChangeDoesNotContainActiveModesInResponse() {
        // Arrange
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "pwrview";
        String userId = "dummyUserId";
        String code = "BAD_REQUEST";
        String updateId = UUID.randomUUID().toString();

        ActiveSystemModeUpdateRequest mockControllerRequest =
                new ActiveSystemModeUpdateRequest(
                        List.of(SystemMode.SELF_SUPPLY, SystemMode.REMOTE_ARBITRAGE));

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("code", code);
        odinResp.addProperty("id", updateId);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        // Action & Assert
        try {
            odinService
                    .setActiveSystemModes(systemId, mockControllerRequest, callerId, userId)
                    .block();
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }
    }

    @Test
    public void testSetActiveSystemModes_OdinTimeoutResponseThrowsBadGateway() {
        // Arrange
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "pwrview";
        String userId = "dummyUserId";
        String code = "TIMEOUT";
        String updateId = UUID.randomUUID().toString();

        ActiveSystemModeUpdateRequest mockControllerRequest =
                new ActiveSystemModeUpdateRequest(
                        List.of(SystemMode.SELF_SUPPLY, SystemMode.REMOTE_ARBITRAGE));

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("id", updateId);
        odinResp.addProperty("code", code);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        String expected =
                String.format(
                        "Did not receive a response from the device in time, id: %s.", updateId);

        // Action & Assert
        try {
            odinService
                    .setActiveSystemModes(systemId, mockControllerRequest, callerId, userId)
                    .block();
            fail("Expected to throw '504: " + expected + "'");
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(GatewayTimeoutException.class, e.getClass());
        }
    }

    @Test
    public void testSetActiveSystemModes_OdinThrowsServerError() throws IOException {
        // Arrange
        String systemId = "fae7d4a7-a74c-418b-a61a-433d92747358";
        String callerId = "pwrview";
        String userId = "dummyUserId";
        String code = "SERVER_ERROR";
        String errorMessage = "Request was not accepted by the Beacon. Error info: some info";
        String updateId = UUID.randomUUID().toString();

        ActiveSystemModeUpdateRequest mockControllerRequest =
                new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELF_SUPPLY));

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("id", updateId);
        odinResp.addProperty("code", code);
        odinResp.addProperty("errorMessage", errorMessage);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        // Action & Assert
        try {
            odinService
                    .setActiveSystemModes(systemId, mockControllerRequest, callerId, userId)
                    .block();
            fail("Expected to throw error 500");
        } catch (Exception e) {
            assertEquals(
                    String.format("Request id: %s. %s", updateId, errorMessage), e.getMessage());
            assertEquals(InternalServerException.class, e.getClass());
        }
    }

    // =======================================================================================================
    //   SET BATTERY SETTINGS
    // =======================================================================================================
    @Test
    public void testSetBatterySettings_UnsuccessfulReturnsCorrectException() throws IOException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "test";
        DeviceSettingsUpdateRequest mockRequest = DeviceSettingsUpdateRequest.builder().build();
        String expected = "Need to include at least one setting in the request.";
        mockOdinWebClient.enqueue(new MockResponse().setResponseCode(400).setBody(expected));

        // Action & Assert
        try {
            odinService
                    .setBatterySettings(
                            systemId, mockRequest, callerId, userId, List.of(providedRcpn))
                    .block();
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
            assertEquals(expected, ((BadRequestException) e).getErrorMsgs().get(0));
        }
    }

    @Test
    public void testSetBatterySettings_UnsuccessfulReturnsUnknownBattery() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "test";
        DeviceSettingsUpdateRequest mockRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(20.0)
                                                .build()))
                        .build();
        String odinResponse =
                "{\n"
                    + "    \"id\": \"bb42e751-25cc-405a-b805-c933c4975355\",\n"
                    + "    \"code\": \"SERVER_ERROR\",\n"
                    + "    \"errorMessage\": \"Request was not accepted by the Beacon. Error info:"
                    + " {\\\"errors\\\":[\\\"{\\\\\\\"errorCode\\\\\\\":\\\\\\\"UNKNOWN_CONTROL_ERROR\\\\\\\",\\\\\\\"message\\\\\\\":\\\\\\\"error"
                    + " loading device having rcpn test\\\\\\\"}\\\"]}\"\n"
                    + "}";
        mockOdinWebClient.enqueue(
                new MockResponse()
                        .setResponseCode(202)
                        .addHeader("Content-Type", "application/json")
                        .setBody(odinResponse));

        // Action & Assert
        try {
            odinService
                    .setBatterySettings(
                            systemId, mockRequest, callerId, userId, List.of(providedRcpn))
                    .block();
        } catch (Exception e) {
            assertEquals(InternalServerException.class, e.getClass());
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Request was not accepted by the Beacon"));
        }
    }

    @Test
    public void testSetBatterySettings_SuccessfulSettingsChangeReturnsNewSettingsInResponse() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "000100080A99";
        Double aChaMax = 12.0;
        Double aDisChaMax = 7.0;
        Double socRsvMax = 98.0;
        Double socRsvMin = 2.0;
        Double socMax = 99.0;
        Double socMin = 1.0;
        String code = "SUCCESS";
        String responseId = "72cf349f-b9e5-4e13-9056-fa879778b8bc";
        DeviceSettingsUpdateRequest mockRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(aChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_DISCHA_MAX)
                                                .value(aDisChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MAX)
                                                .value(socRsvMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MIN)
                                                .value(socRsvMin)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(socMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(socMin)
                                                .build()))
                        .build();

        String odinResponse =
                """
                    {
                        "id": "72cf349f-b9e5-4e13-9056-fa879778b8bc",
                        "code": "SUCCESS",
                        "eventDetails": {
                            "eventType": "EVENT_SETTINGS_CHANGED",
                            "battery": {
                                "setting": [{
                                    "socMax": 99.0,
                                    "socMin": 1.0,
                                    "socRsvMax": 98.0,
                                    "socRsvMin": 2.0,
                                    "aChaMax": 12.0,
                                    "aDisChaMax": 7.0,
                                    "rcpn": "000100080A99"
                                }]
                            }
                        }
                    }
                """;

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResponse);
        mockOdinWebClient.enqueue(mockResponseOdin);

        OdinResponse expected = new OdinResponse();
        expected.setId(responseId);
        BatterySettingResponseDto.OdinBatterySetting expectedSetting =
                BatterySettingResponseDto.OdinBatterySetting.builder()
                        .batterySetting(
                                BaseBatterySetting.builder()
                                        .aChaMax(aChaMax)
                                        .aDisChaMax(aDisChaMax)
                                        .socMax(socMax)
                                        .socMin(socMin)
                                        .socRsvMax(socRsvMax)
                                        .socRsvMin(socRsvMin)
                                        .build())
                        .rcpn(providedRcpn)
                        .build();
        expected.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(List.of(expectedSetting))
                                        .build())
                        .build());

        expected.setCode(code);

        // Action
        OdinResponse actual =
                odinService
                        .setBatterySettings(
                                systemId, mockRequest, callerId, userId, List.of(providedRcpn))
                        .block();

        // Assert
        assertNotNull(expected.getEventDetails());
        assert actual != null;
        assertNotNull(actual.getEventDetails());
        assertEquals(expected, actual);
    }

    @Test
    public void testSetBatterySettings_SuccessfulSettingsChangeWithNullProvidedRcpn() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String retrievedRcpn = "000100080A99";
        Double aChaMax = 12.0;
        Double aDisChaMax = 7.0;
        Double socRsvMax = 98.0;
        Double socRsvMin = 2.0;
        Double socMax = 99.0;
        Double socMin = 1.0;
        String code = "SUCCESS";
        String responseId = "72cf349f-b9e5-4e13-9056-fa879778b8bc";
        DeviceSettingsUpdateRequest mockRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(aChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_DISCHA_MAX)
                                                .value(aDisChaMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MAX)
                                                .value(socRsvMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_RSV_MIN)
                                                .value(socRsvMin)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MAX)
                                                .value(socMax)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.SOC_MIN)
                                                .value(socMin)
                                                .build()))
                        .build();

        String essSystemResponse =
                """
                    {
                        "systemId": "fae7d4a7-a74c-418b-a61a-433d92747358",
                        "eventSettingsChanged": {
                            "batteries": [
                                {
                                    "id": "000100080A99",
                                    "socMax": 99.0,
                                    "socMin": 1.0,
                                    "socRsvMax": 98.0,
                                    "socRsvMin": 2.0,
                                    "aChaMax": 12.0,
                                    "aDisChaMax": 7.0
                                }
                            ]
                        }
                    }
                """;

        MockResponse mockResponseEssSystem = new MockResponse();
        mockResponseEssSystem.addHeader("Content-Type", "application/json");
        mockResponseEssSystem.setStatus("HTTP/1.1 202");
        mockResponseEssSystem.setBody(essSystemResponse);

        mockEssSystemServiceWebClient.enqueue(mockResponseEssSystem);

        String odinResponse =
                """
                    {
                        "id": "72cf349f-b9e5-4e13-9056-fa879778b8bc",
                        "code": "SUCCESS",
                        "eventDetails": {
                            "eventType": "EVENT_SETTINGS_CHANGED",
                            "battery": {
                                "setting": [{
                                    "socMax": 99.0,
                                    "socMin": 1.0,
                                    "socRsvMax": 98.0,
                                    "socRsvMin": 2.0,
                                    "aChaMax": 12.0,
                                    "aDisChaMax": 7.0,
                                    "rcpn": "000100080A99"
                                }]
                            }
                        }
                    }
                """;

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResponse);
        mockOdinWebClient.enqueue(mockResponseOdin);

        OdinResponse expected = new OdinResponse();
        expected.setId(responseId);
        BatterySettingResponseDto.OdinBatterySetting expectedSetting =
                BatterySettingResponseDto.OdinBatterySetting.builder()
                        .batterySetting(
                                BaseBatterySetting.builder()
                                        .aChaMax(aChaMax)
                                        .aDisChaMax(aDisChaMax)
                                        .socMax(socMax)
                                        .socMin(socMin)
                                        .socRsvMax(socRsvMax)
                                        .socRsvMin(socRsvMin)
                                        .build())
                        .rcpn(retrievedRcpn)
                        .build();
        expected.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .battery(
                                BatterySettingResponseDto.builder()
                                        .setting(List.of(expectedSetting))
                                        .build())
                        .build());

        expected.setCode(code);

        // Action
        OdinResponse actual =
                odinService
                        .setBatterySettings(systemId, mockRequest, callerId, userId, null)
                        .block();

        // Assert
        assertNotNull(expected.getEventDetails());
        assert actual != null;
        assertNotNull(actual.getEventDetails());
        assertEquals(expected, actual);
    }

    @Test
    public void testSetBatterySettings_UnsuccessfulSettingsChangeUnknownResponse() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String providedRcpn = "test";
        String code = "SERVER_ERROR";
        String errorMessage = "Server error.";
        String odinTxnId = UUID.randomUUID().toString();
        DeviceSettingsUpdateRequest mockRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.A_CHA_MAX)
                                                .value(20.0)
                                                .build()))
                        .build();

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("id", odinTxnId);
        odinResp.addProperty("code", code);
        odinResp.addProperty("errorMessage", errorMessage);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        // Action & Assert
        try {
            odinService
                    .setBatterySettings(
                            systemId, mockRequest, callerId, userId, List.of(providedRcpn))
                    .block();
        } catch (Exception e) {
            assertEquals(InternalServerException.class, e.getClass());
            assertEquals(
                    String.format("Request id: %s. %s", odinTxnId, errorMessage), e.getMessage());
        }
    }

    // =======================================================================================================
    //   SET SYSTEM MODE (Instant Control)
    // =======================================================================================================
    @Test
    public void testSetSystemMode_UnsuccessfulReturnsCorrectException() throws IOException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";

        SystemModeUpdateRequest mockRequest =
                SystemModeUpdateRequest.builder().systemMode(SystemMode.SELF_SUPPLY).build();
        String expected = "Failed to send System Mode request for systemId: " + systemId;
        mockOdinWebClient.enqueue(new MockResponse().setResponseCode(500));

        // Action & Assert
        try {
            odinService.postSysModeControlMessages(systemId, mockRequest, callerId, userId, true);
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(InternalServerException.class, e.getClass());
        }
    }

    @Test
    public void testSetSystemMode_SuccessfulSysModeChangeReturnsNewSysModeInResponse()
            throws IOException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String id = "178beb31-f82e-4e7a-a8c2-a67e132a086a";
        String code = "SUCCESS";
        SystemMode sysMode = SystemMode.SELF_SUPPLY;

        SystemModeUpdateRequest requestDto =
                SystemModeUpdateRequest.builder().systemMode(sysMode).build();

        OdinResponse.EventSettingsChanged eventSettingsChanged =
                new OdinResponse.EventSettingsChanged();
        eventSettingsChanged.setSystemMode(new OdinSystemModeSettingResponse(sysMode, false));
        eventSettingsChanged.setEventType(NotificationType.EVENT_SETTINGS_CHANGED);

        Gson gson = new Gson();
        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("code", code);
        odinResp.addProperty("id", id);
        JsonObject eventSettingsChangedJson = new JsonObject();
        eventSettingsChangedJson.add(
                "eventType", gson.toJsonTree(eventSettingsChanged.getEventType()));
        eventSettingsChangedJson.add(
                "systemMode", gson.toJsonTree(eventSettingsChanged.getSystemMode()));
        odinResp.add("eventDetails", eventSettingsChangedJson);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        SystemModeUpdateResponse expected =
                SystemModeUpdateResponse.builder()
                        .systemMode(sysMode)
                        .systemId(systemId)
                        .updateId(UUID.fromString(id))
                        .build();

        // Action & Assert
        ResponseEntity<SystemModeUpdateResponse> actual =
                odinService
                        .postSysModeControlMessages(systemId, requestDto, callerId, userId, true)
                        .block();
        assertEquals(expected, actual.getBody());
    }

    @Test
    public void testSetSystemMode_UnsuccessfulSysModeChangeDoesNotContainSysModeInResponse() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String code = "BAD_REQUEST";
        SystemMode sysMode = SystemMode.REMOTE_ARBITRAGE;

        SystemModeUpdateRequest requestDto =
                SystemModeUpdateRequest.builder().systemMode(sysMode).build();

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("code", code);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202"); // Odin responds with a 202 even when it fails
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        // Action & Assert
        assertThrows(
                BadRequestException.class,
                () ->
                        odinService
                                .postSysModeControlMessages(
                                        systemId, requestDto, callerId, userId, true)
                                .block());
    }

    @Test
    public void testSetSystemMode_NullSysmode() throws IOException {
        // Arrange
        String systemId = "test_system_id";
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        SystemModeUpdateRequest mockRequest = SystemModeUpdateRequest.builder().build();
        String expected = "Invalid UUID string: " + systemId;
        mockOdinWebClient.enqueue(new MockResponse().setResponseCode(500));

        // Action & Assert
        try {
            odinService.postSysModeControlMessages(
                    UUID.fromString(systemId), mockRequest, callerId, userId, true);
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void testSetSystemMode_OdinTimeoutResponseThrowsBadGateway() throws IOException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        String userId = "dummyUserId";
        String code = "TIMEOUT";
        String updateId = UUID.randomUUID().toString();
        SystemMode sysMode = SystemMode.SELF_SUPPLY;

        SystemModeUpdateRequest requestDto =
                SystemModeUpdateRequest.builder().systemMode(sysMode).build();

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("code", code);
        odinResp.addProperty("id", updateId);

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202"); // Odin responds with a 202 even when it fails
        mockResponseOdin.setBody(odinResp.toString());
        mockOdinWebClient.enqueue(mockResponseOdin);

        String expected =
                String.format(
                        "Did not receive a response from the device in time, id: %s.", updateId);

        // Action & Assert
        try {
            odinService
                    .postSysModeControlMessages(systemId, requestDto, callerId, userId, true)
                    .block();

            fail("Expected to throw '504: " + expected + "'");
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(GatewayTimeoutException.class, e.getClass());
        }
    }

    // =======================================================================================================
    //   POST SYSTEM CONTROL MESSAGE FOR SYSTEM MODE
    // =======================================================================================================
    @Test
    public void testSystemModeScheduleControl_success() throws InterruptedException {
        String userId = "dummyUserId";

        MockResponse mockResponse =
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(200)
                        .setBody(
                                "{\n"
                                        + "    \"id\": \"6cba3865-b254-4a35-887b-643660027e3c\",\n"
                                        + "    \"code\": \"SUCCESS\"\n"
                                        + "}");

        mockOdinWebClient.enqueue(mockResponse);

        SystemModeUpdateRequest request = validSystemModeScheduleControlRequest();
        ResponseEntity<SystemModeUpdateResponse> response =
                odinService
                        .postSysModeControlMessages(
                                UUID.randomUUID(), request, "pwrview", userId, false)
                        .block();

        assert response != null;
        assertEquals(
                "6cba3865-b254-4a35-887b-643660027e3c", response.getBody().updateId().toString());
        mockOdinWebClient.takeRequest();
    }

    @Test
    public void testSystemModeInstantControl_success() throws InterruptedException {
        String userId = "dummyUserId";

        MockResponse mockResponse =
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(202)
                        .setBody(
                                "{\n"
                                        + "    \"id\": \"6cba3865-b254-4a35-887b-643660027e3c\", \n"
                                        + "    \"code\": \"SUCCESS\", \n"
                                        + "    \"eventDetails\": { \n"
                                        + "        \"eventType\": \"EVENT_SETTINGS_CHANGED\","
                                        + "        \"eventSettingsChangedTime\":"
                                        + " \"2023-08-11T22:27:33.766172Z\", \n"
                                        + "        \"systemMode\": { \n"
                                        + "            \"systemMode\": \"SELF_SUPPLY\", \n"
                                        + "            \"cancelOnExternalChange\": false \n"
                                        + "        } \n"
                                        + "    } \n"
                                        + "}");

        mockOdinWebClient.enqueue(mockResponse);

        SystemModeUpdateRequest request = validSystemModeInstantControlRequest();
        ResponseEntity<SystemModeUpdateResponse> response =
                odinService
                        .postSysModeControlMessages(
                                UUID.randomUUID(), request, "pwrview", userId, true)
                        .block();

        assert response != null;
        assertEquals(
                "6cba3865-b254-4a35-887b-643660027e3c", response.getBody().updateId().toString());

        mockOdinWebClient.takeRequest();
    }

    @Test
    public void testSystemModeScheduleControl_OdinRejectedWithBadRequest()
            throws InterruptedException {
        String userId = "dummyUserId";

        MockResponse mockResponse =
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(400);

        mockOdinWebClient.enqueue(mockResponse);

        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        SystemModeUpdateRequest request = validSystemModeScheduleControlRequest();
        Mono<ResponseEntity<SystemModeUpdateResponse>> odinResponse =
                odinService.postSysModeControlMessages(systemId, request, "pwrview", userId, false);
        ResponseStatusException exception =
                Assert.assertThrows(ResponseStatusException.class, odinResponse::block);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        mockOdinWebClient.takeRequest();
    }

    @Test
    public void testSystemModeInstantControl_OdinRejectedWithBadRequest()
            throws InterruptedException {
        String userId = "dummyUserId";

        MockResponse mockResponse =
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(400);

        mockOdinWebClient.enqueue(mockResponse);

        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        SystemModeUpdateRequest request = validSystemModeInstantControlRequest();
        Mono<ResponseEntity<SystemModeUpdateResponse>> odinResponse =
                odinService.postSysModeControlMessages(systemId, request, "pwrview", userId, true);
        ResponseStatusException exception =
                Assert.assertThrows(ResponseStatusException.class, odinResponse::block);

        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatus().value());

        mockOdinWebClient.takeRequest();
    }

    @Test
    public void testSystemModeInstantControl_OdinTimeoutResponseThrowsBadGateway() {
        String userId = "dummyUserId";
        String odinTxnId = UUID.randomUUID().toString();

        JsonObject odinResp = new JsonObject();
        odinResp.addProperty("id", odinTxnId);
        odinResp.addProperty("code", "TIMEOUT");

        MockResponse mockResponse =
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setBody(odinResp.toString())
                        .setResponseCode(202);

        mockOdinWebClient.enqueue(mockResponse);

        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        SystemModeUpdateRequest request = validSystemModeInstantControlRequest();
        Mono<ResponseEntity<SystemModeUpdateResponse>> odinResponseMono =
                odinService.postSysModeControlMessages(systemId, request, "pwrview", userId, true);
        GatewayTimeoutException exception =
                assertThrows(GatewayTimeoutException.class, odinResponseMono::block);

        String expected =
                String.format(
                        "Did not receive a response from the device in time, id: %s.", odinTxnId);
        Assertions.assertEquals(expected, exception.getMessage());
    }

    private SystemModeUpdateRequest validSystemModeScheduleControlRequest() {
        return SystemModeUpdateRequest.builder()
                .systemMode(SystemMode.SELL)
                .startTime(OffsetDateTime.now())
                .duration(5L)
                .build();
    }

    private SystemModeUpdateRequest validSystemModeInstantControlRequest() {
        return SystemModeUpdateRequest.builder().systemMode(SystemMode.SELL).build();
    }

    // =======================================================================================================
    //   SET EXPORT LIMIT
    // =======================================================================================================
    @Test
    public void testSetExportLimit_UnsuccessfulReturnsCorrectException() throws IOException {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String callerId = "97893ef0-d42f-453b-ae9a-3874266f29e4";
        ExportLimitRequest mockRequest = new ExportLimitRequest();
        String expected = "Failed to set Export Limit Setpoint for systemId: " + systemId;
        mockOdinWebClient.enqueue(new MockResponse().setResponseCode(500));

        // Action & Assert
        try {
            odinService.setExportLimit(String.valueOf(systemId), mockRequest, callerId).block();
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(InternalServerException.class, e.getClass());
        }
    }

    // =======================================================================================================
    //   GET EVENTS LIST
    // =======================================================================================================
    @Test
    public void testGetEventsList_systemModeEventSuccess() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        List<Actor> actorIds = List.of(Actor.fromActorId("pwrfleet"));
        List<Subcontrol.SubControlType> subControlTypes =
                List.of(Subcontrol.SubControlType.SYSTEM_MODE);
        String start = DateTime.now().toString();
        String end = DateTime.now().toString();

        String messageId = "9069ddb8-312d-4da7-927c-7e36a81fe6e2";
        OffsetDateTime requestTimestamp =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606546), ZoneOffset.UTC);
        OffsetDateTime startTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606554), ZoneOffset.UTC);
        OffsetDateTime endTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606558), ZoneOffset.UTC);
        OffsetDateTime scheduleStartTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606546), ZoneOffset.UTC);
        OffsetDateTime scheduleEndTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606546), ZoneOffset.UTC);

        String mockOdinResponse =
                """
                {
                    "events": {
                        "actorEvents": [
                            {
                                "actor": {
                                    "actorId": "pwrfleet",
                                    "priority": 50,
                                    "errors": []
                                },
                                "events": [
                                    {
                                        "messageId": "9069ddb8-312d-4da7-927c-7e36a81fe6e2",
                                        "requestTimestamp": "2023-10-06T15:35:46Z",
                                        "sysmode": {
                                            "setting": {
                                                "cancelOnExternalChange": false,
                                                "touEnabled": true
                                            },
                                            "reversionSetting": {
                                                "systemMode": "SELL"
                                            }
                                        },
                                        "status": "COMPLETE",
                                        "eventSchedule": {
                                            "scheduleStartTime": "2023-10-06T15:35:46Z",
                                            "scheduleEndTime": "2023-10-06T15:35:46Z"
                                        },
                                        "eventActual": {
                                            "startTime": "2023-10-06T15:35:54Z",
                                            "endTime": "2023-10-06T15:35:58Z"
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                }""";

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(mockOdinResponse);
        mockOdinWebClient.enqueue(mockResponseOdin);

        EventsListResponse expected =
                new EventsListResponse(
                        List.of(
                                EventsListResponse.Details.builder()
                                        .actor(Actor.PWRFLEET)
                                        .eventActual(
                                                new EventsListResponse.EventActual(
                                                        startTime, endTime))
                                        .eventSchedule(
                                                new EventsListResponse.EventSchedule(
                                                        scheduleStartTime, scheduleEndTime))
                                        .status(EventStatus.COMPLETE)
                                        .event(
                                                new EventsListResponse.Event(
                                                        new EventsListResponse.Setting(
                                                                SubControlType.SYSTEM_MODE,
                                                                List.of(
                                                                        new EventsListResponse
                                                                                .DeviceRequest(
                                                                                null,
                                                                                new EventsListResponse
                                                                                        .SystemModeRequest(
                                                                                        true, null,
                                                                                        false)))),
                                                        new EventsListResponse.Setting(
                                                                SubControlType.SYSTEM_MODE,
                                                                List.of(
                                                                        new EventsListResponse
                                                                                .DeviceRequest(
                                                                                null,
                                                                                new EventsListResponse
                                                                                        .SystemModeRequest(
                                                                                        null,
                                                                                        SystemMode
                                                                                                .SELL,
                                                                                        null))))))
                                        .messageId(messageId)
                                        .requestTimestamp(requestTimestamp)
                                        .cancellationType(CancellationType.UNKNOWN_CANCEL_TYPE)
                                        .build()));

        // Action
        EventsListResponse actual =
                odinService
                        .getEventsList(systemId.toString(), actorIds, subControlTypes, start, end)
                        .block();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testGetEventsList_batterySettingEventSuccess() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        List<Actor> actorIds = List.of(Actor.fromActorId("pwrfleet"));
        List<Subcontrol.SubControlType> subControlTypes =
                List.of(Subcontrol.SubControlType.BATTERY);
        String start = DateTime.now().toString();
        String end = DateTime.now().toString();

        String messageId = "9069ddb8-312d-4da7-927c-7e36a81fe6e2";
        OffsetDateTime requestTimestamp =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606546), ZoneOffset.UTC);
        OffsetDateTime startTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606554), ZoneOffset.UTC);
        OffsetDateTime endTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606558), ZoneOffset.UTC);
        OffsetDateTime scheduleStartTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606546), ZoneOffset.UTC);
        OffsetDateTime scheduleEndTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606546), ZoneOffset.UTC);

        String mockOdinResponse =
                """
                {
                    "events": {
                        "actorEvents": [
                            {
                                "actor": {
                                    "actorId": "pwrfleet",
                                    "priority": 50,
                                    "errors": []
                                },
                                "events": [
                                    {
                                        "messageId": "9069ddb8-312d-4da7-927c-7e36a81fe6e2",
                                        "requestTimestamp": "2023-10-06T15:35:46Z",
                                        "battery": {
                                            "setting": [
                                                {
                                                    "state": "STATE_ENABLED",
                                                    "cancelOnExternalChange": false,
                                                    "rcpn": "1-2-3-4-5",
                                                    "soc_max": 1.0,
                                                    "soc_min": 2.0,
                                                    "soc_rsv_max": 3.0,
                                                    "soc_rsv_min": 4.0,
                                                    "a_cha_max": 5.0,
                                                    "a_dis_cha_max": 6.0
                                                }
                                            ],
                                            "reversionSetting": [
                                                {
                                                    "state": "STATE_DISABLED",
                                                    "cancelOnExternalChange": true,
                                                    "rcpn": "6-7-8-9-10",
                                                    "soc_max": 7.0,
                                                    "soc_min": 8.0,
                                                    "soc_rsv_max": 9.0,
                                                    "soc_rsv_min": 10.0,
                                                    "a_cha_max": 11.0,
                                                    "a_dis_cha_max": 12.0
                                                }
                                            ]
                                        },
                                        "status": "COMPLETE",
                                        "eventSchedule": {
                                            "scheduleStartTime": "2023-10-06T15:35:46Z",
                                            "scheduleEndTime": "2023-10-06T15:35:46Z"
                                        },
                                        "eventActual": {
                                            "startTime": "2023-10-06T15:35:54Z",
                                            "endTime": "2023-10-06T15:35:58Z"
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                }""";

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody(mockOdinResponse);
        mockOdinWebClient.enqueue(mockResponseOdin);

        EventsListResponse expected =
                new EventsListResponse(
                        List.of(
                                EventsListResponse.Details.builder()
                                        .actor(Actor.PWRFLEET)
                                        .eventActual(
                                                new EventsListResponse.EventActual(
                                                        startTime, endTime))
                                        .eventSchedule(
                                                new EventsListResponse.EventSchedule(
                                                        scheduleStartTime, scheduleEndTime))
                                        .status(EventStatus.COMPLETE)
                                        .event(
                                                new EventsListResponse.Event(
                                                        EventsListResponse.Setting.builder()
                                                                .subControl(SubControlType.BATTERY)
                                                                .requests(
                                                                        Collections.singletonList(
                                                                                new EventsListResponse
                                                                                        .DeviceRequest(
                                                                                        new EventsListResponse
                                                                                                .BatterySettingRequest(
                                                                                                List
                                                                                                        .of(
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_MAX,
                                                                                                                        1.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_MIN,
                                                                                                                        2.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_RSV_MAX,
                                                                                                                        3.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_RSV_MIN,
                                                                                                                        4.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .A_CHA_MAX,
                                                                                                                        5.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .A_DISCHA_MAX,
                                                                                                                        6.0)),
                                                                                                DeviceState
                                                                                                        .STATE_ENABLED,
                                                                                                "1-2-3-4-5",
                                                                                                false),
                                                                                        null)))
                                                                .build(),
                                                        EventsListResponse.Setting.builder()
                                                                .subControl(SubControlType.BATTERY)
                                                                .requests(
                                                                        Collections.singletonList(
                                                                                new EventsListResponse
                                                                                        .DeviceRequest(
                                                                                        new EventsListResponse
                                                                                                .BatterySettingRequest(
                                                                                                List
                                                                                                        .of(
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_MAX,
                                                                                                                        7.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_MIN,
                                                                                                                        8.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_RSV_MAX,
                                                                                                                        9.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .SOC_RSV_MIN,
                                                                                                                        10.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .A_CHA_MAX,
                                                                                                                        11.0),
                                                                                                                new EventsListResponse
                                                                                                                        .SettingNameValue(
                                                                                                                        DeviceSetting
                                                                                                                                .A_DISCHA_MAX,
                                                                                                                        12.0)),
                                                                                                DeviceState
                                                                                                        .STATE_DISABLED,
                                                                                                "6-7-8-9-10",
                                                                                                true),
                                                                                        null)))
                                                                .build()))
                                        .messageId(messageId)
                                        .requestTimestamp(requestTimestamp)
                                        .cancellationType(CancellationType.UNKNOWN_CANCEL_TYPE)
                                        .build()));

        // Action
        EventsListResponse actual =
                odinService
                        .getEventsList(systemId.toString(), actorIds, subControlTypes, start, end)
                        .block();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testGetEventsList_invalidResponseThrowsInternalServerException() {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        List<Actor> actorIds = List.of(Actor.fromActorId("pwrfleet"));
        List<Subcontrol.SubControlType> subControlTypes =
                List.of(Subcontrol.SubControlType.SYSTEM_MODE);
        String start = DateTime.now().toString();
        String end = DateTime.now().toString();

        MockResponse mockResponseOdin = new MockResponse();
        mockResponseOdin.addHeader("Content-Type", "application/json");
        mockResponseOdin.setStatus("HTTP/1.1 202");
        mockResponseOdin.setBody("[]");
        mockOdinWebClient.enqueue(mockResponseOdin);

        // Action & Assert
        try {
            odinService
                    .getEventsList(systemId.toString(), actorIds, subControlTypes, start, end)
                    .block();
            fail("Expected InternalServerException was not thrown.");
        } catch (Exception e) {
            assertEquals(
                    "Error: Unable to process event list response for system: " + systemId,
                    e.getMessage());
            assertEquals(InternalServerException.class, e.getClass());
        }
    }

    // =======================================================================================================
    //   PATCH INVERTER SETTINGS
    // =======================================================================================================

    @Test
    public void setInverterSettings_Success() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        UUID updateId = UUID.randomUUID();
        OffsetDateTime updatedTimestamp = OffsetDateTime.now(ZoneOffset.UTC);

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(ISLANDING)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_OVERRIDE)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES)
                                                .value(2.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.LOAD_SHEDDING)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.GENERATOR_CONTROL_MODE)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.CT_CALIBRATION)
                                                .value(0.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.EXPORT_POWER_LIMIT)
                                                .value(10.0)
                                                .build()))
                        .build();

        OdinResponse odinResponseExpected = new OdinResponse();
        odinResponseExpected.setId(updateId.toString());
        odinResponseExpected.setCode("SUCCESS");
        odinResponseExpected.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .eventSettingsChangedTime(updatedTimestamp)
                        .inverter(
                                InverterSettingResponseDto.builder()
                                        .rcpn(rcpId)
                                        .islanding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .Islanding.ISLANDING_ENABLED)
                                        .exportOverride(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .ExportOverride.EXPORT_OVERRIDE_DISABLED)
                                        .numberOfTransferSwitches(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .NumberOfTransferSwitches
                                                        .NUMBER_OF_TRANSFER_SWITCHES_TWO)
                                        .loadShedding(
                                                InverterSettingControlOuterClass.InverterSetting
                                                        .LoadShedding.LOAD_SHEDDING_DISABLED)
                                        .generatorControlMode(
                                                GeneratorControlMode
                                                        .GENERATOR_CONTROL_MODE_SINGLE_TRANSFER)
                                        .ctCalibration(CTCalibration.CT_CALIBRATION_AUTO)
                                        .exportLimit(10)
                                        .build())
                        .build());

        ObjectNode inverter = objectMapper.createObjectNode();
        inverter.put("rcpn", rcpId);
        inverter.put("islanding", "ISLANDING_ENABLED");
        inverter.put("exportOverride", "EXPORT_OVERRIDE_DISABLED");
        inverter.put("numberOfTransferSwitches", "NUMBER_OF_TRANSFER_SWITCHES_TWO");
        inverter.put("loadShedding", "LOAD_SHEDDING_DISABLED");
        inverter.put("generatorControlMode", "GENERATOR_CONTROL_MODE_SINGLE_TRANSFER");
        inverter.put("ctCalibration", "CT_CALIBRATION_AUTO");
        inverter.put("exportLimit", "10");

        ObjectNode eventDetails = objectMapper.createObjectNode();
        eventDetails.put("eventType", "EVENT_SETTINGS_CHANGED");
        eventDetails.put("eventSettingsChangedTime", updatedTimestamp.toString());
        eventDetails.set("inverter", inverter);

        ObjectNode odinResp = objectMapper.createObjectNode();
        odinResp.put("code", "SUCCESS");
        odinResp.put("id", updateId.toString());
        odinResp.set("eventDetails", eventDetails);

        mockOdinWebClient.enqueue(
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(202)
                        .setBody(odinResp.toString()));

        // Action
        OdinResponse result =
                odinService
                        .setInverterSettings(
                                systemId, deviceSettingsUpdateRequest, callerId, userId)
                        .block();

        // Assert
        assertEquals(odinResponseExpected, result);
    }

    @Test
    public void setInverterSettings_ValueOOR_BadRequest_ISLANDING_ENABLED() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(ISLANDING)
                                                .value(2.0)
                                                .build()))
                        .build();

        // Action
        try {
            odinService
                    .setInverterSettings(systemId, deviceSettingsUpdateRequest, callerId, userId)
                    .block();
            Assert.fail("Expected GatewayTimeoutException was not thrown");
        } catch (BadRequestException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo("Invalid value for Islanding: " + 2.0 + ". Value should be 0 or 1.");
        }
    }

    @Test
    public void setInverterSettings_ValueOOR_BadRequest_EXPORT_OVERRIDE() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(EXPORT_OVERRIDE)
                                                .value(2.0)
                                                .build()))
                        .build();

        // Action
        try {
            odinService
                    .setInverterSettings(systemId, deviceSettingsUpdateRequest, callerId, userId)
                    .block();
            Assert.fail("Expected GatewayTimeoutException was not thrown");
        } catch (BadRequestException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo(
                            "Invalid value for ExportOverride: "
                                    + 2.0
                                    + ". Value should be 0 or 1.");
        }
    }

    @Test
    public void setInverterSettings_ValueOOR_BadRequest_GENERATOR_CONTROL_MODE() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(GENERATOR_CONTROL_MODE)
                                                .value(4.0)
                                                .build()))
                        .build();

        // Action
        try {
            odinService
                    .setInverterSettings(systemId, deviceSettingsUpdateRequest, callerId, userId)
                    .block();
            Assert.fail("Expected GatewayTimeoutException was not thrown");
        } catch (BadRequestException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo("No enum constant with value: 4 in GeneratorControlMode");
        }
    }

    // =======================================================================================================
    //   PATCH PVL SETTINGS
    // =======================================================================================================
    @Test
    public void setPvlSettings_Success() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";
        String rcpId = "dummyRcpId";
        UUID updateId = UUID.randomUUID();
        OffsetDateTime updatedTimestamp = OffsetDateTime.now(ZoneOffset.UTC);

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(ENABLE_PVRSS)
                                                .value(1.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.VIN_STARTUP)
                                                .value(120.0)
                                                .build(),
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(DeviceSetting.NUM_STRING)
                                                .value(2.0)
                                                .build()))
                        .build();

        OdinResponse odinResponseExpected = new OdinResponse();
        odinResponseExpected.setId(updateId.toString());
        odinResponseExpected.setCode("SUCCESS");
        odinResponseExpected.setEventDetails(
                OdinResponse.EventSettingsChanged.builder()
                        .eventType(NotificationType.EVENT_SETTINGS_CHANGED)
                        .eventSettingsChangedTime(updatedTimestamp)
                        .pvLink(
                                PvlSettingResponseDto.builder()
                                        .rcpn(rcpId)
                                        .pvrssState(
                                                PvLinkControl.PVLinkSetting.PVRSSState
                                                        .PVRSS_STATE_ON)
                                        .minimumInputVoltage(120.0)
                                        .numberOfSubstrings(
                                                PvLinkControl.PVLinkSetting.NumberOfSubstrings
                                                        .NUMBER_OF_SUBSTRINGS_TWO)
                                        .build())
                        .build());

        ObjectNode pvl = objectMapper.createObjectNode();
        pvl.put("rcpn", rcpId);
        pvl.put("pvrssState", "PVRSS_STATE_ON");
        pvl.put("minimumInputVoltage", 120.0);
        pvl.put("numberOfSubstrings", "NUMBER_OF_SUBSTRINGS_TWO");

        ObjectNode eventDetails = objectMapper.createObjectNode();
        eventDetails.put("eventType", "EVENT_SETTINGS_CHANGED");
        eventDetails.put("eventSettingsChangedTime", updatedTimestamp.toString());
        eventDetails.set("pvLink", pvl);

        ObjectNode odinResp = objectMapper.createObjectNode();
        odinResp.put("code", "SUCCESS");
        odinResp.put("id", updateId.toString());
        odinResp.set("eventDetails", eventDetails);

        mockOdinWebClient.enqueue(
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(202)
                        .setBody(odinResp.toString()));

        // Action
        OdinResponse result =
                odinService
                        .setPvLinkSettings(
                                systemId, deviceSettingsUpdateRequest, callerId, userId, rcpId)
                        .block();

        // Assert
        assertEquals(odinResponseExpected, result);
    }

    @Test
    public void setPvlSettings_ValueOOR_BadRequest_ENABLE_PVRSS() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";
        String providedRcpn = "test";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(ENABLE_PVRSS)
                                                .value(2.0)
                                                .build()))
                        .build();

        // Action
        try {
            odinService
                    .setPvLinkSettings(
                            systemId, deviceSettingsUpdateRequest, callerId, userId, providedRcpn)
                    .block();
            Assert.fail("Expected GatewayTimeoutException was not thrown");
        } catch (BadRequestException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo(
                            "Invalid value for EnablePvrss: " + 2.0 + ". Value should be 0 or 1.");
        }
    }

    @Test
    public void setPvlSettings_ValueOOR_BadRequest_NUM_STRING() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String callerId = "testCaller";
        String userId = "testUser";
        String providedRcpn = "test";

        DeviceSettingsUpdateRequest deviceSettingsUpdateRequest =
                DeviceSettingsUpdateRequest.builder()
                        .settings(
                                List.of(
                                        DeviceSettingsUpdateRequest.DeviceSettingChange.builder()
                                                .name(NUM_STRING)
                                                .value(3.0)
                                                .build()))
                        .build();

        // Action
        try {
            odinService
                    .setPvLinkSettings(
                            systemId, deviceSettingsUpdateRequest, callerId, userId, providedRcpn)
                    .block();
            Assert.fail("Expected GatewayTimeoutException was not thrown");
        } catch (BadRequestException e) {
            // Assert
            assertThat(e.getMessage())
                    .isEqualTo("No enum constant with value: " + 3 + " in NumberOfSubstrings");
        }
    }
}
