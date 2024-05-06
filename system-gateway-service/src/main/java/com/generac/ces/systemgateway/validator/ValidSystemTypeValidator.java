package com.generac.ces.systemgateway.validator;

import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.model.common.SystemType;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidSystemTypeValidator implements ConstraintValidator<ValidSystemType, SystemType> {

    @Override
    public boolean isValid(SystemType value, ConstraintValidatorContext context) {
        if (!value.equals(SystemType.ESS)) {
            throw new BadRequestException("Invalid System Type");
        }
        return true;
    }
}
