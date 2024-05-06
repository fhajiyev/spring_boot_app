package com.generac.ces.systemgateway.controller.device;

import com.generac.ces.essdataprovider.model.DeviceMapRequestDto;
import com.generac.ces.essdataprovider.model.DeviceMapResponseDto;
import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.DeviceStateRequest;
import com.generac.ces.systemgateway.model.DeviceStateResponse;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.device.DeviceListRequest;
import com.generac.ces.systemgateway.service.system.DeviceSettingsService;
import com.generac.ces.systemgateway.service.system.DevicesService;
import com.generac.ces.systemgateway.validator.ValidSystemType;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Log4j2
@Validated
@RestController
@RequestMapping(value = "/v1")
public class DeviceController {

    @Autowired private DeviceSettingsService deviceSettingsService;
    @Autowired private DevicesService devicesService;

    @PatchMapping(value = {"/systems/{systemId}/devices/{deviceRcpn}/state"})
    public Mono<DeviceStateResponse> setDeviceState(
            @NotNull @PathVariable("systemId") UUID systemId,
            @NotNull @PathVariable("deviceRcpn") String deviceRcpn,
            @RequestParam(value = "instantControl", defaultValue = "true") Boolean instantControl,
            @Valid @RequestBody DeviceStateRequest deviceStateRequest,
            @NotNull @ValidSystemType
                    @RequestParam(value = "systemType", required = false, defaultValue = "ESS")
                    SystemType systemType,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME)
                    String callerId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME)
                    String userId) {
        return deviceSettingsService.setDeviceState(
                systemId, deviceRcpn, callerId, userId, deviceStateRequest, systemType);
    }

    @GetMapping(value = {"/systems/{systemId}/devices/{deviceRcpn}/state"})
    public Mono<DeviceStateResponse> getDeviceState(
            @NotNull @PathVariable("systemId") UUID systemId,
            @NotNull @PathVariable("deviceRcpn") String deviceRcpn) {
        return deviceSettingsService.getDeviceState(systemId, deviceRcpn);
    }

    @PostMapping(value = {"/devicemap"})
    public List<DeviceMapResponseDto> getBulkDeviceMap(
            @Validated @RequestBody DeviceMapRequestDto requestDto) {
        return devicesService.getBulkDeviceMap(requestDto);
    }

    @PostMapping(value = {"/devicelist"})
    public List<DeviceList> getBulkDeviceList(
            @Validated @RequestBody DeviceListRequest requestDto) {
        return devicesService.getBulkDeviceList(requestDto);
    }
}
