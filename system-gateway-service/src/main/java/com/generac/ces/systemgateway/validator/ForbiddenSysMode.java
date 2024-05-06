package com.generac.ces.systemgateway.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ForbiddenSysModeValidator.class)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ForbiddenSysMode {
    String message() default "Forbidden SysMode";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
