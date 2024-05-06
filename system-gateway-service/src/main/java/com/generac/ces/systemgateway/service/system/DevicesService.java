package com.generac.ces.systemgateway.service.system;

import com.generac.ces.essdataprovider.model.DeviceMapRequestDto;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.system.SystemFamilyOuterClass;
import com.generac.ces.systemgateway.entity.device.DeviceMap;
import com.generac.ces.systemgateway.entity.device.SystemFamily;
import com.generac.ces.systemgateway.entity.rgm.RgmEnergyLastUpdate;
import com.generac.ces.systemgateway.helper.DeviceListDtoMapper;
import com.generac.ces.systemgateway.helper.DeviceMapDtoMapper;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.device.DeviceListRequest;
import com.generac.ces.systemgateway.repository.device.DeviceDataRepository;
import com.generac.ces.systemgateway.repository.rgm.RgmRepository;
import com.google.common.collect.Lists;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DevicesService {

    @Autowired private DeviceDataRepository deviceDataRepository;

    @Autowired private RgmRepository rgmRepository;

    @Autowired private EssSystemService essSystemService;

    @Autowired private SystemV2Service systemV2Service;

    private final Map<String, SystemFamilyOuterClass.SystemFamily> systemFamilyCache =
            new HashMap<>();

    private static final long THREE_DAYS_IN_SECONDS = Duration.ofDays(3).getSeconds();

    public List<DeviceMapResponseDto> getBulkDeviceMap(DeviceMapRequestDto requestDto) {

        Map<String, DeviceMapResponseDto> systemIdToResponseDtoMap = new TreeMap<>();
        Map<String, String> inverterIdToSystemIdMap = new HashMap<>();
        prepareMapOfDeviceMapResponse(
                requestDto, systemIdToResponseDtoMap, inverterIdToSystemIdMap);

        List<RgmEnergyLastUpdate> lastHeardBySystemIds = new ArrayList<>();
        try {
            lastHeardBySystemIds =
                    rgmRepository.findLastUpdateBySystemIdIn(
                            Lists.newArrayList(systemIdToResponseDtoMap.keySet()),
                            Timestamp.from(Instant.now().minusSeconds(THREE_DAYS_IN_SECONDS)));
        } catch (Exception e) {
            log.warn("Failed to get RGM lastHeard date", e);
        }

        Map<String, Timestamp> timeStampsBySystemIdsMap = new HashMap<>();
        lastHeardBySystemIds.forEach(
                data -> timeStampsBySystemIdsMap.put(data.getSystemId(), data.getTimestampLocal()));

        List<DeviceMap> deviceMapEntities =
                deviceDataRepository.findR1DeviceMap(inverterIdToSystemIdMap.keySet());
        deviceMapEntities.forEach(
                deviceMapEntity -> {
                    String systemId = inverterIdToSystemIdMap.get(deviceMapEntity.getInverterId());
                    DeviceMapResponseDto dto = systemIdToResponseDtoMap.get(systemId);
                    switch (deviceMapEntity.getDeviceType()) {
                        case INVERTER:
                            dto.getInverters()
                                    .add(
                                            DeviceMapDtoMapper.INSTANCE.entityToInverter(
                                                    deviceMapEntity));
                            break;
                        case PVLINK:
                            dto.getPvls()
                                    .add(DeviceMapDtoMapper.INSTANCE.entityToPvls(deviceMapEntity));
                            break;
                        case ICM:
                            dto.getIcms()
                                    .add(DeviceMapDtoMapper.INSTANCE.entityToIcm(deviceMapEntity));
                            break;
                        case BATTERY:
                            dto.getBatteries()
                                    .add(
                                            DeviceMapDtoMapper.INSTANCE.entityToBattery(
                                                    deviceMapEntity));
                            break;
                        case BEACON:
                            dto.getBeacons()
                                    .add(
                                            DeviceMapDtoMapper.INSTANCE.entityToBeacon(
                                                    deviceMapEntity));
                            break;
                        case RGM:
                            if (timeStampsBySystemIdsMap.get(systemId) != null) {
                                deviceMapEntity.setLastHeard(
                                        timeStampsBySystemIdsMap.get(systemId));
                            }
                            dto.getRgms()
                                    .add(DeviceMapDtoMapper.INSTANCE.entityToRgm(deviceMapEntity));
                            break;
                        case GENERATOR:
                            dto.getGenerators()
                                    .add(
                                            DeviceMapDtoMapper.INSTANCE.entityToGenerator(
                                                    deviceMapEntity));
                            break;
                        case LOAD_CONTROLLER:
                            dto.getLoadcontrollers()
                                    .add(
                                            DeviceMapDtoMapper.INSTANCE.entityToLoadController(
                                                    deviceMapEntity));
                            break;
                        default:
                            log.warn("unknown device type: " + deviceMapEntity.getDeviceType());
                            break;
                    }
                });

        return new ArrayList<>(systemIdToResponseDtoMap.values());
    }

    private void prepareMapOfDeviceMapResponse(
            DeviceMapRequestDto requestDto,
            Map<String, DeviceMapResponseDto> respMap,
            Map<String, String> hostMap) {
        requestDto
                .getSystems()
                .forEach(
                        system -> {
                            DeviceMapResponseDto dto = new DeviceMapResponseDto();
                            dto.setSystemId(system.getSystemId());
                            respMap.put(system.getSystemId(), dto);
                            system.getHosts()
                                    .forEach(
                                            host ->
                                                    hostMap.putIfAbsent(
                                                            host.getHostRcpn(),
                                                            system.getSystemId()));
                        });
    }

    public List<DeviceList> getBulkDeviceList(DeviceListRequest requestDto) {

        Map<String, DeviceList> systemIdToDeviceListMap = new TreeMap<>();
        Map<String, String> inverterIdToSystemIdR1Map = new HashMap<>();
        Map<String, String> inverterIdToSystemIdR2Map = new HashMap<>();

        prepareMapOfDeviceList(
                requestDto,
                systemIdToDeviceListMap,
                inverterIdToSystemIdR1Map,
                inverterIdToSystemIdR2Map);

        List<RgmEnergyLastUpdate> lastHeardBySystemIds = new ArrayList<>();
        try {
            lastHeardBySystemIds =
                    rgmRepository.findLastUpdateBySystemIdIn(
                            Lists.newArrayList(systemIdToDeviceListMap.keySet()),
                            Timestamp.from(Instant.now().minusSeconds(THREE_DAYS_IN_SECONDS)));
        } catch (Exception e) {
            log.warn("Failed to get RGM lastHeard date", e);
        }

        Map<String, Timestamp> timeStampsBySystemIdsMap = new HashMap<>();
        lastHeardBySystemIds.forEach(
                data -> timeStampsBySystemIdsMap.put(data.getSystemId(), data.getTimestampLocal()));

        if (inverterIdToSystemIdR1Map.keySet().size() > 0) {
            List<DeviceMap> deviceMapEntities =
                    deviceDataRepository.findR1DeviceMap(inverterIdToSystemIdR1Map.keySet());
            deviceMapEntities.forEach(
                    deviceMapEntity -> {
                        String systemId =
                                inverterIdToSystemIdR1Map.get(deviceMapEntity.getInverterId());
                        DeviceList dto = systemIdToDeviceListMap.get(systemId);
                        dto.getDevices()
                                .add(
                                        DeviceListDtoMapper.INSTANCE.entityToDevice(
                                                deviceMapEntity,
                                                deviceMapEntity.getDeviceType(),
                                                timeStampsBySystemIdsMap.get(systemId)));
                    });
        }

        if (inverterIdToSystemIdR2Map.keySet().size() > 0) {
            List<DeviceMap> deviceMapEntities =
                    deviceDataRepository.findR2DeviceMap(inverterIdToSystemIdR2Map.keySet());
            deviceMapEntities.forEach(
                    deviceMapEntity -> {
                        String systemId =
                                inverterIdToSystemIdR2Map.get(deviceMapEntity.getInverterId());
                        DeviceList dto = systemIdToDeviceListMap.get(systemId);
                        dto.getDevices()
                                .add(
                                        DeviceListDtoMapper.INSTANCE.entityToDevice(
                                                deviceMapEntity,
                                                deviceMapEntity.getDeviceType(),
                                                timeStampsBySystemIdsMap.get(systemId)));
                    });
        }

        return new ArrayList<>(systemIdToDeviceListMap.values());
    }

    private void prepareMapOfDeviceList(
            DeviceListRequest requestDto,
            Map<String, DeviceList> respMap,
            Map<String, String> hostMapR1,
            Map<String, String> hostMapR2) {

        List<String> systemsWithoutSystemFamily = new ArrayList<>();
        Map<String, SystemFamilyOuterClass.SystemFamily> systemFamilyMap = new HashMap<>();
        requestDto
                .getSystems()
                .forEach(
                        system -> {
                            DeviceList dto = new DeviceList();
                            dto.setSystemId(system.getSystemId());
                            respMap.put(system.getSystemId(), dto);

                            if (system.getSystemFamily() == null
                                    || system.getSystemFamily()
                                            .equals(
                                                    SystemFamilyOuterClass.SystemFamily
                                                            .SYSTEM_FAMILY_UNSPECIFIED)) {

                                SystemFamilyOuterClass.SystemFamily cachedSysFamily =
                                        systemFamilyCache.get(system.getSystemId());
                                // if in local cache then use it
                                if (cachedSysFamily != null) {
                                    systemFamilyMap.put(system.getSystemId(), cachedSysFamily);
                                }
                                // otherwise add to list for db query
                                else {
                                    systemsWithoutSystemFamily.add(system.getSystemId());
                                }
                            }
                            // provided in request already
                            else {
                                systemFamilyMap.put(system.getSystemId(), system.getSystemFamily());
                            }
                        });

        // get system id to system family mapping
        if (systemsWithoutSystemFamily.size() > 0) {
            List<SystemFamily> systemFamilies =
                    deviceDataRepository.findSystemFamiliesForSystemIds(systemsWithoutSystemFamily);
            for (SystemFamily sf : systemFamilies) {
                systemFamilyCache.put(sf.getSystemId(), sf.getSystemFamily());
                systemFamilyMap.put(sf.getSystemId(), sf.getSystemFamily());
            }
        }

        for (String systemId : systemFamilyMap.keySet()) {
            if (!systemFamilyMap
                    .get(systemId)
                    .equals(SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_UNSPECIFIED)) {

                SystemResponse systemResponse;

                if (systemFamilyMap
                        .get(systemId)
                        .equals(SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R1)) {
                    systemResponse =
                            essSystemService.getSystemBySystemId(UUID.fromString(systemId));
                    hostMapR1.put(systemResponse.getRcpId(), systemId);
                }

                if (systemFamilyMap
                        .get(systemId)
                        .equals(SystemFamilyOuterClass.SystemFamily.SYSTEM_FAMILY_PWRCELL_R2)) {
                    systemResponse = systemV2Service.getSystemBySystemId(UUID.fromString(systemId));
                    hostMapR2.put(systemResponse.getHostDeviceId(), systemId);
                }
            } else {
                log.warn("Unspecified system family for systemId: " + systemId);
            }
        }
    }
}
