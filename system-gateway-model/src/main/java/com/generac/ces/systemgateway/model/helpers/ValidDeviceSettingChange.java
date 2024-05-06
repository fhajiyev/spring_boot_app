package com.generac.ces.systemgateway.model.helpers;

import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDeviceSettingValidator.class)
public @interface ValidDeviceSettingChange {
    String message() default "Invalid DeviceSetting";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    DeviceSetting[] allowedSettings() default {};
}
