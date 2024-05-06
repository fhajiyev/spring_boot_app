package com.generac.ces.systemgateway.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.essdataprovider.model.BaseDeviceModel;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.systemgateway.entity.device.DeviceMapInterface;
import java.util.HashMap;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
        config = DeviceMapDtoMapperConfig.class,
        uses = Formatter.class,
        builder = @Builder(disableBuilder = true))
public interface DeviceMapDtoMapper {

    DeviceMapDtoMapper INSTANCE = Mappers.getMapper(DeviceMapDtoMapper.class);
    Logger log = LoggerFactory.getLogger(DeviceMapDtoMapper.class);
    TypeReference<HashMap<String, Double>> typeRef =
            new TypeReference<HashMap<String, Double>>() {};
    ObjectMapper objectMapper = new ObjectMapper();
    String loadControllerPrefix = "0x2000";

    @AfterMapping
    default void after(DeviceMapInterface deviceMapEntity, @MappingTarget BaseDeviceModel device) {
        if (deviceMapEntity.getNameplate() != null) {
            if (device instanceof DeviceMapResponseDto.DeviceMapResponseDtoPvls) {
                try {
                    HashMap<String, Double> map =
                            objectMapper.readValue(deviceMapEntity.getNameplate(), typeRef);
                    ((DeviceMapResponseDto.DeviceMapResponseDtoPvls) device)
                            .setArtg(map.get("a_rtg"));
                    ((DeviceMapResponseDto.DeviceMapResponseDtoPvls) device)
                            .setWrtg(map.get("w_rtg"));
                } catch (Exception e) {
                    log.error("Fail to get Nameplate", e);
                }
            }
            if (device instanceof DeviceMapResponseDto.DeviceMapResponseDtoInverter) {
                ((DeviceMapResponseDto.DeviceMapResponseDtoInverter) device)
                        .setSerialNumber(deviceMapEntity.getSerialNumber());
                try {
                    HashMap<String, Double> map =
                            objectMapper.readValue(deviceMapEntity.getNameplate(), typeRef);
                    ((DeviceMapResponseDto.DeviceMapResponseDtoInverter) device)
                            .setArtg(map.get("a_rtg"));
                    ((DeviceMapResponseDto.DeviceMapResponseDtoInverter) device)
                            .setWrtg(map.get("w_rtg"));
                    ((DeviceMapResponseDto.DeviceMapResponseDtoInverter) device)
                            .setVaRtg(map.get("va_rtg"));
                } catch (Exception e) {
                    log.error("Fail to get Nameplate", e);
                }
            }
            if (device instanceof DeviceMapResponseDto.DeviceMapResponseDtoBattery) {
                try {
                    DeviceMapResponseDto.DeviceMapResponseDtoBattery battery =
                            (DeviceMapResponseDto.DeviceMapResponseDtoBattery) device;
                    HashMap<String, Double> map =
                            objectMapper.readValue(deviceMapEntity.getNameplate(), typeRef);
                    battery.setAhRtg(map.get("ah_rtg"));
                    battery.setWhRtg(map.get("wh_rtg"));
                    battery.setWchaMax(map.get("w_cha_max"));
                    battery.setWdischaMax(map.get("w_discha_max"));
                    battery.setSocMax(map.get("soc_max"));
                    battery.setSocMin(map.get("soc_min"));
                    battery.setSocRsvMax(map.get("soc_rsv_max"));
                    battery.setSocRsvMin(map.get("soc_rsv_min"));
                    battery.setAchaMax(map.get("a_cha_max"));
                    battery.setAdischaMax(map.get("a_discha_max"));
                } catch (Exception e) {
                    log.error("Fail to get Nameplate", e);
                }
            }
        }
        if (device instanceof DeviceMapResponseDto.DeviceMapResponseDtoLoadController) {
            try {
                DeviceMapResponseDto.DeviceMapResponseDtoLoadController loadController =
                        (DeviceMapResponseDto.DeviceMapResponseDtoLoadController) device;
                String id = loadController.getId();
                if (id.startsWith(loadControllerPrefix)) {
                    loadController.setId(id.substring(loadControllerPrefix.length()));
                }
            } catch (Exception e) {
                log.error("Fail to get id", e);
            }
        }
    }

    DeviceMapResponseDto.DeviceMapResponseDtoPvls entityToPvls(DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoInverter entityToInverter(
            DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoBattery entityToBattery(
            DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoBeacon entityToBeacon(
            DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoIcm entityToIcm(DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoRgm entityToRgm(DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoGenerator entityToGenerator(
            DeviceMapInterface deviceMapEntity);

    DeviceMapResponseDto.DeviceMapResponseDtoLoadController entityToLoadController(
            DeviceMapInterface deviceMapEntity);
}
