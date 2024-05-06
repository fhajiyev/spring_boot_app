package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum GeneratorControlMode implements Valuable {
    GENERATOR_CONTROL_MODE_SINGLE_TRANSFER(0),
    GENERATOR_CONTROL_MODE_SOURCE_CYCLING(1),
    GENERATOR_CONTROL_MODE_ALWAYS_ON(2),
    UNRECOGNIZED(-1);

    private final int value;

    GeneratorControlMode(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static GeneratorControlMode fromValue(int value) {
        for (GeneratorControlMode generatorControlMode : values()) {
            if (generatorControlMode.value == value) {
                return generatorControlMode;
            }
        }
        return UNRECOGNIZED;
    }

    public static GeneratorControlMode fromEnum(
            InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode
                    generatorControlModeProto) {
        for (GeneratorControlMode generatorControlMode : values()) {
            if (Objects.equals(generatorControlMode.name(), generatorControlModeProto.name())) {
                return generatorControlMode;
            }
        }
        return UNRECOGNIZED;
    }
}
