package com.generac.ces.systemgateway.controller.setpoint;

import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.ExportLimitRequest;
import com.generac.ces.systemgateway.model.OdinResponse;
import com.generac.ces.systemgateway.service.odin.OdinService;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Log4j2
@Validated
@RestController
@RequestMapping(value = "/v1")
public class SetpointController {

    @Autowired private OdinService odinService;

    @PatchMapping(value = {"/systems/{systemId}/export-limit"})
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public Mono<OdinResponse> setExportLimit(
            @NotNull @PathVariable("systemId") UUID systemId,
            @RequestHeader(CallerMetadataRequestHeaders.Constants.ODIN_CALLER_ID_HEADER_NAME)
                    String callerId,
            @Valid @RequestBody ExportLimitRequest dto) {
        return odinService.setExportLimit(systemId.toString(), dto, callerId);
    }
}
