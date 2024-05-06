package com.generac.ces.systemgateway.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.entity.device.DeviceMapInterface;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.enums.DeviceType;
import org.mapstruct.Context;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingInheritanceStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG)
public interface DeviceListDtoMapperConfig {

    DeviceListDtoMapperConfig INSTANCE = Mappers.getMapper(DeviceListDtoMapperConfig.class);

    DeviceList.Device entityToDevice(
            DeviceMapInterface deviceMapEntity,
            @Context ObjectMapper objectMapper,
            DeviceType deviceType);
}
