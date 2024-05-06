package com.generac.ces.systemgateway.validator;

import com.generac.ces.systemgateway.model.common.InvalidSysModes;
import com.generac.ces.systemgateway.model.system.ActiveSystemModeUpdateRequest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class InvalidActiveSystemModeValidator
        implements ConstraintValidator<InvalidActiveSysMode, ActiveSystemModeUpdateRequest> {

    @Override
    public boolean isValid(
            ActiveSystemModeUpdateRequest request, ConstraintValidatorContext context) {
        List<String> invalidModes =
                Stream.of(InvalidSysModes.values()).map(InvalidSysModes::name).toList();
        List<String> requestModes = request.activeModes().stream().map(Objects::toString).toList();
        return Collections.disjoint(requestModes, invalidModes);
    }
}
