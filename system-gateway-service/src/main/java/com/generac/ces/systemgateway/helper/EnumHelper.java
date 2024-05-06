package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.model.enums.Valuable;

public final class EnumHelper {

    private EnumHelper() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static <T extends Enum<T>> T fromValueForValuable(Class<T> enumClass, int value) {
        for (T enumConstant : enumClass.getEnumConstants()) {
            if (((Valuable) enumConstant).getValue() == value) {
                return enumConstant;
            }
        }
        throw new BadRequestException(
                "No enum constant with value: " + value + " in " + enumClass.getSimpleName());
    }
}
