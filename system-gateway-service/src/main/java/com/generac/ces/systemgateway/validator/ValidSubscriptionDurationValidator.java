package com.generac.ces.systemgateway.validator;

import com.generac.ces.systemgateway.configuration.RateLimitConfiguration;
import com.generac.ces.systemgateway.model.SubscriptionRequest;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidSubscriptionDurationValidator
        implements ConstraintValidator<ValidSubscriptionDuration, SubscriptionRequest> {

    @Override
    public boolean isValid(
            SubscriptionRequest subscriptionRequest,
            ConstraintValidatorContext constraintValidatorContext) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        for (SubscriptionRequest.System system : subscriptionRequest.getSystems()) {
            String key =
                    subscriptionRequest.getSubscriberType().name()
                            + "-"
                            + system.getSystemType().name()
                            + "-"
                            + subscriptionRequest.getResourceType().name();
            // try to find this key in max duration map
            Integer maxDuration = RateLimitConfiguration.maxDurationMap.get(key);
            if (maxDuration == null) {
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(
                                "Unknown combination of subscriber type: %s | system type: %s | resource type %s."
                                        .formatted(
                                                subscriptionRequest.getSubscriberType().name(),
                                                system.getSystemType().name(),
                                                subscriptionRequest.getResourceType().name()))
                        .addConstraintViolation();
                return false;
            }
            // compare duration in request with max duration
            if (subscriptionRequest.getDurationSeconds() <= 0
                    || subscriptionRequest.getDurationSeconds() > maxDuration) {
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(
                                "Subscription duration should be positive and do not exceed "
                                        + maxDuration
                                        + " seconds.")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
