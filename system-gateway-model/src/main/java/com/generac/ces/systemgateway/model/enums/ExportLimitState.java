package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum ExportLimitState implements Valuable {
    EXPORT_LIMIT_STATE_DISABLED(0),
    EXPORT_LIMIT_STATE_ENABLED(1),
    UNRECOGNIZED(-1);
    private final int value;

    ExportLimitState(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static ExportLimitState fromValue(int value) {
        for (ExportLimitState exportLimitState : values()) {
            if (exportLimitState.value == value) {
                return exportLimitState;
            }
        }
        return UNRECOGNIZED;
    }

    public static ExportLimitState fromEnum(
            InverterSettingControlOuterClass.InverterSetting.ExportLimitState
                    exportLimitStateProto) {
        for (ExportLimitState exportLimitState : values()) {
            if (Objects.equals(exportLimitState.name(), exportLimitStateProto.name())) {
                return exportLimitState;
            }
        }
        return UNRECOGNIZED;
    }
}
