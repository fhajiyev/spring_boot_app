package com.generac.ces.systemgateway.service.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generac.ces.essdataprovider.enums.InverterSettingFields;
import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.essdataprovider.fields.FieldValue;
import com.generac.ces.essdataprovider.fields.SysModesListField;
import com.generac.ces.essdataprovider.model.InverterSettingResponseDto;
import com.generac.ces.essdataprovider.model.ModeResponseDto;
import com.generac.ces.systemgateway.configuration.CacheStore;
import com.generac.ces.systemgateway.configuration.TestConfig;
import com.generac.ces.systemgateway.exception.ResourceNotFoundException;
import com.generac.ces.systemgateway.exception.TooManyRequestsException;
import com.generac.ces.systemgateway.exception.UnprocessableEntityException;
import com.generac.ces.systemgateway.helper.SystemSettingsCachedResponseProcessor;
import com.generac.ces.systemgateway.model.ParameterTimestampMap;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateResponse;
import com.generac.ces.systemgateway.model.system.SystemModeGetResponse;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateResponse;
import com.generac.ces.systemgateway.service.SystemSettingCacheService;
import com.generac.ces.systemgateway.service.dataprovider.EssDataProviderService;
import com.generac.ces.systemgateway.service.odin.OdinService;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = {TestConfig.class})
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:test.properties")
public class SystemModeServiceTest {

    @InjectMocks private SystemModeService systemModeService;

    @Mock private EssSystemService essSystemService;

    @Mock private EssDataProviderService essDataProviderService;

    @Mock private OdinService odinService;

    @Mock private SystemSettingCacheService systemSettingCacheService;

    @SpyBean private CacheStore<ParameterTimestampMap> remoteSettingsCache;

    @InjectMocks
    private SystemSettingsCachedResponseProcessor systemSettingsCachedResponseProcessor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    // =======================================================================================================
    //   GET SYSTEM MODE
    // =======================================================================================================

    @Test
    public void testGetSysMode_EmptySystemMode() {
        SystemResponse systemResponse = new SystemResponse();
        UUID systemId = UUID.randomUUID();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId("dummyRcpId");
        OffsetDateTime dummyTimestamp = OffsetDateTime.parse("2023-07-14T17:25:13Z");
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponse);
        when(systemSettingCacheService.getSettings(any(), any())).thenReturn(null);

        ModeResponseDto modeResponseDto = new ModeResponseDto(null, null, null, null);
        when(essDataProviderService.getCurrentSysMode(any()))
                .thenReturn(Mono.just(modeResponseDto));

        FieldValue sysModesAllowed =
                new SysModesListField(
                        InverterSettingFields.sysModesAllowed.name(),
                        Arrays.asList(
                                SysModes.Sell,
                                SysModes.CleanBackup,
                                SysModes.SelfSupply,
                                SysModes.GridTie,
                                SysModes.SafetyShutdown));
        when(essDataProviderService.getAllowedSysModesByInverter(any()))
                .thenReturn(
                        Mono.just(
                                InverterSettingResponseDto.builder()
                                        .settings(
                                                Map.of(
                                                        InverterSettingFields.sysModesAllowed
                                                                .name(),
                                                        sysModesAllowed))
                                        .build()));
        try {
            systemModeService.getSystemMode(systemId).block();
            Assert.fail("Excepted ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException ignored) {
        }
    }

    @Test
    public void testGetSysMode_SuccessCacheMiss() {
        SystemResponse systemResponse = new SystemResponse();
        UUID systemId = UUID.randomUUID();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId("dummyRcpId");
        OffsetDateTime dummyTimestamp = OffsetDateTime.parse("2023-07-14T17:25:13Z");
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponse);
        when(systemSettingCacheService.updateSystemModeWithCachedSettings(
                        any(), any(), any(), any(), any()))
                .thenReturn(null);

