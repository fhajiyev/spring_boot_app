package com.generac.ces.systemgateway.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.entity.device.DeviceMapInterface;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.enums.DeviceType;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.HashMap;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
        config = DeviceListDtoMapperConfig.class,
        uses = Formatter.class,
        builder = @Builder(disableBuilder = true))
public interface DeviceListDtoMapper {

    DeviceListDtoMapper INSTANCE = Mappers.getMapper(DeviceListDtoMapper.class);
    Logger log = LoggerFactory.getLogger(DeviceListDtoMapper.class);
    TypeReference<HashMap<String, Double>> typeRef =
            new TypeReference<HashMap<String, Double>>() {};
    ObjectMapper objectMapper = new ObjectMapper();
    String loadControllerPrefix = "0x2000";

    @AfterMapping
    default void after(
            DeviceMapInterface deviceMapEntity,
            @MappingTarget DeviceList.Device device,
            DeviceType deviceType,
            Timestamp lastHeard) {

        device.setDeviceType(deviceType);

        // common
        if (deviceMapEntity.getManufacturer() != null) {
            device.getProperties()
                    .add(
                            DeviceList.DeviceProperty.builder()
                                    .name("manufacturer")
                                    .type("STRING")
                                    .value(deviceMapEntity.getManufacturer())
                                    .build());
        }

        if (deviceMapEntity.getModel() != null) {
            device.getProperties()
                    .add(
                            DeviceList.DeviceProperty.builder()
                                    .name("name")
                                    .type("STRING")
                                    .value(deviceMapEntity.getModel())
                                    .build());
        }

        if (deviceMapEntity.getVersion() != null) {
            device.getProperties()
                    .add(
                            DeviceList.DeviceProperty.builder()
                                    .name("version")
                                    .type("STRING")
                                    .value(deviceMapEntity.getVersion())
                                    .build());
        }

        if (deviceMapEntity.getSt() != null) {
            device.getProperties()
                    .add(
                            DeviceList.DeviceProperty.builder()
                                    .name("state")
                                    .type("DOUBLE")
                                    .value(Integer.toString(deviceMapEntity.getSt()))
                                    .build());
        }

        if (deviceMapEntity.getStateText() != null) {
            device.getProperties()
                    .add(
                            DeviceList.DeviceProperty.builder()
                                    .name("stateText")
                                    .type("STRING")
                                    .value(deviceMapEntity.getStateText())
                                    .build());
        }

        if (deviceMapEntity.getLastHeard() != null) {
            if (deviceType.equals(DeviceType.RGM) && lastHeard != null) {
                deviceMapEntity.setLastHeard(lastHeard);
            }

            device.getProperties()
                    .add(
                            DeviceList.DeviceProperty.builder()
                                    .name("lastUpdate")
                                    .type("DOUBLE")
                                    .value(
                                            Long.toString(
                                                    deviceMapEntity
                                                            .getLastHeard()
                                                            .toLocalDateTime()
                                                            .atZone(ZoneId.of("UTC"))
                                                            .toEpochSecond()))
                                    .build());
        }

        // nameplate
        if (deviceMapEntity.getNameplate() != null) {

            // device-specific nameplate
            if (deviceType.equals(DeviceType.PVLINK)) {
                try {
                    HashMap<String, Double> map =
                            objectMapper.readValue(deviceMapEntity.getNameplate(), typeRef);

                    if (map.get("a_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("artg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("a_rtg")))
                                                .build());
                    }

                    if (map.get("w_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("wrtg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("w_rtg")))
                                                .build());
                    }

                } catch (Exception e) {
                    log.error("Fail to get Nameplate", e);
                }
            }
            if (deviceType.equals(DeviceType.INVERTER)) {
                try {
                    HashMap<String, Double> map =
                            objectMapper.readValue(deviceMapEntity.getNameplate(), typeRef);

                    if (map.get("a_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("artg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("a_rtg")))
                                                .build());
                    }

                    if (map.get("w_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("wrtg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("w_rtg")))
                                                .build());
                    }

                    if (map.get("va_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("vaRtg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("va_rtg")))
                                                .build());
                    }

                } catch (Exception e) {
                    log.error("Fail to get Nameplate", e);
                }
            }
            if (deviceType.equals(DeviceType.BATTERY)) {
                try {
                    HashMap<String, Double> map =
                            objectMapper.readValue(deviceMapEntity.getNameplate(), typeRef);

                    if (map.get("ah_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("ahRtg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("ah_rtg")))
                                                .build());
                    }

                    if (map.get("wh_rtg") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("whRtg")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("wh_rtg")))
                                                .build());
                    }

                    if (map.get("w_cha_max") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("wchaMax")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("w_cha_max")))
                                                .build());
                    }

                    if (map.get("w_discha_max") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("wdischaMax")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("w_discha_max")))
                                                .build());
                    }

                    if (map.get("soc_max") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("socMax")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("soc_max")))
                                                .build());
                    }

                    if (map.get("soc_min") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("socMin")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("soc_min")))
                                                .build());
                    }

                    if (map.get("soc_rsv_max") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("socRsvMax")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("soc_rsv_max")))
                                                .build());
                    }

                    if (map.get("soc_rsv_min") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("socRsvMin")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("soc_rsv_min")))
                                                .build());
                    }

                    if (map.get("a_cha_max") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("achaMax")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("a_cha_max")))
                                                .build());
                    }

                    if (map.get("a_discha_max") != null) {
                        device.getProperties()
                                .add(
                                        DeviceList.DeviceProperty.builder()
                                                .name("adischaMax")
                                                .type("DOUBLE")
                                                .value(Double.toString(map.get("a_discha_max")))
                                                .build());
                    }

                } catch (Exception e) {
                    log.error("Fail to get Nameplate", e);
                }
            }
        }

        // loadcontroller-specific
        if (deviceType.equals(DeviceType.LOAD_CONTROLLER)) {
            String id = deviceMapEntity.getDeviceId();
            if (id.startsWith(loadControllerPrefix)) {
                device.setDeviceId(id.substring(loadControllerPrefix.length()));
            }
        }

        // inverter-specific
        if (deviceType.equals(DeviceType.INVERTER)) {
            if (deviceMapEntity.getSerialNumber() != null) {
                device.getProperties()
                        .add(
                                DeviceList.DeviceProperty.builder()
                                        .name("serialNumber")
                                        .type("STRING")
                                        .value(deviceMapEntity.getSerialNumber())
                                        .build());
            }
        }
    }

    DeviceList.Device entityToDevice(
            DeviceMapInterface deviceMapEntity, DeviceType deviceType, Timestamp lastHeard);
}
