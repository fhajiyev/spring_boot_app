package com.generac.ces.systemgateway.helper;

import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.model.common.SystemMode;

public class SysModeConversion {
    public static SysModes convert(SystemMode sysMode) {
        if (sysMode == null) throw new BadRequestException("System mode cannot be null");
        switch (sysMode) {
            case GRID_TIE:
                return SysModes.GridTie;
            case SELF_SUPPLY:
                return SysModes.SelfSupply;
            case CLEAN_BACKUP:
                return SysModes.CleanBackup;
            case PRIORITY_BACKUP:
                return SysModes.PriorityBackup;
            case REMOTE_ARBITRAGE:
                return SysModes.RemoteArbitrage;
            case SELL:
                return SysModes.Sell;
            default:
                throw new BadRequestException("Unknown system mode");
        }
    }
}
