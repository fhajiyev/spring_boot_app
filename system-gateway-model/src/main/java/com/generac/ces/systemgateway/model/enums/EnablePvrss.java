package com.generac.ces.systemgateway.model.enums;

import com.generac.ces.system.control.subcontrol.PvLinkControl;
import java.util.Objects;

public enum EnablePvrss implements Valuable {
    PVRSS_STATE_OFF(0),
    PVRSS_STATE_ON(1),
    UNRECOGNIZED(-1);

    private final int value;

    EnablePvrss(int value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    public static EnablePvrss fromValue(int value) {
        for (EnablePvrss enablePvrss : values()) {
            if (enablePvrss.value == value) {
                return enablePvrss;
            }
        }
        return UNRECOGNIZED;
    }

    public static EnablePvrss fromEnum(PvLinkControl.PVLinkSetting.PVRSSState pvrssStateProto) {
        for (EnablePvrss enablePvrss : values()) {
            if (Objects.equals(enablePvrss.name(), pvrssStateProto.name())) {
                return enablePvrss;
            }
        }
        return UNRECOGNIZED;
    }
}
