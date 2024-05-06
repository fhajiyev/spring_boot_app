package com.generac.ces.systemgateway.validator;

import java.lang.annotation.*;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ValidSubscriptionDurationValidator.class)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSubscriptionDuration {
    String message() default "Invalid Subscription Request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
