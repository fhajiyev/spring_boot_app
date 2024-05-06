package com.generac.ces.systemgateway.controller.subscription;

import com.generac.ces.systemgateway.model.CallerMetadataRequestHeaders;
import com.generac.ces.systemgateway.model.Quota;
import com.generac.ces.systemgateway.model.SubscriptionRequest;
import com.generac.ces.systemgateway.model.SubscriptionResponse;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import com.generac.ces.systemgateway.service.subscription.SubscriptionService;
import com.generac.ces.systemgateway.validator.ValidSubscriptionDuration;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Log4j2
@Validated
@RestController
@RequestMapping(value = "/v1")
public class SubscriptionController {

    @Autowired private SubscriptionService subscriptionService;

    @PostMapping(value = {"/subscriptions"})
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public List<SubscriptionResponse> createSubscription(
            @Valid @ValidSubscriptionDuration @RequestBody SubscriptionRequest dto,
            @Valid
                    @NotEmpty
                    @RequestHeader(
                            CallerMetadataRequestHeaders.Constants.COGNITO_CLIENT_ID_HEADER_NAME)
                    String clientId) {

        return subscriptionService.createSubscription(dto, clientId);
    }

    @GetMapping(value = {"/quota"})
    public Quota getRemainingQuotaInSeconds(
            @Valid @NotNull @RequestParam("subscriberType") SubscriberType subscriberType,
            @Valid @NotNull @RequestParam("systemType") SystemType systemType,
            @Valid @NotNull @RequestParam("resourceType") ResourceType resourceType,
            @Valid @NotNull @RequestParam("systemId") String systemId) {

        return subscriptionService.getRemainingQuotaInSeconds(
                subscriberType, systemType, resourceType, systemId);
    }
}
