package com.generac.ces.systemgateway.controller.system;

import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.system.SystemFirmwareUpdateRequest;
import com.generac.ces.systemgateway.model.system.SystemFirmwareUpdateResponse;
import com.generac.ces.systemgateway.service.system.BlueMarlinFirmwareService;
import com.generac.ces.systemgateway.service.system.SystemFirmwareUpdateRequestMetadata;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "System Firmware controller",
        description =
                "Contains endpoints having to do with firmware and firmware updates for system"
                    + " types which support applying those firmware updates at the system level.")
@RestController
@Validated
@RequestMapping("/v1/systems/{systemId}/firmware")
public class SystemFirmwareController {
    private final BlueMarlinFirmwareService blueMarlinFirmwareService;

    public SystemFirmwareController(BlueMarlinFirmwareService blueMarlinFirmwareService) {
        this.blueMarlinFirmwareService = blueMarlinFirmwareService;
    }

    @PatchMapping
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public SystemFirmwareUpdateResponse updateSystemFirmwareDetails(
            @PathVariable UUID systemId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.USER_ID_HEADER_NAME) UUID userId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.APPLICATION_NAME_HEADER_NAME)
                    String applicationName,
            @RequestBody @Valid SystemFirmwareUpdateRequest updateRequest) {
        /* Comment out call to validate the received firmware version until endpoint to support the validation exists.
                    blueMarlinFirmwareService.throwIfFirmwareVersionIsInvalidForSystem(
                        systemId, updateRequest.updateVersion());
        */

        var requestMetadata = new SystemFirmwareUpdateRequestMetadata(userId, applicationName);
        return blueMarlinFirmwareService.updateSystemFirmwareVersion(
                systemId, updateRequest.updateVersion(), updateRequest.notes(), requestMetadata);
    }
}
