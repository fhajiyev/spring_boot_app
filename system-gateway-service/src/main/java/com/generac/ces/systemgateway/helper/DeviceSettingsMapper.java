package com.generac.ces.systemgateway.helper;

import com.generac.ces.essdataprovider.model.DeviceSettings;
import com.generac.ces.essdataprovider.model.DeviceSettingsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceSettingsMapper {
    @Mapping(source = "deviceSettingsResponse.hostRcpn", target = "hostDeviceId")
    com.generac.ces.systemgateway.model.device.DeviceSettingsResponse
            mapDeviceSettingsEssDpToDeviceSettings(
                    com.generac.ces.essdataprovider.model.DeviceSettingsResponse
                            deviceSettingsResponse);

    @Mapping(source = "metadata", target = "settings")
    com.generac.ces.systemgateway.model.device.DeviceSettings mapDeviceSettingsEssDpToDeviceSetting(
            DeviceSettings deviceSettings);

    @Mapping(source = "deviceSettingsResponse.hostDeviceId", target = "hostRcpn")
    DeviceSettingsResponse mapEssDpDeviceSettingsResponseToDeviceSettingsResponse(
            com.generac.ces.systemgateway.model.device.DeviceSettingsResponse
                    deviceSettingsResponse);

    @Mapping(source = "settings", target = "metadata")
    DeviceSettings mapEssDpDeviceSettingsToDeviceSetting(
            com.generac.ces.systemgateway.model.device.DeviceSettings deviceSettings);
}
