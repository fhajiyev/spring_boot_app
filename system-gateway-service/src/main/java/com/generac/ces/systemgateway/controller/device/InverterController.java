package com.generac.ces.systemgateway.controller.device;

import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.RequestType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.device.DeviceSettingUpdateResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingsResponse;
import com.generac.ces.systemgateway.model.device.DeviceSettingsUpdateRequest;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.generac.ces.systemgateway.model.helpers.ValidDeviceSettingChange;
import com.generac.ces.systemgateway.service.system.DeviceSettingsService;
import com.generac.ces.systemgateway.service.system.EssSystemService;
import com.generac.ces.systemgateway.validator.ValidSystemType;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Log4j2
@Validated
@RestController
@RequestMapping(value = "/v1")
public class InverterController {
    @Autowired private EssSystemService systemService;
    @Autowired private DeviceSettingsService deviceSettingsService;

    @GetMapping(value = {"/systems/{systemId}/inverters/settings"})
    @ResponseStatus(value = HttpStatus.OK)
    public Mono<DeviceSettingsResponse> getInverterSettings(
            @NotNull @PathVariable("systemId") UUID systemId) {
        return systemService.getInverterSettings(systemId);
    }

    @PatchMapping(value = {"/systems/{systemId}/inverters/settings"})
    @ApiOperation(value = "Set inverter settings for the provided system")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DeviceSettingUpdateResponse
                                                                        .class))),
                @ApiResponse(responseCode = "404", description = "System not found by systemId"),
                @ApiResponse(responseCode = "429", description = "Too Many Requests"),
                @ApiResponse(responseCode = "500", description = "Internal Server Error"),
                @ApiResponse(responseCode = "503", description = "Service Unavailable"),
                @ApiResponse(responseCode = "504", description = "Gateway Timeout")
            })
    public Mono<DeviceSettingUpdateResponse> setInverterSettings(
            @NotNull @PathVariable("systemId") UUID systemId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME)
                    String callerId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME)
                    String userId,
            @ValidDeviceSettingChange(
                            allowedSettings = {
                                DeviceSetting.ISLANDING,
                                DeviceSetting.EXPORT_OVERRIDE,
                                DeviceSetting.NUMBER_OF_TRANSFER_SWITCHES,
                                DeviceSetting.LOAD_SHEDDING,
                                DeviceSetting.CT_CALIBRATION,
                                DeviceSetting.EXPORT_LIMITING,
                                DeviceSetting.GENERATOR_CONTROL_MODE,
                                DeviceSetting.EXPORT_POWER_LIMIT
                            })
                    @RequestBody
                    DeviceSettingsUpdateRequest deviceSettingsUpdateRequest,
            @NotNull @ValidSystemType
                    @RequestParam(value = "systemType", required = false, defaultValue = "ESS")
                    SystemType systemType) {
        return deviceSettingsService.setDeviceSettings(
                systemId,
                deviceSettingsUpdateRequest,
                callerId,
                userId,
                null,
                RequestType.INSTANT_INVERTER_SETTINGS_PATCH,
                systemType);
    }
}
