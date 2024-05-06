package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import java.util.Objects;

public enum ExportOverride implements Valuable {
    EXPORT_OVERRIDE_DISABLED(0),
    EXPORT_OVERRIDE_ENABLED(1),
    UNRECOGNIZED(-1);

    private final int value;

    ExportOverride(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static ExportOverride fromValue(int value) {
        for (ExportOverride exportOverride : values()) {
            if (exportOverride.value == value) {
                return exportOverride;
            }
        }
        return UNRECOGNIZED;
    }

    public static ExportOverride fromName(
            InverterSettingControlOuterClass.InverterSetting.ExportOverride exportOverrideEnum) {
        for (ExportOverride exportOverride : values()) {
            if (Objects.equals(exportOverride.name(), exportOverrideEnum.name())) {
                return exportOverride;
            }
        }
        return UNRECOGNIZED;
    }
}
