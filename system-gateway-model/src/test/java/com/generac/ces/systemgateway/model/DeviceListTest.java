package com.generac.ces.systemgateway.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.systemgateway.model.device.DeviceList;
import com.generac.ces.systemgateway.model.enums.DeviceType;
import com.generac.ces.systemgateway.model.exception.InvalidDevicePropertyTypeException;
import org.junit.jupiter.api.Test;

class DeviceListTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeviceListDeserialization() throws Exception {
        // Arrange
        String deviceListFile = "src/test/resources/validDeviceList.json";
        String deviceListJson = TestUtil.readFromFile(deviceListFile);
        DeviceList deviceListObject = objectMapper.readValue(deviceListJson, DeviceList.class);

        // Assert
        assertEquals("55742987-dcc2-4673-b2f1-c07bca2b81f8", deviceListObject.getSystemId());

        Object property1value = null;
        Object property2value = null;
        Object property3value = null;
        for (DeviceList.Device device : deviceListObject.getDevices()) {
            assertEquals("battery-id", device.getDeviceId());
            assertEquals(DeviceType.BATTERY.name(), device.getDeviceType().name());
            for (DeviceList.DeviceProperty property : device.getProperties()) {
                if ("property-1".equals(property.getName())) {
                    property1value = property.getTypedValue();
                }
                if ("property-2".equals(property.getName())) {
                    property2value = property.getTypedValue();
                }
                if ("property-with-no-value".equals(property.getName())) {
                    property3value = property.getTypedValue();
                }
            }
        }

        assertEquals("some-string", property1value);
        assertEquals(123.0D, property2value);
        assertNull(property3value);

        DeviceList.DeviceProperty propertyWithNoType = null;
        for (DeviceList.Device device : deviceListObject.getDevices()) {
            for (DeviceList.DeviceProperty property : device.getProperties()) {
                if ("property-with-invalid-type".equals(property.getName())) {
                    propertyWithNoType = property;
                }
            }
        }

        final DeviceList.DeviceProperty propertyWithNoTypeFinal =
                propertyWithNoType; // variable in lambda should be final
        assertThrows(
                InvalidDevicePropertyTypeException.class,
                () -> {
                    propertyWithNoTypeFinal.getTypedValue();
                });
    }
}
