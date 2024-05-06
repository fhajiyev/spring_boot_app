package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum CTCalibration implements Valuable {
    CT_CALIBRATION_AUTO(0),
    CT_CALIBRATION_TRIGGER(1),
    UNRECOGNIZED(-1);

    private final int value;

    CTCalibration(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static CTCalibration fromValue(int value) {
        for (CTCalibration ctCalibration : values()) {
            if (ctCalibration.value == value) {
                return ctCalibration;
            }
        }
        return UNRECOGNIZED;
    }

    public static CTCalibration fromEnum(
            InverterSettingControlOuterClass.InverterSetting.CTCalibration ctcalibrationproto) {
        for (CTCalibration ctCalibration : values()) {
            if (ctcalibrationproto != null
                    && ctCalibration.name() != null
                    && Objects.equals(ctCalibration.name(), ctcalibrationproto.name())) {
                return ctCalibration;
            }
        }
        return UNRECOGNIZED;
    }
}
