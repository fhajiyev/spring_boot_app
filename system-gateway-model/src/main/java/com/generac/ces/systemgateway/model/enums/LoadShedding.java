package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum LoadShedding implements Valuable {
    LOAD_SHEDDING_DISABLED(0),
    LOAD_SHEDDING_SMM_ONLY(1),
    LOAD_SHEDDING_ATS_AND_SMM(2),
    UNRECOGNIZED(-1);

    private final int value;

    LoadShedding(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static LoadShedding fromValue(int value) {
        for (LoadShedding shedding : values()) {
            if (shedding.value == value) {
                return shedding;
            }
        }
        return UNRECOGNIZED;
    }

    public static LoadShedding fromEnum(
            InverterSettingControlOuterClass.InverterSetting.LoadShedding loadSheddingEnum) {
        for (LoadShedding shedding : values()) {
            if (Objects.equals(shedding.name(), loadSheddingEnum.name())) {
                return shedding;
            }
        }
        return UNRECOGNIZED;
    }
}
