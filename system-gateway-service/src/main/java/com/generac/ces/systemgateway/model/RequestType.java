package com.generac.ces.systemgateway.model;

public enum RequestType {
    INSTANT_SYSTEM_MODE_PATCH,
    INSTANT_ACTIVE_SYSTEM_MODE_PATCH,
    INSTANT_DEVICE_STATE_PATCH,
    INSTANT_BATTERY_SETTINGS_PATCH,
    INSTANT_INVERTER_SETTINGS_PATCH,
    INSTANT_PVLINK_SETTINGS_PATCH;

    public String combineWithSystemId(String systemId) {
        return systemId + "_" + this.toString();
    }
}
