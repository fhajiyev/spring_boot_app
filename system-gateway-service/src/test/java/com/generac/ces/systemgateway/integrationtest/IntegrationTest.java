package com.generac.ces.systemgateway.integrationtest;

import static com.generac.ces.systemgateway.integrationtest.IntegrationTestBase.ContainerInitializer.createStream;
import static com.generac.ces.systemgateway.integrationtest.IntegrationTestBase.ContainerInitializer.deleteStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.SubscriptionProto;
import com.generac.ces.SystemShadowCommandsProto;
import com.generac.ces.essdataprovider.model.DeviceMapRequestDto;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.system.SystemFamilyOuterClass;
import com.generac.ces.system.SystemTypeOuterClass;
import com.generac.ces.systemgateway.entity.subscription.SubscriptionAudit;
import com.generac.ces.systemgateway.entity.subscription.SubscriptionMetric;
import com.generac.ces.systemgateway.helper.InputStreamHelper;
import com.generac.ces.systemgateway.model.*;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.device.DeviceListRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest extends IntegrationTestBase {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired Seeder seeder;

    @BeforeEach
    public void beforeEach() throws Exception {
        seeder.populateClickhouseFromFile(
                InputStreamHelper.readFromInputStream(
                        getClass().getResourceAsStream("devicemap/createTestData.sql")));
        createStream();
    }

    @AfterEach
    public void afterEach() throws Exception {
        seeder.cleanDB();
        deleteStream();
    }

    @Test
    void createSubscriptionEssTest() throws Exception {
        // Arrange
        JsonObject json = new JsonObject();
        json.addProperty("subscriberType", "PWRVIEW");
        json.addProperty("resourceType", "ENERGY_RECORDSET_1HZ");
        json.addProperty("durationSeconds", 5);
        JsonArray arr = new JsonArray();
        JsonObject systemJson = new JsonObject();
        systemJson.addProperty("systemType", "ESS");
        String systemId = UUID.randomUUID().toString();
        systemJson.addProperty("systemId", systemId);
        arr.add(systemJson);
        json.add("systems", arr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(
                CallerMetadataRequestHeaders.COGNITO_CLIENT_ID.getHeaderName(), "sample_client_id");
        String body = json.toString();
        RequestEntity<String> requestEntity =
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/v1/subscriptions"));

        enqueueEssResponsesIntoMockServer();

        // Action & Assert
        ResponseEntity<List<SubscriptionResponse>> responseEntity =
                restTemplate.exchange(
                        "/v1/subscriptions",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<SubscriptionResponse>>() {});

        Thread.sleep(5000);

        // audit assertions
        List<SubscriptionAudit> allAuditRecords = seeder.getAllAuditRecords();
        assertEquals(1, allAuditRecords.size());
        Assert.assertEquals(SubscriberType.PWRVIEW, allAuditRecords.get(0).getSubscriberType());
        Assert.assertEquals(
                ResourceType.ENERGY_RECORDSET_1HZ, allAuditRecords.get(0).getResourceType());
        assertEquals(5, (int) allAuditRecords.get(0).getDurationSec());
        Assert.assertEquals(SystemType.ESS, allAuditRecords.get(0).getSystemType());
        assertEquals("000100073534", allAuditRecords.get(0).getHostId());
        assertEquals("sample_client_id", allAuditRecords.get(0).getClientId());

        // metric assertions
        List<SubscriptionMetric> allMetricRecords = seeder.getAllMetricRecords();
        assertEquals(1, allMetricRecords.size());
        Assert.assertEquals(SubscriberType.PWRVIEW, allMetricRecords.get(0).getSubscriberType());
        Assert.assertEquals(
                ResourceType.ENERGY_RECORDSET_1HZ, allMetricRecords.get(0).getResourceType());
        assertEquals(5, (int) allMetricRecords.get(0).getTotalDailyDurationSec());
        Assert.assertEquals(SystemType.ESS, allMetricRecords.get(0).getSystemType());

        Thread.sleep(5000);
        List<ByteBuffer> records =
                IntegrationTestBase.ContainerInitializer.readFromStream("test-sink-stream");
        assertEquals(1, records.size());

        for (ByteBuffer bb : records) {
            SystemShadowCommandsProto.SystemShadowCommandRequest sysShadowCommand =
                    SystemShadowCommandsProto.SystemShadowCommandRequest.parseFrom(bb);
            SubscriptionProto.Subscription subscription = sysShadowCommand.getSubscription();
            assertEquals(
                    subscription.getSubscriptionId(), allAuditRecords.get(0).getSubscriptionId());
            assertEquals(SubscriptionProto.Subscriber.PWRVIEW, subscription.getSubscriber());
            assertEquals(
                    SubscriptionProto.ResourceType.ENERGY_RECORDSET_1HZ,
                    subscription.getResourceType());
            assertEquals(5, subscription.getDurationSeconds());
            assertEquals(SystemTypeOuterClass.SystemType.ESS, subscription.getSystemType());
            assertEquals("000100073534", subscription.getHostId());
            assertEquals(systemId, subscription.getSystemId());
            assertEquals("sample_site_id", subscription.getSiteId());
            assertEquals("000100120934", subscription.getSensorId()); // beacon
        }
    }

    @Test
    void createSubscriptionEs2Test() throws Exception {
        // Arrange
        JsonObject json = new JsonObject();
        json.addProperty("subscriberType", "PWRVIEW");
        json.addProperty("resourceType", "ENERGY_RECORDSET_1HZ");
        json.addProperty("durationSeconds", 5);
        JsonArray arr = new JsonArray();
        JsonObject systemJson = new JsonObject();
        systemJson.addProperty("systemType", "ES2");
        String systemId = UUID.randomUUID().toString();
        systemJson.addProperty("systemId", systemId);
        arr.add(systemJson);
        json.add("systems", arr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(
                CallerMetadataRequestHeaders.COGNITO_CLIENT_ID.getHeaderName(), "sample_client_id");
        String body = json.toString();
        RequestEntity<String> requestEntity =
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/v1/subscriptions"));

        enqueueEs2ResponsesIntoMockServer();

        // Action & Assert
        ResponseEntity<List<SubscriptionResponse>> responseEntity =
                restTemplate.exchange(
                        "/v1/subscriptions",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<SubscriptionResponse>>() {});

        Thread.sleep(5000);

        // audit assertions
        List<SubscriptionAudit> allAuditRecords = seeder.getAllAuditRecords();
        assertEquals(1, allAuditRecords.size());
        Assert.assertEquals(SubscriberType.PWRVIEW, allAuditRecords.get(0).getSubscriberType());
        Assert.assertEquals(
                ResourceType.ENERGY_RECORDSET_1HZ, allAuditRecords.get(0).getResourceType());
        assertEquals(5, (int) allAuditRecords.get(0).getDurationSec());
        Assert.assertEquals(SystemType.ES2, allAuditRecords.get(0).getSystemType());
        assertEquals("000100073534", allAuditRecords.get(0).getHostId());
        assertEquals("sample_client_id", allAuditRecords.get(0).getClientId());

        // metric assertions
        List<SubscriptionMetric> allMetricRecords = seeder.getAllMetricRecords();
        assertEquals(1, allMetricRecords.size());
        Assert.assertEquals(SubscriberType.PWRVIEW, allMetricRecords.get(0).getSubscriberType());
        Assert.assertEquals(
                ResourceType.ENERGY_RECORDSET_1HZ, allMetricRecords.get(0).getResourceType());
        assertEquals(5, (int) allMetricRecords.get(0).getTotalDailyDurationSec());
        Assert.assertEquals(SystemType.ES2, allMetricRecords.get(0).getSystemType());

        Thread.sleep(5000);
        List<ByteBuffer> records =
                IntegrationTestBase.ContainerInitializer.readFromStream("test-sink-stream");
        assertEquals(1, records.size());

        for (ByteBuffer bb : records) {
            SystemShadowCommandsProto.SystemShadowCommandRequest sysShadowCommand =
                    SystemShadowCommandsProto.SystemShadowCommandRequest.parseFrom(bb);
            SubscriptionProto.Subscription subscription = sysShadowCommand.getSubscription();
            assertEquals(
                    subscription.getSubscriptionId(), allAuditRecords.get(0).getSubscriptionId());
            assertEquals(SubscriptionProto.Subscriber.PWRVIEW, subscription.getSubscriber());
            assertEquals(
                    SubscriptionProto.ResourceType.ENERGY_RECORDSET_1HZ,
                    subscription.getResourceType());
            assertEquals(5, subscription.getDurationSeconds());
            assertEquals(SystemTypeOuterClass.SystemType.ES2, subscription.getSystemType());
            assertEquals("000100073534", subscription.getHostId());
            assertEquals(systemId, subscription.getSystemId());
            assertEquals("sample_site_id", subscription.getSiteId());
            assertEquals("000100073534", subscription.getSensorId()); // gateway
        }
    }

    @Test
    void getSubscriptionQuotaTest() throws Exception {
        // Arrange
        int quota = 5184000; // pwrview daily quota
        int duration = 7;
        JsonObject json = new JsonObject();
        json.addProperty("subscriberType", "PWRVIEW");
        json.addProperty("resourceType", "ENERGY_RECORDSET_1HZ");
        json.addProperty("durationSeconds", duration);
        JsonArray arr = new JsonArray();
        JsonObject systemJson = new JsonObject();
        systemJson.addProperty("systemType", "ESS");
        systemJson.addProperty("systemId", "fae7d4a7-a74c-418b-a61a-433d92747358");
        arr.add(systemJson);
        json.add("systems", arr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(
                CallerMetadataRequestHeaders.COGNITO_CLIENT_ID.getHeaderName(), "sample_client_id");
        String body = json.toString();
        RequestEntity<String> requestEntity =
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/v1/subscriptions"));
        enqueueEssResponsesIntoMockServer();

        // Action & Assert

        // create subscription
        ResponseEntity<List<SubscriptionResponse>> resp1 =
                restTemplate.exchange(
                        "/v1/subscriptions",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<SubscriptionResponse>>() {});

        Thread.sleep(5000);

        // get remaining quota
        JsonObject sysMsResp = new JsonObject();
        sysMsResp.addProperty("systemId", "fae7d4a7-a74c-418b-a61a-433d92747358");
        String sampleSiteId = UUID.randomUUID().toString();
        sysMsResp.addProperty("siteId", sampleSiteId);
        sysMsResp.addProperty("rcpId", "000100073534");
        sysMsResp.addProperty("beaconRcpn", "000100120934");
        MockResponse mockResponseSysMs = new MockResponse();
        mockResponseSysMs.addHeader("Content-Type", "application/json");
        mockResponseSysMs.setStatus("HTTP/1.1 200");
        mockResponseSysMs.setBody(sysMsResp.toString());
        mockBackEnd.enqueue(mockResponseSysMs); // simulate system-ms response

        ResponseEntity<Quota> resp =
                restTemplate.exchange(
                        "/v1/quota?"
                                + "subscriberType=PWRVIEW&"
                                + "systemType=ESS&"
                                + "resourceType=ENERGY_RECORDSET_1HZ&"
                                + "systemId=fae7d4a7-a74c-418b-a61a-433d92747358",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Quota>() {});

        assertEquals(quota - duration, resp.getBody().getRemainingQuotaSeconds());
    }

    @Test
    void deviceMapEndpointTest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // setting up the request headers
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        // setting up the request body
        List<DeviceMapRequestDto.DeviceMapSystem> systems = new ArrayList<>();
        systems.add(new DeviceMapRequestDto.DeviceMapSystem());
        systems.get(0).setSystemId("systemId1");
        systems.get(0).setHosts(new ArrayList<>());
        systems.get(0).getHosts().add(new DeviceMapRequestDto.Host());
        systems.get(0).getHosts().get(0).setHostRcpn("0001000720D0");

        systems.add(new DeviceMapRequestDto.DeviceMapSystem());
        systems.get(1).setSystemId("systemId2");
        systems.get(1).setHosts(new ArrayList<>());
        systems.get(1).getHosts().add(new DeviceMapRequestDto.Host());
        systems.get(1).getHosts().get(0).setHostRcpn("000100071818");

        systems.add(new DeviceMapRequestDto.DeviceMapSystem());
        systems.get(2).setSystemId("systemId3");
        systems.get(2).setHosts(new ArrayList<>());
        systems.get(2).getHosts().add(new DeviceMapRequestDto.Host());
        systems.get(2).getHosts().get(0).setHostRcpn("asdf");

        DeviceMapRequestDto requestDto = new DeviceMapRequestDto(systems);

        // request entity is created with request body and headers
        HttpEntity<DeviceMapRequestDto> requestEntity =
                new HttpEntity<>(requestDto, requestHeaders);

        ResponseEntity<List<DeviceMapResponseDto>> responseEntity =
                restTemplate.exchange(
                        "/v1/devicemap/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<DeviceMapResponseDto>>() {});

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertNotNull(responseEntity.getBody());
        assertEquals(3, responseEntity.getBody().size());
        assertEquals(13, responseEntity.getBody().get(0).getPvls().size());
        assertEquals(4, responseEntity.getBody().get(0).getInverters().size());
        assertEquals(2, responseEntity.getBody().get(0).getBatteries().size());
        assertEquals(4, responseEntity.getBody().get(0).getBeacons().size());
        assertEquals(4, responseEntity.getBody().get(0).getIcms().size());
        assertEquals(1, responseEntity.getBody().get(0).getRgms().size());
        assertEquals(1, responseEntity.getBody().get(0).getGenerators().size());
        assertEquals(1, responseEntity.getBody().get(0).getLoadcontrollers().size());

        assertEquals(2, responseEntity.getBody().get(1).getPvls().size());
        assertEquals(1, responseEntity.getBody().get(1).getInverters().size());
        assertEquals(0, responseEntity.getBody().get(1).getBatteries().size());
        assertEquals(1, responseEntity.getBody().get(1).getBeacons().size());
        assertEquals(1, responseEntity.getBody().get(1).getIcms().size());
        assertEquals(0, responseEntity.getBody().get(1).getRgms().size());
        assertEquals(0, responseEntity.getBody().get(1).getGenerators().size());
        assertEquals(0, responseEntity.getBody().get(1).getLoadcontrollers().size());

        assertEquals(
                objectMapper.readTree(
                        getClass().getResourceAsStream("devicemap/testResultsDeviceMap.json")),
                objectMapper.readTree(objectMapper.writeValueAsString(responseEntity.getBody())));

        // Validation tests:

        requestDto = new DeviceMapRequestDto(systems);
        requestDto.setSystems(null);
        requestEntity = new HttpEntity<>(requestDto, requestHeaders);
        ResponseEntity<Object> errResp =
                restTemplate.exchange(
                        "/v1/devicemap/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<Object>() {});
        assertEquals(400, errResp.getStatusCodeValue());

        requestDto = new DeviceMapRequestDto(systems);
        requestDto.setSystems(new ArrayList<>());
        requestEntity = new HttpEntity<>(requestDto, requestHeaders);
        errResp =
                restTemplate.exchange(
                        "/v1/devicemap/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<Object>() {});
        assertEquals(400, errResp.getStatusCodeValue());

        systems = new ArrayList<>();
        systems.add(new DeviceMapRequestDto.DeviceMapSystem());
        systems.get(0).setSystemId("systemId1");
        systems.get(0).setHosts(new ArrayList<>());
        systems.get(0).getHosts().add(new DeviceMapRequestDto.Host());
        systems.get(0).getHosts().get(0).setHostRcpn("0001000720D0");
        requestDto = new DeviceMapRequestDto(systems);
        systems.get(0).setHosts(new ArrayList<>());
        requestEntity = new HttpEntity<>(requestDto, requestHeaders);
        errResp =
                restTemplate.exchange(
                        "/v1/devicemap/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<Object>() {});
        assertEquals(400, errResp.getStatusCodeValue());

        systems = new ArrayList<>();
        systems.add(new DeviceMapRequestDto.DeviceMapSystem());
        systems.get(0).setSystemId("systemId1");
        systems.get(0).setHosts(new ArrayList<>());
        systems.get(0).getHosts().add(new DeviceMapRequestDto.Host());
        systems.get(0).getHosts().get(0).setHostRcpn("0001000720D0");
        requestDto = new DeviceMapRequestDto(systems);
        systems.get(0).setHosts(null);
        requestEntity = new HttpEntity<>(requestDto, requestHeaders);
        errResp =
                restTemplate.exchange(
                        "/v1/devicemap/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<Object>() {});
        assertEquals(400, errResp.getStatusCodeValue());
    }

    @Test
    void deviceListEndpointTest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // setting up the request headers
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        // setting up the request body
        String systemId1 = "373725e6-37e6-4cd4-8445-9efb78a0aeb5";
        String systemId2 = "45df47dd-63c6-47ba-849b-a2ca92e36db9";
        String systemId3 = "5d1a0824-49cc-4cfb-980f-76b13583fe24";
        enqueueSystemResponseIntoMockServer(
                "0001000720D0", SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R1);
        enqueueSystemResponseIntoMockServer(
                "000100079132", SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R2);
        enqueueSystemResponseIntoMockServer(
                "000100071818", SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R1);

        List<DeviceListRequest.DeviceListSystem> systems = new ArrayList<>();
        systems.add(new DeviceListRequest.DeviceListSystem());
        systems.get(0).setSystemId(systemId1);

        systems.add(new DeviceListRequest.DeviceListSystem());
        systems.get(1).setSystemId(systemId2);

        systems.add(new DeviceListRequest.DeviceListSystem());
        systems.get(2).setSystemId(systemId3);

        DeviceListRequest requestDto = new DeviceListRequest(systems);

        // request entity is created with request body and headers
        HttpEntity<DeviceListRequest> requestEntity = new HttpEntity<>(requestDto, requestHeaders);

        ResponseEntity<List<DeviceList>> responseEntity =
                restTemplate.exchange(
                        "/v1/devicelist/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<List<DeviceList>>() {});

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertNotNull(responseEntity.getBody());
        assertEquals(3, responseEntity.getBody().size());

        assertEquals(
                objectMapper.readTree(
                        getClass().getResourceAsStream("devicemap/testResultsDeviceList.json")),
                objectMapper.readTree(objectMapper.writeValueAsString(responseEntity.getBody())));

        // Validation tests:

        requestDto = new DeviceListRequest(systems);
        requestDto.setSystems(null);
        requestEntity = new HttpEntity<>(requestDto, requestHeaders);
        ResponseEntity<Object> errResp =
                restTemplate.exchange(
                        "/v1/devicelist/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<Object>() {});
        assertEquals(400, errResp.getStatusCodeValue());

        requestDto = new DeviceListRequest(systems);
        requestDto.setSystems(new ArrayList<>());
        requestEntity = new HttpEntity<>(requestDto, requestHeaders);
        errResp =
                restTemplate.exchange(
                        "/v1/devicelist/",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<Object>() {});
        assertEquals(400, errResp.getStatusCodeValue());
    }

    private void enqueueEssResponsesIntoMockServer() {
        JsonObject sysMsResp = new JsonObject();
        sysMsResp.addProperty("systemId", "fae7d4a7-a74c-418b-a61a-433d92747358");
        sysMsResp.addProperty("siteId", "sample_site_id");
        sysMsResp.addProperty("rcpId", "000100073534");
        sysMsResp.addProperty("beaconRcpn", "000100120934");

        JsonObject odinMsResp = new JsonObject();
        odinMsResp.addProperty("id", UUID.randomUUID().toString());
        odinMsResp.addProperty("code", "SUCCESS");

        MockResponse mockResponseSysMs = new MockResponse();
        mockResponseSysMs.addHeader("Content-Type", "application/json");
        mockResponseSysMs.setStatus("HTTP/1.1 200");
        mockResponseSysMs.setBody(sysMsResp.toString());
        mockBackEnd.enqueue(mockResponseSysMs); // simulate system-ms response

        MockResponse mockResponseOdinMs = new MockResponse();
        mockResponseOdinMs.addHeader("Content-Type", "application/json");
        mockResponseOdinMs.setStatus("HTTP/1.1 202");
        mockResponseOdinMs.setBody(odinMsResp.toString());
        mockBackEnd.enqueue(mockResponseOdinMs); // simulate odin-ms response
    }

    private void enqueueEs2ResponsesIntoMockServer() {
        JsonObject sysMsV2Resp = new JsonObject();
        sysMsV2Resp.addProperty("systemId", "fae7d4a7-a74c-418b-a61a-433d92747358");
        sysMsV2Resp.addProperty("hostDeviceId", "000100073534");

        JsonObject siteMsResp = new JsonObject();
        siteMsResp.addProperty("systemId", "fae7d4a7-a74c-418b-a61a-433d92747358");
        siteMsResp.addProperty("siteId", "sample_site_id");

        JsonObject odinMsResp = new JsonObject();
        odinMsResp.addProperty("id", UUID.randomUUID().toString());
        odinMsResp.addProperty("code", "SUCCESS");

        MockResponse mockResponseSysV2Ms = new MockResponse();
        mockResponseSysV2Ms.addHeader("Content-Type", "application/json");
        mockResponseSysV2Ms.setStatus("HTTP/1.1 200");
        mockResponseSysV2Ms.setBody(sysMsV2Resp.toString());
        mockBackEnd.enqueue(mockResponseSysV2Ms); // simulate system-ms response

        MockResponse mockResponseSiteMs = new MockResponse();
        mockResponseSiteMs.addHeader("Content-Type", "application/json");
        mockResponseSiteMs.setStatus("HTTP/1.1 200");
        mockResponseSiteMs.setBody(siteMsResp.toString());
        mockBackEnd.enqueue(mockResponseSiteMs); // simulate system-ms response

        MockResponse mockResponseOdinMs = new MockResponse();
        mockResponseOdinMs.addHeader("Content-Type", "application/json");
        mockResponseOdinMs.setStatus("HTTP/1.1 202");
        mockResponseOdinMs.setBody(odinMsResp.toString());
        mockBackEnd.enqueue(mockResponseOdinMs); // simulate odin-ms response
    }

    private void enqueueSystemResponseIntoMockServer(
            String inverterId, SystemFamilyOuterClass.SystemFamily systemFamily) {
        JsonObject sysMsResp = new JsonObject();
        if (systemFamily.equals(SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R1)) {
            sysMsResp.addProperty("rcpId", inverterId);
        }
        if (systemFamily.equals(SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R2)) {
            sysMsResp.addProperty("hostDeviceId", inverterId);
        }

        MockResponse mockResponseSysMs = new MockResponse();
        mockResponseSysMs.addHeader("Content-Type", "application/json");
        mockResponseSysMs.setStatus("HTTP/1.1 200");
        mockResponseSysMs.setBody(sysMsResp.toString());
        mockBackEnd.enqueue(mockResponseSysMs); // simulate system-ms response
    }
}
