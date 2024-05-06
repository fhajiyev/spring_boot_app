package com.generac.ces.systemgateway.service;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generac.ces.common.client.WebClientFactory;
import com.generac.ces.essdataprovider.model.BatteryPropertiesDto;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.system.DeviceTypeOuterClass;
import com.generac.ces.system.DeviceTypeOuterClass.DeviceType;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.helper.DeviceSettingsMapper;
import com.generac.ces.systemgateway.helper.JsonResponseHelper;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.Type;
import com.generac.ces.systemgateway.model.common.Unit;
import com.generac.ces.systemgateway.model.device.DeviceSettings;
import com.generac.ces.systemgateway.model.device.DeviceSettingsResponse;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = ConfigDataApplicationContextInitializer.class,
        classes = {ObjectMapper.class})
public class EssSystemServiceTest {
    private MockWebServer mockSystemServiceWebClient;
    private MockWebServer mockEssDataProviderWebClient;

    @Autowired ObjectMapper objectMapper;

    private EssSystemService essSystemService;
    private WebClient systemServiceWebClient;

    private final JsonResponseHelper responseHelper = new JsonResponseHelper();

    @Spy DeviceSettingsMapper deviceSettingsMapper = Mappers.getMapper(DeviceSettingsMapper.class);

    @Mock SystemSettingCacheService<DeviceSetting> systemSettingCacheService;

