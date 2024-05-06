package com.generac.ces.systemgateway.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.essdataprovider.model.BaseDeviceModel;
import com.generac.ces.systemgateway.entity.device.DeviceMapInterface;
import org.mapstruct.Context;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingInheritanceStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG)
public interface DeviceMapDtoMapperConfig {

    DeviceMapDtoMapperConfig INSTANCE = Mappers.getMapper(DeviceMapDtoMapperConfig.class);

    @Mapping(target = "id", source = "deviceId")
    @Mapping(target = "manufacturer", source = "manufacturer")
    @Mapping(target = "name", source = "model")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "state", source = "st")
    @Mapping(target = "stateText", source = "stateText")
    @Mapping(target = "firstUpdate", source = "firstUpdate", qualifiedByName = "timeStampToLong")
    @Mapping(target = "lastUpdate", source = "lastHeard", qualifiedByName = "timeStampToLong")
    BaseDeviceModel entityToDevice(
            DeviceMapInterface deviceMapEntity, @Context ObjectMapper objectMapper);
}
