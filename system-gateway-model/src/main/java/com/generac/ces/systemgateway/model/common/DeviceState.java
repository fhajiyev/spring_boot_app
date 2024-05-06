package com.generac.ces.systemgateway.model.common;

import lombok.Getter;

@Getter
public enum DeviceState {
    STATE_UNSPECIFIED(0, null),
    STATE_DISABLED(1, false),
    STATE_ENABLED(2, true),
    UNRECOGNIZED(-1, null);

    private final int stateVal;
    private final Boolean stateBool;

    DeviceState(int stateVal, Boolean stateBool) {
        this.stateVal = stateVal;
        this.stateBool = stateBool;
    }

    public static DeviceState fromBoolean(Boolean requestedState) {
        for (DeviceState deviceState : values()) {
            if (deviceState.stateBool != null && deviceState.stateBool.equals(requestedState)) {
                return deviceState;
            }
        }
        throw new IllegalArgumentException("Corresponding enum not found for provided state.");
    }

    public static DeviceState fromValue(Integer requestedVal) {
        for (DeviceState deviceState : values()) {
            if (deviceState.stateVal == requestedVal) {
                return deviceState;
            }
        }
        throw new IllegalArgumentException("Corresponding enum not found for provided state.");
    }

    public static double toCacheableDouble(boolean stateBool) {
        return (stateBool) ? 1.0 : 0.0;
    }
}
