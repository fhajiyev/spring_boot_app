package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.PvLinkControl;
import java.util.Objects;

public enum NumberOfSubstrings implements Valuable {
    NUMBER_OF_SUBSTRINGS_UNSPECIFIED(0),
    NUMBER_OF_SUBSTRINGS_ONE(1),
    NUMBER_OF_SUBSTRINGS_TWO(2),
    UNRECOGNIZED(-1);

    private final int value;

    NumberOfSubstrings(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static NumberOfSubstrings fromValue(int value) {
        for (NumberOfSubstrings substrings : values()) {
            if (substrings.value == value) {
                return substrings;
            }
        }
        return UNRECOGNIZED;
    }

    public static NumberOfSubstrings fromEnum(
            PvLinkControl.PVLinkSetting.NumberOfSubstrings substringsProto) {
        for (NumberOfSubstrings substrings : values()) {
            if (Objects.equals(substrings.name(), substringsProto.name())) {
                return substrings;
            }
        }
        return UNRECOGNIZED;
    }
}
