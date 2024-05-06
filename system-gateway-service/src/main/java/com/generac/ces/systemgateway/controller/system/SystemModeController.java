package com.generac.ces.systemgateway.controller.system;

import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateResponse;
import com.generac.ces.systemgateway.model.system.SystemModeGetResponse;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateResponse;
import com.generac.ces.systemgateway.service.system.SystemModeService;
import com.generac.ces.systemgateway.validator.ForbiddenSysModeControl;
import com.generac.ces.systemgateway.validator.InvalidActiveSysMode;
import com.generac.ces.systemgateway.validator.ValidSystemType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class SystemModeController {

    @Autowired private SystemModeService systemModeService;

    @Operation(
            summary = "Update current system mode.",
            description =
                    "Submits system mode sub-controller settings change event for system mode.")
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
                                                                SystemModeUpdateResponse.class))),
                @ApiResponse(
                        responseCode = "202",
                        description = "Accepted",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                SystemModeUpdateResponse.class))),
                @ApiResponse(responseCode = "429", description = "Too Many Requests"),
                @ApiResponse(responseCode = "500", description = "Internal Server Error"),
                @ApiResponse(responseCode = "503", description = "Service Unavailable"),
                @ApiResponse(responseCode = "504", description = "Gateway Timeout")
            })
    @PatchMapping(value = {"/systems/{systemId}/mode"})
    public Mono<ResponseEntity<SystemModeUpdateResponse>> patchSystemMode(
            @NotNull @PathVariable("systemId") UUID systemId,
            @RequestParam(value = "instantControl", defaultValue = "true") Boolean instantControl,
            @NotNull @ValidSystemType
                    @RequestParam(value = "systemType", required = false, defaultValue = "ESS")
                    SystemType systemType,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME)
                    String callerId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME)
                    String userId,
            @NotNull @Valid @RequestBody @ForbiddenSysModeControl SystemModeUpdateRequest request) {
        // todo add validation for scheduled request. (Handled in odin-ms but should handle here as
        // well)
        return systemModeService.updateSystemMode(
                systemType, systemId, request, callerId, userId, instantControl);
    }

    @Operation(
            summary = "Get current & active system modes.",
            description = "Returns current and active system modes as per telemetry.")
    @GetMapping(value = {"/systems/{systemId}/mode"})
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
                                                                SystemModeGetResponse.class))),
                @ApiResponse(responseCode = "404", description = "System not found by systemId"),
                @ApiResponse(responseCode = "500", description = "Internal Server Error"),
                @ApiResponse(responseCode = "503", description = "Service Unavailable"),
                @ApiResponse(responseCode = "504", description = "Gateway Timeout")
            })
    public Mono<SystemModeGetResponse> getSystemMode(
            @NotNull @PathVariable("systemId") UUID systemId) {
        return systemModeService.getSystemMode(systemId);
    }

    @Operation(
            summary = "Update active system modes.",
            description = "Submits legacy controller event settings change for inverter settings.")
    @PatchMapping(value = {"/systems/{systemId}/active-modes"})
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
                                                                ActiveSystemModeUpdateResponse
                                                                        .class))),
                @ApiResponse(responseCode = "429", description = "Too Many Requests"),
                @ApiResponse(responseCode = "500", description = "Internal Server Error"),
                @ApiResponse(responseCode = "503", description = "Service Unavailable"),
                @ApiResponse(responseCode = "504", description = "Gateway Timeout")
            })
    public Mono<ActiveSystemModeUpdateResponse> setActiveSystemModes(
            @NotNull @PathVariable("systemId") UUID systemId,
            @NotNull @ValidSystemType
                    @RequestParam(value = "systemType", required = false, defaultValue = "ESS")
                    SystemType systemType,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.CALLER_ID_HEADER_NAME)
                    String callerId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME)
                    String userId,
            @Valid @NotNull @RequestBody @InvalidActiveSysMode
                    ActiveSystemModeUpdateRequest activeSystemModeUpdateRequest) {
        return systemModeService.setActiveSystemModes(
                systemId, activeSystemModeUpdateRequest, systemType, callerId, userId);
    }
}