        /// Define the desired order for allowed modes
        List<SystemMode> expectedActiveModes =
                Arrays.asList(
                        SystemMode.GRID_TIE,
                        SystemMode.SELF_SUPPLY,
                        SystemMode.CLEAN_BACKUP,
                        SystemMode.PRIORITY_BACKUP,
                        SystemMode.SELL);

        ModeResponseDto modeResponseDto =
                new ModeResponseDto(2, "self supply", SysModes.SelfSupply, dummyTimestamp);
        when(essDataProviderService.getCurrentSysMode(any()))
                .thenReturn(Mono.just(modeResponseDto));

        FieldValue sysModesAllowed =
                new SysModesListField(
                        InverterSettingFields.sysModesAllowed.name(),
                        Arrays.asList(
                                SysModes.Sell,
                                SysModes.CleanBackup,
                                SysModes.SelfSupply,
                                SysModes.GridTie,
                                SysModes.PriorityBackup));
        when(essDataProviderService.getAllowedSysModesByInverter(any()))
                .thenReturn(
                        Mono.just(
                                InverterSettingResponseDto.builder()
                                        .settings(
                                                Map.of(
                                                        InverterSettingFields.sysModesAllowed
                                                                .name(),
                                                        sysModesAllowed))
                                        .build()));

        SystemModeGetResponse systemModeResponse =
                systemModeService.getSystemMode(systemId).block();

        assert systemModeResponse != null;

