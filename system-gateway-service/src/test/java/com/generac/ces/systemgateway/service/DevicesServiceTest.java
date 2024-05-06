package com.generac.ces.systemgateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.essdataprovider.model.DeviceMapRequestDto;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.system.SystemFamilyOuterClass;
import com.generac.ces.systemgateway.entity.device.DeviceMap;
import com.generac.ces.systemgateway.entity.device.SystemFamily;
import com.generac.ces.systemgateway.model.SystemResponse;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.device.DeviceListRequest;
import com.generac.ces.systemgateway.model.enums.DeviceType;
import com.generac.ces.systemgateway.repository.device.DeviceDataRepository;
import com.generac.ces.systemgateway.repository.rgm.RgmRepository;
import com.generac.ces.systemgateway.service.system.DevicesService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.generac.ces.systemgateway.service.system.SystemV2Service;
import com.github.javafaker.Faker;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DevicesServiceTest {

    @InjectMocks private DevicesService devicesService;

    @Mock private DeviceDataRepository deviceDataRepository;

    @Mock private RgmRepository rgmRepository;

    @Mock private EssSystemService essSystemService;
    @Mock private SystemV2Service systemV2Service;

    @Test
    void testGetBulkDeviceMap() throws JsonProcessingException {

        // Arrange
        String inverterId = "INVERTER_ID_A";
        List<DeviceMap> sampleDeviceMap =
                getSampleDeviceMap(
                        Arrays.asList(
                                DeviceType.PVLINK,
                                DeviceType.INVERTER,
                                DeviceType.BEACON,
                                DeviceType.BATTERY,
                                DeviceType.LOAD_CONTROLLER,
                                DeviceType.RGM,
                                DeviceType.ICM),
                        inverterId);
        when(deviceDataRepository.findR1DeviceMap(anyCollection())).thenReturn(sampleDeviceMap);

        DeviceMapRequestDto requestDto =
                DeviceMapRequestDto.builder()
                        .systems(
                                Arrays.asList(
                                        DeviceMapRequestDto.DeviceMapSystem.builder()
                                                .systemId("systemId-" + inverterId)
                                                .hosts(
                                                        Arrays.asList(
                                                                DeviceMapRequestDto.Host.builder()
                                                                        .hostRcpn(inverterId)
                                                                        .build()))
                                                .build()))
                        .build();

        // Action
        List<DeviceMapResponseDto> relatedDevices = devicesService.getBulkDeviceMap(requestDto);

        // Assert
        ObjectMapper om = new ObjectMapper();

        assertEquals(1, relatedDevices.size());
        assertEquals(1, relatedDevices.get(0).getPvls().size());
        assertEquals(1, relatedDevices.get(0).getInverters().size());
        assertEquals(1, relatedDevices.get(0).getBeacons().size());
        assertEquals(1, relatedDevices.get(0).getBatteries().size());
        assertEquals(1, relatedDevices.get(0).getLoadcontrollers().size());

        assertEquals("systemId-" + inverterId, relatedDevices.get(0).getSystemId());

        for (DeviceMap dm : sampleDeviceMap) {
            if (dm.getDeviceType().equals(DeviceType.PVLINK)) {
                DeviceMapResponseDto.DeviceMapResponseDtoPvls pvl =
                        relatedDevices.get(0).getPvls().get(0);
                assertEquals(dm.getDeviceId(), pvl.getId());
                assertEquals(dm.getManufacturer(), pvl.getManufacturer());
                assertEquals(dm.getModel(), pvl.getName());
                assertEquals(dm.getVersion(), pvl.getVersion());
                assertEquals(dm.getSt(), pvl.getState());
                assertEquals(dm.getStateText(), pvl.getStateText());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        pvl.getLastUpdate());

                Map<String, Double> map =
                        om.readValue(
                                dm.getNameplate(), new TypeReference<HashMap<String, Double>>() {});
                assertEquals(map.get("a_rtg"), pvl.getArtg());
                assertEquals(map.get("w_rtg"), pvl.getWrtg());
            }
            if (dm.getDeviceType().equals(DeviceType.INVERTER)) {
                DeviceMapResponseDto.DeviceMapResponseDtoInverter inverter =
                        relatedDevices.get(0).getInverters().get(0);
                assertEquals(dm.getDeviceId(), inverter.getId());
                assertEquals(dm.getManufacturer(), inverter.getManufacturer());
                assertEquals(dm.getModel(), inverter.getName());
                assertEquals(dm.getVersion(), inverter.getVersion());
                assertEquals(dm.getSt(), inverter.getState());
                assertEquals(dm.getStateText(), inverter.getStateText());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        inverter.getLastUpdate());
                assertEquals(dm.getSerialNumber(), inverter.getSerialNumber());

                Map<String, Double> map =
                        om.readValue(
                                dm.getNameplate(), new TypeReference<HashMap<String, Double>>() {});
                assertEquals(map.get("a_rtg"), inverter.getArtg());
                assertEquals(map.get("w_rtg"), inverter.getWrtg());
                assertEquals(map.get("va_rtg"), inverter.getVaRtg());
            }
            if (dm.getDeviceType().equals(DeviceType.BEACON)) {
                DeviceMapResponseDto.DeviceMapResponseDtoBeacon beacon =
                        relatedDevices.get(0).getBeacons().get(0);
                assertEquals(dm.getDeviceId(), beacon.getId());
                assertEquals(dm.getManufacturer(), beacon.getManufacturer());
                assertEquals(dm.getModel(), beacon.getName());
                assertEquals(dm.getVersion(), beacon.getVersion());
                assertEquals(dm.getSt(), beacon.getState());
                assertEquals(dm.getStateText(), beacon.getStateText());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        beacon.getLastUpdate());
            }
            if (dm.getDeviceType().equals(DeviceType.BATTERY)) {
                DeviceMapResponseDto.DeviceMapResponseDtoBattery battery =
                        relatedDevices.get(0).getBatteries().get(0);
                assertEquals(dm.getDeviceId(), battery.getId());
                assertEquals(dm.getManufacturer(), battery.getManufacturer());
                assertEquals(dm.getModel(), battery.getName());
                assertEquals(dm.getVersion(), battery.getVersion());
                assertEquals(dm.getSt(), battery.getState());
                assertEquals(dm.getStateText(), battery.getStateText());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        battery.getLastUpdate());

                Map<String, Double> map =
                        om.readValue(
                                dm.getNameplate(), new TypeReference<HashMap<String, Double>>() {});
                assertEquals(map.get("soc_rsv_min"), battery.getSocRsvMin());
                assertEquals(map.get("soc_rsv_max"), battery.getSocRsvMax());
            }
            if (dm.getDeviceType().equals(DeviceType.LOAD_CONTROLLER)) {
                DeviceMapResponseDto.DeviceMapResponseDtoLoadController loadController =
                        relatedDevices.get(0).getLoadcontrollers().get(0);
                assertEquals(dm.getDeviceId(), loadController.getId());
                assertEquals(dm.getManufacturer(), loadController.getManufacturer());
                assertEquals(dm.getModel(), loadController.getName());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        loadController.getLastUpdate());
            }
            if (dm.getDeviceType().equals(DeviceType.RGM)) {
                DeviceMapResponseDto.DeviceMapResponseDtoRgm rgm =
                        relatedDevices.get(0).getRgms().get(0);
                assertEquals(dm.getDeviceId(), rgm.getId());
                assertEquals(dm.getManufacturer(), rgm.getManufacturer());
                assertEquals(dm.getModel(), rgm.getName());
                assertEquals(dm.getVersion(), rgm.getVersion());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        rgm.getLastUpdate());
            }
            if (dm.getDeviceType().equals(DeviceType.ICM)) {
                DeviceMapResponseDto.DeviceMapResponseDtoIcm icm =
                        relatedDevices.get(0).getIcms().get(0);
                assertEquals(dm.getDeviceId(), icm.getId());
                assertEquals(dm.getManufacturer(), icm.getManufacturer());
                assertEquals(dm.getModel(), icm.getName());
                assertEquals(dm.getVersion(), icm.getVersion());
                assertEquals(dm.getSt(), icm.getState());
                assertEquals(dm.getStateText(), icm.getStateText());
                assertEquals(
                        dm.getLastHeard()
                                .toLocalDateTime()
                                .atZone(ZoneId.of("UTC"))
                                .toEpochSecond(),
                        icm.getLastUpdate());
            }
        }
    }

    @Test
    void testGetBulkDeviceListR1() throws JsonProcessingException {
        // Arrange
        String inverterId = "INVERTER_ID_A";
        UUID systemId = UUID.randomUUID();
        List<DeviceMap> sampleDeviceMap =
                getSampleDeviceMap(
                        Arrays.asList(
                                DeviceType.PVLINK,
                                DeviceType.INVERTER,
                                DeviceType.BEACON,
                                DeviceType.BATTERY,
                                DeviceType.LOAD_CONTROLLER,
                                DeviceType.RGM,
                                DeviceType.ICM),
                        inverterId);
        when(deviceDataRepository.findR1DeviceMap(anyCollection())).thenReturn(sampleDeviceMap);
        when(deviceDataRepository.findSystemFamiliesForSystemIds(anyCollection()))
                .thenReturn(
                        Arrays.asList(
                                SystemFamily.builder()
                                        .systemId(systemId.toString())
                                        .systemFamily(
                                                SystemFamilyOuterClass.SystemFamily
                                                        .SYSTEM_FAMILY_PWRCELL_R1)
                                        .build()));

        when(essSystemService.getSystemBySystemId(systemId))
                .thenReturn(SystemResponse.builder().rcpId(inverterId).build());

        DeviceListRequest requestDto =
                DeviceListRequest.builder()
                        .systems(
                                Arrays.asList(
                                        DeviceListRequest.DeviceListSystem.builder()
                                                .systemId(systemId.toString())
                                                .build()))
                        .build();

        // Action
        List<DeviceList> relatedDevices = devicesService.getBulkDeviceList(requestDto);

        // Assert
        ObjectMapper om = new ObjectMapper();

        assertEquals(1, relatedDevices.size());
        assertEquals(systemId.toString(), relatedDevices.get(0).getSystemId());
        DeviceList deviceList = relatedDevices.get(0);
        assertEquals(7, deviceList.getDevices().size());

        for (DeviceList.Device d : deviceList.getDevices()) {
            Map<String, String> properties = new HashMap<>();
            for (DeviceList.DeviceProperty dp : d.getProperties()) {
                properties.put(dp.getName(), dp.getValue());
            }
            if (d.getDeviceType().equals(DeviceType.PVLINK)) {

                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.PVLINK)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(dm.getVersion(), properties.get("version"));
                        assertEquals(dm.getSt(), Integer.parseInt(properties.get("state")));
                        assertEquals(dm.getStateText(), properties.get("stateText"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));

                        Map<String, Double> map =
                                om.readValue(
                                        dm.getNameplate(),
                                        new TypeReference<HashMap<String, Double>>() {});
                        assertEquals(map.get("a_rtg"), Double.parseDouble(properties.get("artg")));
                        assertEquals(map.get("w_rtg"), Double.parseDouble(properties.get("wrtg")));

                        break;
                    }
                }
            }
            if (d.getDeviceType().equals(DeviceType.INVERTER)) {
                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.INVERTER)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(dm.getVersion(), properties.get("version"));
                        assertEquals(dm.getSt(), Integer.parseInt(properties.get("state")));
                        assertEquals(dm.getStateText(), properties.get("stateText"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));
                        assertEquals(dm.getSerialNumber(), properties.get("serialNumber"));

                        Map<String, Double> map =
                                om.readValue(
                                        dm.getNameplate(),
                                        new TypeReference<HashMap<String, Double>>() {});
                        assertEquals(map.get("a_rtg"), Double.parseDouble(properties.get("artg")));
                        assertEquals(map.get("w_rtg"), Double.parseDouble(properties.get("wrtg")));
                        assertEquals(
                                map.get("va_rtg"), Double.parseDouble(properties.get("vaRtg")));

                        break;
                    }
                }
            }
            if (d.getDeviceType().equals(DeviceType.BEACON)) {
                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.BEACON)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(dm.getVersion(), properties.get("version"));
                        assertEquals(dm.getSt(), Integer.parseInt(properties.get("state")));
                        assertEquals(dm.getStateText(), properties.get("stateText"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));

                        break;
                    }
                }
            }
            if (d.getDeviceType().equals(DeviceType.BATTERY)) {
                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.BATTERY)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(dm.getVersion(), properties.get("version"));
                        assertEquals(dm.getSt(), Integer.parseInt(properties.get("state")));
                        assertEquals(dm.getStateText(), properties.get("stateText"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));

                        Map<String, Double> map =
                                om.readValue(
                                        dm.getNameplate(),
                                        new TypeReference<HashMap<String, Double>>() {});
                        assertEquals(
                                map.get("soc_rsv_min"),
                                Double.parseDouble(properties.get("socRsvMin")));
                        assertEquals(
                                map.get("soc_rsv_max"),
                                Double.parseDouble(properties.get("socRsvMax")));

                        break;
                    }
                }
            }
            if (d.getDeviceType().equals(DeviceType.LOAD_CONTROLLER)) {
                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.LOAD_CONTROLLER)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));

                        break;
                    }
                }
            }
            if (d.getDeviceType().equals(DeviceType.RGM)) {
                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.RGM)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(dm.getVersion(), properties.get("version"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));

                        break;
                    }
                }
            }
            if (d.getDeviceType().equals(DeviceType.ICM)) {
                for (DeviceMap dm : sampleDeviceMap) {
                    if (dm.getDeviceType().equals(DeviceType.ICM)) {
                        assertEquals(dm.getDeviceId(), d.getDeviceId());
                        assertEquals(dm.getManufacturer(), properties.get("manufacturer"));
                        assertEquals(dm.getModel(), properties.get("name"));
                        assertEquals(dm.getVersion(), properties.get("version"));
                        assertEquals(dm.getSt(), Integer.parseInt(properties.get("state")));
                        assertEquals(dm.getStateText(), properties.get("stateText"));
                        assertEquals(
                                dm.getLastHeard()
                                        .toLocalDateTime()
                                        .atZone(ZoneId.of("UTC"))
                                        .toEpochSecond(),
                                Long.parseLong(properties.get("lastUpdate")));

                        break;
                    }
                }
            }
        }
    }

    @Test
    void testGetBulkDeviceListR2() throws JsonProcessingException {
        // Arrange
        String inverterId = "INVERTER_ID_A";
        UUID systemId = UUID.randomUUID();
        List<DeviceMap> sampleDeviceMap =
                getSampleDeviceMap(
                        Arrays.asList(
                                DeviceType.PVLINK,
                                DeviceType.INVERTER,
                                DeviceType.BEACON,
                                DeviceType.BATTERY,
                                DeviceType.LOAD_CONTROLLER,
                                DeviceType.RGM,
                                DeviceType.ICM),
                        inverterId);
        when(deviceDataRepository.findR2DeviceMap(anyCollection())).thenReturn(sampleDeviceMap);
        when(systemV2Service.getSystemBySystemId(systemId))
                .thenReturn(SystemResponse.builder().hostDeviceId(inverterId).build());

        DeviceListRequest requestDto =
                DeviceListRequest.builder()
                        .systems(
                                Arrays.asList(
                                        DeviceListRequest.DeviceListSystem.builder()
                                                .systemId(systemId.toString())
                                                .systemFamily(
                                                        SystemFamilyOuterClass.SystemFamily
                                                                .SYSTEM_FAMILY_PWRCELL_R2)
                                                .build()))
                        .build();

        // Action
        List<DeviceList> relatedDevices = devicesService.getBulkDeviceList(requestDto);

        // Assert
        ObjectMapper om = new ObjectMapper();

        assertEquals(1, relatedDevices.size());
        assertEquals(systemId.toString(), relatedDevices.get(0).getSystemId());
        DeviceList deviceList = relatedDevices.get(0);
        assertEquals(7, deviceList.getDevices().size());

        verify(systemV2Service, times(1)).getSystemBySystemId(systemId);
        verify(essSystemService, times(0)).getSystemBySystemId(systemId);
        // family already given in request so no need to check system family map table
        verify(deviceDataRepository, times(0)).findSystemFamiliesForSystemIds(anyCollection());
    }

    private List<DeviceMap> getSampleDeviceMap(
            List<DeviceType> deviceTypesInDeviceMap, String inverterId) {
        Faker faker = Faker.instance();
        String nameplate = null;
        List<DeviceMap> devicesInMap = new ArrayList<>();
        for (int i = 0; i < deviceTypesInDeviceMap.size(); i++) {
            DeviceType deviceType = deviceTypesInDeviceMap.get(i);
            if (deviceType.equals(DeviceType.PVLINK)) {
                nameplate =
                        "{\"a_rtg\":"
                                + faker.random().nextDouble()
                                + ",\"w_rtg\":"
                                + faker.random().nextDouble()
                                + "}";
            }
            if (deviceType.equals(DeviceType.INVERTER)) {
                nameplate =
                        "{\"va_rtg\":"
                                + faker.random().nextDouble()
                                + ",\"a_rtg\":"
                                + faker.random().nextDouble()
                                + ",\"w_rtg\":"
                                + faker.random().nextDouble()
                                + "}";
            }
            if (deviceType.equals(DeviceType.BATTERY)) {
                nameplate =
                        "{\"soc_rsv_min\":"
                                + faker.random().nextDouble()
                                + ",\"soc_rsv_max\":"
                                + faker.random().nextDouble()
                                + "}";
            }
            devicesInMap.add(
                    DeviceMap.builder()
                            .deviceType(deviceType)
                            .inverterId(inverterId)
                            .deviceId("RCPN" + i + 1)
                            .manufacturer(faker.idNumber().valid())
                            .model(faker.idNumber().valid())
                            .version(faker.idNumber().valid())
                            .st(faker.random().nextInt(0, 100000))
                            .stateText(faker.idNumber().valid())
                            .lastHeard(Timestamp.from(Instant.now().minusSeconds(i)))
                            .serialNumber(faker.idNumber().valid())
                            .nameplate(nameplate)
                            .build());
        }

        return devicesInMap;
    }
}
