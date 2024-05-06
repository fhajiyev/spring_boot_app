package com.generac.ces.systemgateway.validator;

import com.generac.ces.systemgateway.model.SystemModeRequest;
import com.generac.ces.systemgateway.model.common.InvalidSysModes;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ForbiddenSysModeValidator
        implements ConstraintValidator<ForbiddenSysMode, SystemModeRequest> {

    @Override
    public boolean isValid(SystemModeRequest value, ConstraintValidatorContext context) {
        List<String> invalidModes =
                Stream.of(InvalidSysModes.values()).map(InvalidSysModes::name).toList();
        return !invalidModes.contains(value.getMode().toString().toUpperCase());
    }
}
