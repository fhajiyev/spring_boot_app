package com.generac.ces.systemgateway.model.common;

public enum EventStatus {
    UNKNOWN_STATE(0),
    SCHEDULED(1),
    RUNNING(2),
    SHADOWED(3),
    COMPLETE(4),
    CANCELLED(5),
    UNRECOGNIZED(-1);

    private final int statusVal;

    EventStatus(int statusVal) {
        this.statusVal = statusVal;
    }

    public static EventStatus fromValue(Integer requestedVal) {
        for (EventStatus eventStatus : values()) {
            if (eventStatus.statusVal == requestedVal) {
                return eventStatus;
            }
        }
        throw new IllegalArgumentException("Corresponding enum not found for provided status.");
    }
}
