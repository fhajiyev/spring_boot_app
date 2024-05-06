package com.generac.ces.systemgateway.model.common;

public enum CancellationType {
    UNKNOWN_CANCEL_TYPE(0),
    ROOT_ACTOR(1),
    SHADOW_CANCELATION(2),
    SAME_ACTOR_SHADOW(3),
    EXTERNAL(4),
    SAME_ACTOR_CANCELATION(5),
    HIGHER_PRECEDENCE_ACTOR(6),
    UNRECOGNIZED(-1);

    private final int typeVal;

    CancellationType(int typeVal) {
        this.typeVal = typeVal;
    }

    public static CancellationType fromValue(Integer requestedVal) {
        for (CancellationType cancellationType : values()) {
            if (cancellationType.typeVal == requestedVal) {
                return cancellationType;
            }
        }
        throw new IllegalArgumentException(
                "Corresponding enum not found for provided cancellation type.");
    }
}
