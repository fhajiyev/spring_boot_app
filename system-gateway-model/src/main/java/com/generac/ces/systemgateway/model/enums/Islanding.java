package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum Islanding implements Valuable {
    ISLANDING_DISABLED(0),
    ISLANDING_ENABLED(1),
    UNRECOGNIZED(-1);

    private final int value;

    Islanding(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static Islanding fromValue(int value) {
        for (Islanding islanding : values()) {
            if (islanding.value == value) {
                return islanding;
            }
        }
        return UNRECOGNIZED;
    }

    public static Islanding fromEnum(
            InverterSettingControlOuterClass.InverterSetting.Islanding islandingproto) {
        for (Islanding islanding : values()) {
            if (Objects.equals(islanding.name(), islandingproto.name())) {
                return islanding;
            }
        }
        return UNRECOGNIZED;
    }
}
