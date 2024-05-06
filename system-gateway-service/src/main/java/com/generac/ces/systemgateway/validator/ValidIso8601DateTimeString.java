package com.generac.ces.systemgateway.validator;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = Iso8601DateTimeStringValidator.class)
public @interface ValidIso8601DateTimeString {
    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