        Assert.assertEquals(5, systemModeResponse.activeModes().size());
        Assertions.assertIterableEquals(expectedActiveModes, systemModeResponse.activeModes());
        Assert.assertEquals(SystemMode.SELF_SUPPLY, systemModeResponse.mode());
        Assert.assertEquals(dummyTimestamp, systemModeResponse.updatedTimestampUtc());
    }

    @Test
    public void testGetSysMode_noAvailSysModes() {
        SystemResponse systemResponse = new SystemResponse();
        UUID systemId = UUID.randomUUID();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId("dummyRcpId");
        OffsetDateTime dummyTimestamp = OffsetDateTime.parse("2023-07-14T17:25:13Z");
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponse);
        when(systemSettingCacheService.updateSystemModeWithCachedSettings(
                        any(), any(), any(), any(), any()))
                .thenReturn(null);

        ModeResponseDto modeResponseDto =
                new ModeResponseDto(2, "self supply", SysModes.SelfSupply, dummyTimestamp);
        when(essDataProviderService.getCurrentSysMode(any()))
                .thenReturn(Mono.just(modeResponseDto));

        FieldValue sysModesAllowed =
                new SysModesListField(
                        InverterSettingFields.sysModesAllowed.name(), Collections.emptyList());
        when(essDataProviderService.getAllowedSysModesByInverter(any()))
                .thenReturn(
                        Mono.just(
                                InverterSettingResponseDto.builder()
                                        .settings(
                                                Map.of(
                                                        InverterSettingFields.sysModesAllowed
                                                                .name(),
                                                        sysModesAllowed))
                                        .build()));

        SystemModeGetResponse systemModeResponse =
                systemModeService.getSystemMode(systemId).block();

        assert systemModeResponse != null;

        Assert.assertEquals(0, systemModeResponse.activeModes().size());
    }

    @Test
    public void testGetSysMode_offlineSystem_availableModesNotFound() {
        SystemResponse systemResponse = new SystemResponse();
        UUID systemId = UUID.randomUUID();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId("dummyRcpId");
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponse);

        when(essDataProviderService.getAllowedSysModesByInverter(any()))
                .thenThrow(new ResourceNotFoundException("dummyRcpId"));

        try {
            systemModeService.getSystemMode(systemId);
            Assert.fail("Excepted ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException ignored) {
        }
    }

    @Test
    public void testGetSysMode_offlineSystem_activeModesNotFound() {
        SystemResponse systemResponse = new SystemResponse();
        UUID systemId = UUID.randomUUID();
        systemResponse.setSystemId(systemId);
        systemResponse.setRcpId("dummyRcpId");
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponse);

        // Found available SysModes but not currently active sysMode
        FieldValue sysModesAllowed =
                new SysModesListField(
                        InverterSettingFields.sysModesAllowed.name(), Collections.emptyList());
        when(essDataProviderService.getAllowedSysModesByInverter(any()))
                .thenReturn(
                        Mono.just(
                                InverterSettingResponseDto.builder()
                                        .settings(
                                                Map.of(
                                                        InverterSettingFields.sysModesAllowed
                                                                .name(),
                                                        sysModesAllowed))
                                        .build()));
        when(essDataProviderService.getCurrentSysMode(any()))
                .thenThrow(new ResourceNotFoundException(systemId.toString()));

        try {
            systemModeService.getSystemMode(systemId);
            Assert.fail("Excepted ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException ignored) {
        }
    }

    @Test
    public void testGetSysMode_Error() {
        when(essSystemService.getSystemBySystemId(any())).thenReturn(new SystemResponse());

        UUID systemId = UUID.randomUUID();
        try {
            systemModeService.getSystemMode(systemId);
            Assert.fail("Excepted UnprocessableEntityException to be thrown");
        } catch (UnprocessableEntityException ignored) {
        }
    }

    @Test
    public void testUpdateSystemMode_RateLimiting() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        String beaconRcpn = "000100121057";
        SystemResponse systemResponseRcpn = new SystemResponse();
        systemResponseRcpn.setBeaconRcpn(beaconRcpn);
        SystemModeUpdateResponse systemResponse =
                new SystemModeUpdateResponse(
                        systemId, updateId, SystemMode.CLEAN_BACKUP, OffsetDateTime.now());

        when(odinService.postSysModeControlMessages(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(ResponseEntity.accepted().body(systemResponse)));
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponseRcpn);
        Mockito.doNothing().when(systemSettingCacheService).throwOnRateLimit(any(), any(), any());
        Mockito.doNothing().when(systemSettingCacheService).saveSettings(any(), any(), any());

        systemModeService
                .updateSystemMode(
                        SystemType.ESS,
                        systemId,
                        SystemModeUpdateRequest.builder().build(),
                        "someCallerId",
                        "userId",
                        true)
                .block(); // call updateSystemMode once to populate cache

        // Action
        try {
            Mockito.doThrow(new TooManyRequestsException("Expected exception"))
                    .when(systemSettingCacheService)
                    .throwOnRateLimit(any(), any(), any());

            systemModeService.updateSystemMode(
                    SystemType.ESS,
                    systemId,
                    SystemModeUpdateRequest.builder().build(),
                    "someCallerId",
                    "userId",
                    true);
            Assert.fail("Excepted TooManyRequestsException to be thrown");
        } catch (TooManyRequestsException ignored) {
        }
        verify(odinService, times(1)).postSysModeControlMessages(any(), any(), any(), any(), any());
        verify(systemSettingCacheService, times(2)).throwOnRateLimit(any(), any(), any());
        verify(systemSettingCacheService, times(1)).saveSettings(any(), any(), any());
    }

    @Test
    public void testUpdateSystemMode_Scheduled() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        String beaconRcpn = "0001001282AE";
        SystemResponse systemResponse = new SystemResponse();
        systemResponse.setBeaconRcpn(beaconRcpn);
        SystemModeUpdateResponse systemModeUpdateResponseResponse =
                new SystemModeUpdateResponse(
                        systemId, updateId, SystemMode.CLEAN_BACKUP, OffsetDateTime.now());
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponse);
        when(odinService.postSysModeControlMessages(any(), any(), any(), any(), any()))
                .thenReturn(
                        Mono.just(
                                ResponseEntity.accepted().body(systemModeUpdateResponseResponse)));
        Mockito.doNothing().when(systemSettingCacheService).throwOnRateLimit(any(), any(), any());

        // Action
        systemModeService
                .updateSystemMode(
                        SystemType.ESS,
                        systemId,
                        SystemModeUpdateRequest.builder().build(),
                        "someCallerId",
                        "userId",
                        false)
                .block();

        verify(odinService, times(1)).postSysModeControlMessages(any(), any(), any(), any(), any());
        verify(systemSettingCacheService, times(0)).throwOnRateLimit(any(), any(), any());
        verify(systemSettingCacheService, times(0)).saveSettings(any(), any(), any());
    }

    // =======================================================================================================
    //   PATCH ACTIVE SYSTEM MODES
    // =======================================================================================================

    @Test
    public void testSetActiveSystemModes_RateLimitingSuccess() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        String beaconRcpn = "000100121057";
        SystemResponse systemResponseRcpn = new SystemResponse();
        systemResponseRcpn.setBeaconRcpn(beaconRcpn);

        ActiveSystemModeUpdateResponse systemResponse =
                new ActiveSystemModeUpdateResponse(
                        systemId, updateId, List.of(SystemMode.SELL), OffsetDateTime.now());

        when(odinService.setActiveSystemModes(any(), any(), any(), any()))
                .thenReturn(Mono.just(systemResponse));
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponseRcpn);
        when(systemSettingCacheService.getSettings(any(), any())).thenReturn(null);
        Mockito.doNothing().when(systemSettingCacheService).saveSettings(any(), any(), any());

        // Action
        systemModeService
                .setActiveSystemModes(
                        systemId,
                        new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELL)),
                        SystemType.ESS,
                        "someCallerId",
                        "userId")
                .block();

        verify(odinService, times(1)).setActiveSystemModes(any(), any(), any(), any());
        verify(systemSettingCacheService, times(1)).throwOnRateLimit(any(), any(), any());
        verify(systemSettingCacheService, times(1)).saveSettings(any(), any(), any());
    }

    @Test
    public void testSetActiveSystemModes_EmptyCache_NoRateLimiting() {
        // Arrange
        UUID systemId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        String beaconRcpn = "000100121057";
        SystemResponse systemResponseRcpn = new SystemResponse();
        systemResponseRcpn.setBeaconRcpn(beaconRcpn);

        ActiveSystemModeUpdateResponse systemResponse =
                new ActiveSystemModeUpdateResponse(
                        systemId, updateId, List.of(SystemMode.SELL), OffsetDateTime.now());

        when(odinService.setActiveSystemModes(any(), any(), any(), any()))
                .thenReturn(Mono.just(systemResponse));
        when(essSystemService.getSystemBySystemId(any())).thenReturn(systemResponseRcpn);
        Mockito.doNothing().when(systemSettingCacheService).throwOnRateLimit(any(), any(), any());
        Mockito.doNothing().when(systemSettingCacheService).saveSettings(any(), any(), any());

        systemModeService
                .setActiveSystemModes(
                        systemId,
                        new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELL)),
                        SystemType.ESS,
                        "someCallerId",
                        "userId")
                .block(); // call setActiveSystemModes once to populate cache

        // Action
        try {
            Mockito.doThrow(new TooManyRequestsException("Expected exception"))
                    .when(systemSettingCacheService)
                    .throwOnRateLimit(any(), any(), any());

            systemModeService.setActiveSystemModes(
                    systemId,
                    new ActiveSystemModeUpdateRequest(List.of(SystemMode.SELL)),
                    SystemType.ESS,
                    "someCallerId",
                    "userId");
            Assert.fail("Excepted TooManyRequestsException to be thrown");
        } catch (TooManyRequestsException ignored) {
        }
        verify(odinService, times(1)).setActiveSystemModes(any(), any(), any(), any());
        verify(systemSettingCacheService, times(2)).throwOnRateLimit(any(), any(), any());
        verify(systemSettingCacheService, times(1)).saveSettings(any(), any(), any());
    }
}
