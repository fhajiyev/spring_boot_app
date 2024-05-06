package com.generac.ces.systemgateway.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.generac.ces.systemgateway.model.common.DeviceState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DeviceStateTest {
    @Test
    public void testFromBoolean_validMappingReturnsCorrectEnum() {
        // Arrange
        boolean testState = true;
        DeviceState expected = DeviceState.STATE_ENABLED;

        // Action
        DeviceState actual = DeviceState.fromBoolean(testState);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testFromBoolean_invalidMappingThrowsIllegalArgumentException() {
        // Action & Assert
        try {
            DeviceState.fromBoolean(null);
            fail("Expected IllegalArgumentException not thrown");
        } catch (Exception e) {
            assertEquals("Corresponding enum not found for provided state.", e.getMessage());
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }
}
