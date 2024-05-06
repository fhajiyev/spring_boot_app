package com.generac.ces.systemgateway.model.common;

import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.system.SystemModeOuterClass;
import java.util.Arrays;
import java.util.List;

public enum SystemMode {
    SAFETY_SHUTDOWN(1, SystemModeOuterClass.SystemMode.SAFETY_SHUTDOWN),
    GRID_TIE(2, SystemModeOuterClass.SystemMode.GRID_TIE),
    SELF_SUPPLY(4, SystemModeOuterClass.SystemMode.SELF_SUPPLY),
    CLEAN_BACKUP(8, SystemModeOuterClass.SystemMode.CLEAN_BACKUP),
    PRIORITY_BACKUP(16, SystemModeOuterClass.SystemMode.PRIORITY_BACKUP),
    REMOTE_ARBITRAGE(32, SystemModeOuterClass.SystemMode.REMOTE_ARBITRAGE),
    SELL(64, SystemModeOuterClass.SystemMode.SELL),
    UNKNOWN_SYSTEM_MODE(128, SystemModeOuterClass.SystemMode.UNKNOWN_SYSTEM_MODE);

    private final int bitField;

    public final SystemModeOuterClass.SystemMode mode;

    SystemMode(int bitField, SystemModeOuterClass.SystemMode mode) {
        this.bitField = bitField;
        this.mode = mode;
    }

    public int getBitField() {
        return bitField;
    }

    public static SystemMode fromMode(SystemModeOuterClass.SystemMode mode) {
        for (SystemMode systemMode : values()) {
            if (systemMode.mode == mode) {
                return systemMode;
            }
        }
        throw new IllegalArgumentException(
                "Corresponding enum not found for provided system mode.");
    }

    public static SystemMode fromEssDpSystemMode(SysModes mode) {
        return switch (mode) {
            case SafetyShutdown -> SAFETY_SHUTDOWN;
            case GridTie -> GRID_TIE;
            case SelfSupply -> SELF_SUPPLY;
            case CleanBackup -> CLEAN_BACKUP;
            case PriorityBackup -> PRIORITY_BACKUP;
            case RemoteArbitrage -> REMOTE_ARBITRAGE;
            case Sell -> SELL;
            default -> UNKNOWN_SYSTEM_MODE;
        };
    }

    public int getOrder() {
        return switch (this) {
            case GRID_TIE -> 1;
            case SELF_SUPPLY -> 2;
            case CLEAN_BACKUP -> 3;
            case PRIORITY_BACKUP -> 4;
            case REMOTE_ARBITRAGE -> 5;
            case SELL -> 6;
            default -> Integer.MAX_VALUE;
        };
    }

    public static List<SystemMode> getAllModesExceptExcluded() {
        return Arrays.stream(values())
                .filter(mode -> mode != SAFETY_SHUTDOWN && mode != UNKNOWN_SYSTEM_MODE)
                .toList();
    }
}
