package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum NumberOfTransferSwitches implements Valuable {
    NUMBER_OF_TRANSFER_SWITCHES_ZERO(0),
    NUMBER_OF_TRANSFER_SWITCHES_ONE(1),
    NUMBER_OF_TRANSFER_SWITCHES_TWO(2),
    UNRECOGNIZED(-1);

    private final int value;

    NumberOfTransferSwitches(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static NumberOfTransferSwitches fromValue(int value) {
        for (NumberOfTransferSwitches switches : values()) {
            if (switches.value == value) {
                return switches;
            }
        }
        return UNRECOGNIZED;
    }

    public static NumberOfTransferSwitches fromEnum(
            InverterSettingControlOuterClass.InverterSetting.NumberOfTransferSwitches
                    numberOfTransferSwitchesEnum) {
        for (NumberOfTransferSwitches switches : values()) {
            if (Objects.equals(switches.toString(), numberOfTransferSwitchesEnum.name())) {
                return switches;
            }
        }
        return UNRECOGNIZED;
    }
}
