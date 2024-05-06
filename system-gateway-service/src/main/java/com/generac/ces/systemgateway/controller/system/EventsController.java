package com.generac.ces.systemgateway.controller.system;

import com.generac.ces.system.control.subcontrol.Subcontrol.SubControlType;
import com.generac.ces.systemgateway.model.EventsListResponse;
import com.generac.ces.systemgateway.model.common.Actor;
import com.generac.ces.systemgateway.service.odin.OdinService;
import com.generac.ces.systemgateway.validator.ValidIso8601DateTimeString;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Log4j2
@Validated
@RestController
@RequestMapping(value = "/v1")
public class EventsController {

    @Autowired private OdinService odinService;

    @Operation(
            summary = "Get a list of system events",
            description =
                    "Get list of events that are (wholly or partially) scheduled between provided"
                            + " start and end timestamps. Returned list will be filtered based upon"
                            + " provided actorIds and subControlTypes.")
    @GetMapping(value = {"/systems/{systemId}/events"})
    public Mono<EventsListResponse> getEventsList(
            @NotNull @PathVariable("systemId")
                    @ApiParam(value = "System Id for which to get list of events.", required = true)
                    UUID systemId,
            @RequestParam(value = "actorIds", required = false, defaultValue = "")
                    @ApiParam(value = "Optional. Filter events by Actors.")
                    List<Actor> actorIds,
            @RequestParam(value = "subControlTypes", required = false)
                    @ApiParam(
                            value =
                                    "If sub control types are not provided, all supported types"
                                            + " will be used.")
                    List<SubControlType> subControlTypes,
            @RequestParam
                    @ApiParam(
                            value = "Start timestamp (inclusive) to list events.",
                            required = true)
                    @ValidIso8601DateTimeString(message = "Invalid start value")
                    String start,
            @RequestParam(required = false, defaultValue = "")
                    @ApiParam(
                            value =
                                    "End timestamp (exclusive) to list events. If not provided, the"
                                            + " current time will be assumed.")
                    @ValidIso8601DateTimeString(message = "Invalid end value")
                    String end) {
        return odinService.getEventsList(
                systemId.toString(), actorIds, subControlTypes, start, end);
    }
}
