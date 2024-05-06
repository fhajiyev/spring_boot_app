package com.generac.ces.systemgateway.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.generac.ces.essdataprovider.enums.SysModes;
import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.model.SystemModeRequest;
import com.generac.ces.systemgateway.model.common.SystemMode;
import java.io.IOException;
import org.junit.Test;

public class SysModeConversionHelperTest {

    // =======================================================================================================
    //   SYSTEM MODE CONVERSION
    // =======================================================================================================
    @Test
    public void testSysModeConversion_CorrectSysMode() {
        // Arrange
        SystemModeRequest requestDto = new SystemModeRequest();
        requestDto.setMode(SystemMode.GRID_TIE);
        SysModes systemMode = SysModeConversion.convert(requestDto.getMode());
        assertEquals(SysModes.GridTie, systemMode);

        requestDto.setMode(SystemMode.SELF_SUPPLY);
        systemMode = SysModeConversion.convert(requestDto.getMode());
        assertEquals(SysModes.SelfSupply, systemMode);

        requestDto.setMode(SystemMode.CLEAN_BACKUP);
        systemMode = SysModeConversion.convert(requestDto.getMode());
        assertEquals(SysModes.CleanBackup, systemMode);

        requestDto.setMode(SystemMode.PRIORITY_BACKUP);
        systemMode = SysModeConversion.convert(requestDto.getMode());
        assertEquals(SysModes.PriorityBackup, systemMode);

        requestDto.setMode(SystemMode.REMOTE_ARBITRAGE);
        systemMode = SysModeConversion.convert(requestDto.getMode());
        assertEquals(SysModes.RemoteArbitrage, systemMode);

        requestDto.setMode(SystemMode.SELL);
        systemMode = SysModeConversion.convert(requestDto.getMode());
        assertEquals(SysModes.Sell, systemMode);
    }

    @Test
    public void testSysModeConversion_NullSysMode() throws IOException {
        // Arrange
        String expected = "System mode cannot be null";

        try {
            SysModeConversion.convert(null);
        } catch (Exception e) {
            assertEquals(expected, e.getMessage());
            assertEquals(BadRequestException.class, e.getClass());
        }
    }
}