    @Before
    public void setUp() throws IOException {
        mockSystemServiceWebClient = new MockWebServer();
        mockEssDataProviderWebClient = new MockWebServer();

        mockSystemServiceWebClient.start();
        mockEssDataProviderWebClient.start();

        CacheStore<SystemResponse> cache =
                new CacheStore<SystemResponse>(10000, 5, TimeUnit.SECONDS);

        objectMapper = new ObjectMapper();

        systemServiceWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockSystemServiceWebClient.getHostName(),
                                mockSystemServiceWebClient.getPort()));

        WebClient essDataProviderWebClient =
                WebClientFactory.newRestClient(
                        String.format(
                                "http://%s:%d/",
                                mockEssDataProviderWebClient.getHostName(),
                                mockEssDataProviderWebClient.getPort()));

        essSystemService =
                new EssSystemService(
                        systemServiceWebClient,
                        essDataProviderWebClient,
                        new EssDataProviderService(essDataProviderWebClient),
                        cache,
                        deviceSettingsMapper,
                        systemSettingCacheService);
    }

    @After
    public void tearDown() throws IOException {
        mockSystemServiceWebClient.shutdown();
        mockEssDataProviderWebClient.shutdown();
    }

    // =======================================================================================================
    //   RETRIEVE BATTERY RCPNs
    // =======================================================================================================
    @Test
    public void testRetrieveBatteryRcpns_throwsNotFoundException() throws IOException {
        // Arrange
        String systemId = "systemId";
        mockSystemServiceWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.retrieveBatteryRCPNs(systemId);
        } catch (Exception e) {
            Assertions.assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    // =======================================================================================================
    //   GET BATTERY MIN/MAX
    // =======================================================================================================
    @Test
    public void testGetBatterySettings_systemIdNotFoundInSystemServiceCallReturnsCorrectError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String expectedResponse = String.format("ESS system id = %s not found.", systemId);
        mockSystemServiceWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getBatterySettings(systemId).block();
            fail("Expected to throw '404: " + expectedResponse + "'");
        } catch (Exception e) {
            assertEquals(expectedResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void testGetBatterySettings_systemIdNotFoundInEssDpCallReturnsCorrectError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String rcpId = "00010007A42A";
        String expectedResponse = String.format("ESS system id = %s not found.", systemId);

        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", rcpId);

        MockResponse mockResponseSystemService = new MockResponse();
        mockResponseSystemService.addHeader("Content-Type", "application/json");
        mockResponseSystemService.setStatus("HTTP/1.1 200");
        mockResponseSystemService.setBody(mockSystemResponse.toString());
        mockSystemServiceWebClient.enqueue(mockResponseSystemService);

        mockEssDataProviderWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getBatterySettings(systemId).block();
            fail("Expected to throw '404: " + expectedResponse + "'");
        } catch (Exception e) {
            assertEquals(expectedResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void testGetBatterySettings_happyCaseMapsCorrectResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceId = "000100080A99";
        String hostRcpn = "00010007A42A";
        List<DeviceSetting> settingNames =
                List.of(
                        DeviceSetting.A_CHA_MAX,
                        DeviceSetting.A_DISCHA_MAX,
                        DeviceSetting.AH_RTG,
                        DeviceSetting.SOC_MAX,
                        DeviceSetting.SOC_MIN,
                        DeviceSetting.SOC_RSV_MAX,
                        DeviceSetting.SOC_RSV_MIN,
                        DeviceSetting.W_CHA_MAX,
                        DeviceSetting.W_DISCHA_MAX,
                        DeviceSetting.WH_RTG);

        List<String> settingLabels =
                List.of(
                        "Maximum Charge Current",
                        "Maximum Discharge Current",
                        "Charge Capacity",
                        "Maximum SOC",
                        "Minimum SOC",
                        "Maximum Reserve Capacity",
                        "Minimum Reserve Capacity",
                        "Maximum Charge Rate",
                        "Maximum Discharge Rate",
                        "Energy Capacity");
        List<Double> settingValues =
                List.of(10.0, 35.0, 62.5, 99.0, 1.0, 100.0, 1.0, 27642.0, 64554.0, 17550.0);
        List<Type> settingTypes =
                List.of(
                        Type.FLOAT,
                        Type.FLOAT,
                        Type.FLOAT,
                        Type.FLOAT,
                        Type.FLOAT,
                        Type.FLOAT,
                        Type.FLOAT,
                        Type.INTEGER,
                        Type.INTEGER,
                        Type.INTEGER);
        List<Unit> settingUnits =
                List.of(
                        Unit.AMPS,
                        Unit.AMPS,
                        Unit.PERCENT,
                        Unit.PERCENT,
                        Unit.PERCENT,
                        Unit.PERCENT,
                        Unit.PERCENT,
                        Unit.WATTS,
                        Unit.WATTS,
                        Unit.WATT_HOURS);
        List<String> settingDescriptions =
                List.of(
                        "Instantaneous maximum DC charge current.",
                        "Instantaneous maximum DC discharge current.",
                        "Nameplate charge capacity in amp-hours.",
                        "Manufacturer maximum state of charge, expressed as a percentage.",
                        "Manufacturer minimum state of charge, expressed as a percentage.",
                        "Setpoint for maximum reserve for storage as a percentage of the nominal"
                                + " maximum storage.",
                        "Setpoint for minimum reserve for storage as a percentage of the nominal"
                                + " maximum storage.",
                        "Maximum rate of energy transfer into the storage device in DC watts.",
                        "Maximum rate of energy transfer out of the storage device in DC watts.",
                        "Nameplate energy capacity in DC watt-hours.");

        Double settingMinValue = 0.0;
        Double settingMaxValue = 35.0;
        List<Double> settingAllowedValues = List.of(1.0, 2.0, 3.0);
        DeviceSettings.DeviceSettingsMetadata.MetadataConstraints metadataConstraints =
                DeviceSettings.DeviceSettingsMetadata.MetadataConstraints.builder()
                        .maxValue(settingMaxValue)
                        .minValue(settingMinValue)
                        .allowedValues(settingAllowedValues)
                        .build();

        List<DeviceSettings> mockBatteries = new ArrayList<>();

        List<DeviceSettings.DeviceSettingsMetadata> batteryMetadataList = new ArrayList<>();
        for (int i = 0; i < settingNames.size(); i++) {
            DeviceSettings.DeviceSettingsMetadata batteryMetadata =
                    DeviceSettings.DeviceSettingsMetadata.builder()
                            .name(settingNames.get(i))
                            .label(settingLabels.get(i))
                            .value(settingValues.get(i))
                            .constraints(metadataConstraints)
                            .type(String.valueOf(settingTypes.get(i)))
                            .unit(String.valueOf(settingUnits.get(i)))
                            .description(settingDescriptions.get(i))
                            .build();

            batteryMetadataList.add(batteryMetadata);
        }

        DeviceSettings battery =
                DeviceSettings.builder()
                        .deviceId(deviceId)
                        .deviceType(DeviceTypeOuterClass.DeviceType.BATTERY)
                        .settings(batteryMetadataList)
                        .build();

        mockBatteries.add(battery);

        com.generac.ces.systemgateway.model.device.DeviceSettingsResponse expected =
                new com.generac.ces.systemgateway.model.device.DeviceSettingsResponse(
                        hostRcpn, mockBatteries);

        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", hostRcpn);

        MockResponse mockResponseSystemService = new MockResponse();
        mockResponseSystemService.addHeader("Content-Type", "application/json");
        mockResponseSystemService.setStatus("HTTP/1.1 200");
        mockResponseSystemService.setBody(mockSystemResponse.toString());
        mockSystemServiceWebClient.enqueue(mockResponseSystemService);

        MockResponse mockResponseEssDp = new MockResponse();
        mockResponseEssDp.addHeader("Content-Type", "application/json");
        mockResponseEssDp.setStatus("HTTP/1.1 200");
        mockResponseEssDp.setBody(JsonResponseHelper.DEVICE_SETTING_RESPONSE_BATTERIES);
        mockEssDataProviderWebClient.enqueue(mockResponseEssDp);

        when(systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        any(), any(), any()))
                .thenReturn(expected);

        // Action
        com.generac.ces.systemgateway.model.device.DeviceSettingsResponse actual =
                essSystemService.getBatterySettings(systemId).block();

        // Assert
        assertEquals(expected, actual);
    }

    // =======================================================================================================
    //   GET BATTERY PROPERTIES
    // =======================================================================================================
    @Test
    public void testGetBatteryProperties_providedRcpnDoesNotBelongToProvidedSystem() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceRcpn = "000100080A99";
        String expectedResponse =
                String.format(
                        "Provided battery rcpn: %s not found within system: %s",
                        deviceRcpn, systemId);
        mockSystemServiceWebClient.enqueue(new MockResponse().setResponseCode(200));

        // Action & Assert
        try {
            essSystemService.getBatteryProperties(systemId, deviceRcpn).block();
            fail("Expected to throw '404: " + expectedResponse + "'");
        } catch (Exception e) {
            assertEquals(expectedResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void testGetBatteryProperties_happyCaseReturnsCorrectResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceRcpn = "000100080A99";
        OffsetDateTime timestampUtc = OffsetDateTime.parse("2023-07-24T20:09:16Z");

        EssSystemService essSystemServiceSpy = spy(essSystemService);
        doReturn(Flux.just(deviceRcpn))
                .when(essSystemServiceSpy)
                .retrieveBatteryRCPNs(String.valueOf(systemId));

        MockResponse mockResponseEssDp = new MockResponse();
        mockResponseEssDp.addHeader("Content-Type", "application/json");
        mockResponseEssDp.setStatus("HTTP/1.1 200");
        mockResponseEssDp.setBody(JsonResponseHelper.BATTERY_PROPERTIES_RESPONSE);
        mockEssDataProviderWebClient.enqueue(mockResponseEssDp);

        BatteryPropertiesDto expected =
                BatteryPropertiesDto.builder()
                        .deviceId(deviceRcpn)
                        .timestampUtc(timestampUtc)
                        .type(4)
                        .ahRtg(62.5)
                        .whRtg(17550.0)
                        .socMax(100.0)
                        .socMin(1.0)
                        .socRsvMax(100.0)
                        .socRsvMin(1.0)
                        .w(-5.0)
                        .i(-0.01)
                        .v(312.2)
                        .soc(95.0)
                        .soh(94.0)
                        .maxCellV(4.0)
                        .minCellV(3.984)
                        .wChaMax(64989.0)
                        .wDischaMax(64554.0)
                        .aChaMax(30.0)
                        .aDischaMax(35.0)
                        .build();

        // Action
        BatteryPropertiesDto actual =
                essSystemServiceSpy.getBatteryProperties(systemId, deviceRcpn).block();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSystemBySystemId_TimeoutDueToNoResponse() {
        // Arrange
        // simulate request timeout
        mockSystemServiceWebClient.enqueue(
                new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        // Action and Assert
        assertThrows(
                UncheckedExecutionException.class,
                () -> {
                    essSystemService.getSystemBySystemId(UUID.randomUUID());
                });
    }

    @Test
    public void testGetSystemBySystemId_TimeoutDueToDelayedResponse() {
        // Arrange
        // simulate delayed response
        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", "sample_rcp_id");
        mockSystemResponse.addProperty("systemId", UUID.randomUUID().toString());
        mockSystemResponse.addProperty("siteId", UUID.randomUUID().toString());

        mockSystemServiceWebClient.enqueue(
                new MockResponse()
                        .setBodyDelay(5, TimeUnit.SECONDS)
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(200)
                        .setBody(mockSystemResponse.toString()));

        // Action and Assert
        assertThrows(
                UncheckedExecutionException.class,
                () -> {
                    essSystemService.getSystemBySystemId(UUID.randomUUID());
                });
    }

    @Test
    public void testSystemBySystemId_CacheHit() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String siteId = UUID.randomUUID().toString();
        SystemResponse systemResponse =
                new SystemResponse(
                        systemId,
                        siteId,
                        "sample_rcp_id",
                        "sample_beacon_rcpn",
                        "sample_host_device_id");
        WebClient systemServiceWebClient = Mockito.mock(WebClient.class);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock =
                Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock =
                Mockito.mock(WebClient.RequestHeadersSpec.class);
        CacheStore<SystemResponse> cache =
                new CacheStore<SystemResponse>(10000, 5, TimeUnit.SECONDS);

        EssSystemService essSystemService =
                new EssSystemService(
                        systemServiceWebClient,
                        null,
                        null,
                        cache,
                        deviceSettingsMapper,
                        systemSettingCacheService);

        when(systemServiceWebClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(any(Function.class))).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchangeToMono(any(Function.class)))
                .thenReturn(Mono.just(systemResponse));

        essSystemService.getSystemBySystemId(systemId); // call sys-ms once to populate cache

        // Action
        essSystemService.getSystemBySystemId(systemId);
        // Assert (hit)
        // there is cache entry already so sys-ms is not called this time
        verify(systemServiceWebClient, times(1)).get();
    }

    @Test
    public void testSystemBySystemId_CacheMiss() throws InterruptedException {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String siteId = UUID.randomUUID().toString();
        SystemResponse systemResponse =
                new SystemResponse(
                        systemId,
                        siteId,
                        "sample_rcp_id",
                        "sample_beacon_rcpn",
                        "sample_host_device_id");
        WebClient systemServiceWebClient = Mockito.mock(WebClient.class);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock =
                Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock =
                Mockito.mock(WebClient.RequestHeadersSpec.class);
        CacheStore<SystemResponse> cache =
                new CacheStore<SystemResponse>(10000, 5, TimeUnit.SECONDS);

        EssSystemService essSystemService =
                new EssSystemService(
                        systemServiceWebClient,
                        null,
                        null,
                        cache,
                        deviceSettingsMapper,
                        systemSettingCacheService);

        when(systemServiceWebClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(any(Function.class))).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchangeToMono(any(Function.class)))
                .thenReturn(Mono.just(systemResponse));

        essSystemService.getSystemBySystemId(systemId); // call sys-ms once to populate cache

        // Action
        Thread.sleep(5000); // simulate expiry of ttl
        essSystemService.getSystemBySystemId(systemId);
        // Assert (miss)
        // cache entry expired so sys-ms is called again
        verify(systemServiceWebClient, times(2)).get();
    }

    // =======================================================================================================
    //   GET/FIND DEVICE TYPE
    // =======================================================================================================
    @Test
    public void testGetDeviceType_notFound() {
        // Arrange
        String deviceId = "000100080A99";
        String systemId = "1e793d7a-7c54-49d9-bc8e-dd7cdd397450";
        mockSystemServiceWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getDeviceType(deviceId, systemId);
        } catch (Exception e) {
            Assertions.assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void testFindDeviceType_batteryMatch() {
        // Arrange
        String deviceId = "000100080A99";
        DeviceMapResponseDto.DeviceMapResponseDtoBattery battery =
                new DeviceMapResponseDto.DeviceMapResponseDtoBattery();
        battery.setId(deviceId);
        DeviceMapResponseDto deviceMapResponseDto = new DeviceMapResponseDto();
        deviceMapResponseDto.setBatteries(List.of(battery));

        DeviceTypeOuterClass.DeviceType expected = DeviceTypeOuterClass.DeviceType.BATTERY;

        // Action
        DeviceTypeOuterClass.DeviceType actual =
                essSystemService.findDeviceType(deviceMapResponseDto, deviceId);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testFindDeviceType_inverterMatch() {
        // Arrange
        String deviceId = "000100080A99";
        DeviceMapResponseDto.DeviceMapResponseDtoInverter inverter =
                new DeviceMapResponseDto.DeviceMapResponseDtoInverter();
        inverter.setId(deviceId);
        DeviceMapResponseDto deviceMapResponseDto = new DeviceMapResponseDto();
        deviceMapResponseDto.setInverters(List.of(inverter));

        DeviceTypeOuterClass.DeviceType expected = DeviceTypeOuterClass.DeviceType.INVERTER;

        // Action
        DeviceTypeOuterClass.DeviceType actual =
                essSystemService.findDeviceType(deviceMapResponseDto, deviceId);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testFindDeviceType_noMatchThrowsException() {
        // Arrange
        String deviceId = "000100080A99";
        String exceptionMessage = "Unable to find device type for provided deviceId: " + deviceId;
        DeviceMapResponseDto deviceMapResponseDto = new DeviceMapResponseDto();

        // Action & Assert
        try {
            essSystemService.findDeviceType(deviceMapResponseDto, deviceId);
            fail("Expected to throw 404");
        } catch (Exception e) {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(exceptionMessage, e.getMessage());
        }
    }

    // =======================================================================================================
    //   GET INVERTER SETTINGS
    // =======================================================================================================

    @Test
    public void getInverterSettings_systemIdNotFoundInSystemServiceCallReturnsError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String expectErrorResponse = String.format("ESS system id = %s not found.", systemId);
        mockSystemServiceWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getInverterSettings(systemId).block();
            fail("Expected to throw '404': " + expectErrorResponse + "'");
        } catch (Exception e) {
            assertEquals(expectErrorResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void getInverterSettings_systemIdNotFoundInEssDpCallReturnsError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceId = "000100080A99";
        String expectedResponse = String.format("ESS system id = %s not found.", systemId);

        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", deviceId);

        MockResponse mockResponseSystemService = new MockResponse();
        mockResponseSystemService.addHeader("Content-Type", "application/json");
        mockResponseSystemService.setStatus("HTTP/1.1 200");
        mockResponseSystemService.setBody(mockSystemResponse.toString());
        mockSystemServiceWebClient.enqueue(mockResponseSystemService);

        mockEssDataProviderWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getInverterSettings(systemId).block();
            fail("Expected to throw '404: " + expectedResponse + "'");
        } catch (Exception e) {
            assertEquals(expectedResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void getInverterSettings_mapsCorrectResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceId = "000100080A99";
        List<DeviceSetting> settingNames =
                List.of(
                        DeviceSetting.STATE,
                        DeviceSetting.ISLANDING,
                        DeviceSetting.EXPORT_OVERRIDE,
                        DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES,
                        DeviceSetting.LOAD_SHEDDING,
                        DeviceSetting.GENERATOR_CONTROL_MODE,
                        DeviceSetting.SELF_SUPPLY_SOURCE_POWER_LIMIT,
                        DeviceSetting.SELF_SUPPLY_SINK_POWER_LIMIT,
                        DeviceSetting.CT_TURNS_RATIO,
                        DeviceSetting.GRID_PARALLEL_INVERTERS,
                        DeviceSetting.GENERATOR_POWER_RATING,
                        DeviceSetting.EXPORT_POWER_LIMIT,
                        DeviceSetting.ZERO_IMPORT);
        List<String> settingLabels =
                List.of(
                        "Enable/Disable Inverter",
                        "Enable Islanding",
                        "Export Override",
                        "Number of Transfer Switches",
                        "Enable Load Shedding",
                        "AC Generator control mode",
                        "Self Supply Source Power",
                        "Self Supply Sink Power",
                        "CT Turns Ratio",
                        "GridParInverters",
                        "GenPower kW",
                        "Export Limit",
                        "Zero Import");
        List<Double> settingValues =
                List.of(1.0, 0.0, 1.0, 5.0, 1.0, 0.0, 12.0, 50.0, 45.0, 2.0, 100.0, 100.0, 0.0);
        List<Type> settingTypes =
                List.of(
                        Type.BOOLEAN,
                        Type.BOOLEAN,
                        Type.BOOLEAN,
                        Type.INTEGER,
                        Type.ENUM,
                        Type.ENUM,
                        Type.INTEGER,
                        Type.INTEGER,
                        Type.INTEGER,
                        Type.INTEGER,
                        Type.FLOAT,
                        Type.INTEGER,
                        Type.BOOLEAN);
        List<Unit> settingUnits =
                List.of(
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS,
                        Unit.KILOWATTS);
        List<String> settingDescriptions =
                List.of(
                        "This setting allows users to remotely disable and re-enable inverter",
                        "Allows system to island, providing backup power during a grid outage",
                        "Inhibits PWRcell system from exporting power to the grid",
                        "Set to zero if no ATS, set to 1 if a single external ATS is installed and"
                            + " set to 2 if two ATSs are installed to operate with the inverter",
                        "Select 1 if using SMM/PWRManager devices to shed loads, 2 if using PWRCell"
                                + " SACM to shed loads (with or without SMM)",
                        "An AC Generator integrated into an ESS PWRCell can operate under one of 3"
                            + " control modes; 0 - 'Single Transfer', 1 - 'Source Cycling', or 2 -"
                            + " 'Always on'",
                        "Max threshold for importing power before battery discharges in self supply"
                                + " mode",
                        "Min power import maintained by charging the battery from the grid in self"
                                + " supply mode",
                        "Allows a different turns ratio to be set for the specific CT",
                        "This setting allows for two inverters to share one set of CTs. Set to 2 if"
                                + " daisy chaining CTs between two inverters",
                        "test description",
                        "Set the kW limit exported to grid",
                        "Enable / disable zero import");

        Map<Object, Object> constraintsMap_1a = new HashMap<>();
        constraintsMap_1a.put("name", "LOAD_SHED_SMM_ONLY");
        constraintsMap_1a.put("value", 1.0);
        Map<Object, Object> constraintsMap_1b = new HashMap<>();
        constraintsMap_1b.put("name", "LOAD_SHED_DISABLED");
        constraintsMap_1b.put("value", 0.0);
        Map<Object, Object> constraintsMap_1c = new HashMap<>();
        constraintsMap_1c.put("name", "LOAD_SHED_ATS_AND_SMM");
        constraintsMap_1c.put("value", 2.0);
        List<?> allowedValuesLoadShed =
                Arrays.asList(constraintsMap_1a, constraintsMap_1b, constraintsMap_1c);

        Map<Object, Object> constraintsMap_2a = new HashMap<>();
        constraintsMap_2a.put("name", "ALWAYS_ON");
        constraintsMap_2a.put("value", 2.0);
        Map<Object, Object> constraintsMap_2b = new HashMap<>();
        constraintsMap_2b.put("name", "SOURCE_CYCLING");
        constraintsMap_2b.put("value", 1.0);
        Map<Object, Object> constraintsMap_2c = new HashMap<>();
        constraintsMap_2c.put("name", "SINGLE_TRANSFER");
        constraintsMap_2c.put("value", 0.0);
        List<?> allowedValuesGenControlMode =
                Arrays.asList(constraintsMap_2a, constraintsMap_2b, constraintsMap_2c);

        List<Double> allowedValue = List.of(1.0, 2.0, 3.0);
        List<List<?>> allowedValues =
                List.of(
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValuesLoadShed,
                        allowedValuesGenControlMode,
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValue);
        List<Double> minValues =
                List.of(0.0, 0.0, 0.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        List<Double> maxValues =
                List.of(
                        35.0, 35.0, 35.0, 35.0, -1.0, -1.0, 35.0, 35.0, 35.0, 35.0, 35.0, 35.0,
                        35.0);

        List<DeviceSettings> mockInverters = new ArrayList<>();

        List<DeviceSettings.DeviceSettingsMetadata> inverterMetadataList = new ArrayList<>();
        for (int i = 0; i < settingNames.size(); i++) {
            DeviceSettings.DeviceSettingsMetadata.MetadataConstraints metadataConstraints =
                    DeviceSettings.DeviceSettingsMetadata.MetadataConstraints.builder()
                            .minValue(
                                    (settingNames.get(i).equals(DeviceSetting.LOAD_SHEDDING)
                                                    || settingNames
                                                            .get(i)
                                                            .equals(
                                                                    DeviceSetting
                                                                            .GENERATOR_CONTROL_MODE))
                                            ? null
                                            : minValues.get(i))
                            .maxValue(
                                    (settingNames.get(i).equals(DeviceSetting.LOAD_SHEDDING)
                                                    || settingNames
                                                            .get(i)
                                                            .equals(
                                                                    DeviceSetting
                                                                            .GENERATOR_CONTROL_MODE))
                                            ? null
                                            : maxValues.get(i))
                            .allowedValues(allowedValues.get(i))
                            .build();

            DeviceSettings.DeviceSettingsMetadata inverterMetadata =
                    DeviceSettings.DeviceSettingsMetadata.builder()
                            .name(settingNames.get(i))
                            .label(settingLabels.get(i))
                            .value(settingValues.get(i))
                            .constraints(metadataConstraints)
                            .type(String.valueOf(settingTypes.get(i)))
                            .unit(String.valueOf(settingUnits.get(i)))
                            .description(settingDescriptions.get(i))
                            .build();
            inverterMetadataList.add(inverterMetadata);
        }

        DeviceSettings inverter =
                new DeviceSettings(
                        deviceId, DeviceTypeOuterClass.DeviceType.INVERTER, inverterMetadataList);
        mockInverters.add(inverter);

        DeviceSettingsResponse expected = new DeviceSettingsResponse(deviceId, mockInverters);

        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", deviceId);

        MockResponse mockResponseSystemService = new MockResponse();
        mockResponseSystemService.addHeader("Content-Type", "application/json");
        mockResponseSystemService.setStatus("HTTP/1.1 200");
        mockResponseSystemService.setBody(mockSystemResponse.toString());
        mockSystemServiceWebClient.enqueue(mockResponseSystemService);

        MockResponse mockResponseEssDp = new MockResponse();
        mockResponseEssDp.addHeader("Content-Type", "application/json");
        mockResponseEssDp.setStatus("HTTP/1.1 200");
        mockResponseEssDp.setBody(JsonResponseHelper.DEVICE_SETTING_RESPONSE_INVERTERS);
        mockEssDataProviderWebClient.enqueue(mockResponseEssDp);

        when(systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        any(), any(), any()))
                .thenReturn(expected);

        // Action
        DeviceSettingsResponse actual = essSystemService.getInverterSettings(systemId).block();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void getPvlSettings_systemIdNotFoundInSystemServiceCallReturnsError() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String expectErrorResponse = String.format("ESS system id = %s not found.", systemId);
        mockSystemServiceWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getPvlSettings(systemId).block();
            fail("Expected to throw '404': " + expectErrorResponse + "'");
        } catch (Exception e) {
            assertEquals(expectErrorResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void getPvlSettings_systemIdNotFoundInEssDpCallReturnsError() {
        UUID systemId = UUID.randomUUID();
        String deviceId = "000100080A99";
        String expectedResponse = String.format("ESS system id = %s not found.", systemId);

        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", deviceId);

        MockResponse mockResponseSystemService = new MockResponse();
        mockResponseSystemService.addHeader("Content-Type", "application/json");
        mockResponseSystemService.setStatus("HTTP/1.1 200");
        mockResponseSystemService.setBody(mockSystemResponse.toString());
        mockSystemServiceWebClient.enqueue(mockResponseSystemService);

        mockEssDataProviderWebClient.enqueue(new MockResponse().setResponseCode(404));

        // Action & Assert
        try {
            essSystemService.getPvlSettings(systemId).block();
            fail("Expected to throw '404: " + expectedResponse + "'");
        } catch (Exception e) {
            assertEquals(expectedResponse, e.getMessage());
            assertEquals(ResourceNotFoundException.class, e.getClass());
        }
    }

    @Test
    public void getPvlSettings_mapsCorrectResponse() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        String deviceId = "000100080A99";
        List<DeviceSetting> settingNames =
                List.of(
                        DeviceSetting.PVLINK_STATE,
                        DeviceSetting.ENABLE_PVRSS,
                        DeviceSetting.OVERRIDE_PVRSS,
                        DeviceSetting.NUM_STRING,
                        DeviceSetting.VIN_STARTUP,
                        DeviceSetting.PLM_CHANNEL,
                        DeviceSetting.SNAP_RS_INSTALLED_CNT,
                        DeviceSetting.SNAP_RS_DETECTED_CNT);
        List<String> settingLabels =
                List.of(
                        "Enable/Disable PVLS",
                        "Enable PVRSS",
                        "Override PVRSS",
                        "String Count",
                        "Vin Startup",
                        "PLM Channel",
                        "SnapRSInstalled",
                        "SnapRSDetected");
        List<Double> settingValues = List.of(0.0, 0.0, 1.0, 1.0, 1110.0, 8.0, 0.0, 0.0);
        List<Type> settingTypes =
                List.of(
                        Type.BOOLEAN,
                        Type.BOOLEAN,
                        Type.ENUM,
                        Type.ENUM,
                        Type.INTEGER,
                        Type.ENUM,
                        Type.INTEGER,
                        Type.INTEGER);
        List<String> settingDescriptions =
                List.of(
                        "",
                        "If SnapRS devices are installed, this must be \"on\".",
                        "Closes SnapRS device without requiring a count. Should not be "
                                + "left ‘on’, only for troubleshooting purposes",
                        "Number of parallel PV substrings connected to this PV Link.",
                        "Minimum input voltage from PV sub-string for the PV Link to make power",
                        "Channel for REbus communications. All devices in a system must use the"
                                + " same channel (except REbus Beacon).",
                        "The total number of SnapRS devices physically installed on this PV Link.",
                        "The number of SnapRS devices detected by the PV Link after its daily"
                                + " count.");

        Map<Object, Object> constraintsMap_1a = new HashMap<>();
        constraintsMap_1a.put("name", "minValue");
        constraintsMap_1a.put("value", 60.0);
        Map<Object, Object> constraintsMap_1b = new HashMap<>();
        constraintsMap_1b.put("name", "maxValue");
        constraintsMap_1b.put("value", 420.0);
        List<?> allowedValuesVinStartup = Arrays.asList(constraintsMap_1a, constraintsMap_1b);

        Map<Object, Object> constraintsMap_2a = new HashMap<>();
        constraintsMap_2a.put("name", "minValue");
        constraintsMap_2a.put("value", 0.0);
        Map<Object, Object> constraintsMap_2b = new HashMap<>();
        constraintsMap_2b.put("name", "maxValue");
        constraintsMap_2b.put("value", 12.0);
        List<?> allowedValuesSnapRSInstalled = Arrays.asList(constraintsMap_2a, constraintsMap_2b);

        Map<Object, Object> constraintsMap_3a = new HashMap<>();
        constraintsMap_3a.put("name", "minValue");
        constraintsMap_3a.put("value", 0.0);
        Map<Object, Object> constraintsMap_3b = new HashMap<>();
        constraintsMap_3b.put("name", "maxValue");
        constraintsMap_3b.put("value", 12.0);
        List<?> allowedValuesStringCnt = Arrays.asList(constraintsMap_3a, constraintsMap_3b);

        Map<Object, Object> constraintsMap_4a = new HashMap<>();
        constraintsMap_4a.put("name", "CH_0");
        constraintsMap_4a.put("value", 0.0);
        Map<Object, Object> constraintsMap_4b = new HashMap<>();
        constraintsMap_4b.put("name", "CH_1");
        constraintsMap_4b.put("value", 1.0);
        Map<Object, Object> constraintsMap_4c = new HashMap<>();
        constraintsMap_4c.put("name", "CH_2");
        constraintsMap_4c.put("value", 2.0);
        Map<Object, Object> constraintsMap_4d = new HashMap<>();
        constraintsMap_4d.put("name", "CH_3");
        constraintsMap_4d.put("value", 3.0);
        Map<Object, Object> constraintsMap_4e = new HashMap<>();
        constraintsMap_4e.put("name", "CH_4");
        constraintsMap_4e.put("value", 4.0);
        Map<Object, Object> constraintsMap_4f = new HashMap<>();
        constraintsMap_4f.put("name", "CH_5");
        constraintsMap_4f.put("value", 5.0);
        Map<Object, Object> constraintsMap_4g = new HashMap<>();
        constraintsMap_4g.put("name", "CH_6");
        constraintsMap_4g.put("value", 6.0);
        Map<Object, Object> constraintsMap_4h = new HashMap<>();
        constraintsMap_4h.put("name", "CH_7");
        constraintsMap_4h.put("value", 7.0);
        Map<Object, Object> constraintsMap_4j = new HashMap<>();
        constraintsMap_4j.put("name", "CH_8");
        constraintsMap_4j.put("value", 8.0);
        Map<Object, Object> constraintsMap_4k = new HashMap<>();
        constraintsMap_4k.put("name", "CH_9");
        constraintsMap_4k.put("value", 8.0);
        Map<Object, Object> constraintsMap_4l = new HashMap<>();
        constraintsMap_4l.put("name", "CH_10");
        constraintsMap_4l.put("value", 10.0);
        Map<Object, Object> constraintsMap_4m = new HashMap<>();
        constraintsMap_4m.put("name", "CH_11");
        constraintsMap_4m.put("value", 11.0);
        Map<Object, Object> constraintsMap_4n = new HashMap<>();
        constraintsMap_4n.put("name", "CH_12");
        constraintsMap_4n.put("value", 12.0);
        List<?> allowedValuesPlmChannel =
                Arrays.asList(
                        constraintsMap_4a,
                        constraintsMap_4b,
                        constraintsMap_4c,
                        constraintsMap_4d,
                        constraintsMap_4e,
                        constraintsMap_4f,
                        constraintsMap_4g,
                        constraintsMap_4h,
                        constraintsMap_4j,
                        constraintsMap_4k,
                        constraintsMap_4l,
                        constraintsMap_4m,
                        constraintsMap_4n);

        List<Double> allowedValue = List.of();
        List<List<?>> allowedValues =
                List.of(
                        allowedValue,
                        allowedValue,
                        allowedValue,
                        allowedValuesStringCnt,
                        allowedValuesVinStartup,
                        allowedValuesPlmChannel,
                        allowedValuesSnapRSInstalled,
                        allowedValue);

        List<DeviceSettings> mockPvls = new ArrayList<>();

        List<DeviceSettings.DeviceSettingsMetadata> pvlMetadataList = new ArrayList<>();
        for (int i = 0; i < settingNames.size(); i++) {
            DeviceSettings.DeviceSettingsMetadata.MetadataConstraints metadataConstraints =
                    DeviceSettings.DeviceSettingsMetadata.MetadataConstraints.builder()
                            .allowedValues(allowedValues.get(i))
                            .build();

            DeviceSettings.DeviceSettingsMetadata pvlMetadata =
                    DeviceSettings.DeviceSettingsMetadata.builder()
                            .name(settingNames.get(i))
                            .label(settingLabels.get(i))
                            .value(settingValues.get(i))
                            .constraints(metadataConstraints)
                            .type(String.valueOf(settingTypes.get(i)))
                            .unit(
                                    settingNames.get(i).equals(DeviceSetting.VIN_STARTUP)
                                            ? "VOLTS"
                                            : null)
                            .description(settingDescriptions.get(i))
                            .build();
            pvlMetadataList.add(pvlMetadata);
        }

        DeviceSettings inverter = new DeviceSettings(deviceId, DeviceType.PVLINK, pvlMetadataList);
        mockPvls.add(inverter);

        DeviceSettingsResponse expected = new DeviceSettingsResponse(deviceId, mockPvls);

        JsonObject mockSystemResponse = new JsonObject();
        mockSystemResponse.addProperty("rcpId", deviceId);

        MockResponse mockResponseSystemService = new MockResponse();
        mockResponseSystemService.addHeader("Content-Type", "application/json");
        mockResponseSystemService.setStatus("HTTP/1.1 200");
        mockResponseSystemService.setBody(mockSystemResponse.toString());
        mockSystemServiceWebClient.enqueue(mockResponseSystemService);

        MockResponse mockResponseEssDp = new MockResponse();
        mockResponseEssDp.addHeader("Content-Type", "application/json");
        mockResponseEssDp.setStatus("HTTP/1.1 200");
        mockResponseEssDp.setBody(JsonResponseHelper.DEVICE_SETTING_RESPONSE_PVLS);
        mockEssDataProviderWebClient.enqueue(mockResponseEssDp);

        when(systemSettingCacheService.updateDeviceSettingsResponseWithCachedSettings(
                        any(), any(), any()))
                .thenReturn(expected);

        // Action
        DeviceSettingsResponse actual = essSystemService.getPvlSettings(systemId).block();

        // Assert
        assertEquals(expected, actual);
    }
}
