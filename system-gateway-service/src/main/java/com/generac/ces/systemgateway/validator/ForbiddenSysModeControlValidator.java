package com.generac.ces.systemgateway.validator;

import com.generac.ces.systemgateway.model.common.InvalidSysModes;
import com.generac.ces.systemgateway.model.system.SystemModeUpdateRequest;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ForbiddenSysModeControlValidator
        implements ConstraintValidator<ForbiddenSysModeControl, SystemModeUpdateRequest> {
    @Override
    public boolean isValid(
            SystemModeUpdateRequest systemModeUpdateRequest,
            ConstraintValidatorContext constraintValidatorContext) {
        List<String> invalidModes =
                Stream.of(InvalidSysModes.values()).map(InvalidSysModes::name).toList();
        return !invalidModes.contains(
                systemModeUpdateRequest.systemMode().mode.name().toUpperCase());
    }
}
