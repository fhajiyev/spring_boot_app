package com.generac.ces.systemgateway.model.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.systemgateway.model.enums.DeviceType;
import com.generac.ces.systemgateway.model.exception.InvalidDevicePropertyTypeException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceList {

    private String systemId;
    private List<Device> devices;

    public DeviceList() {
        devices = new ArrayList<>();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class Device {
        private String deviceId;
        private DeviceType deviceType;
        private List<DeviceProperty> properties;

        public Device() {
            properties = new ArrayList<>();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceProperty {
        private String name;
        private String value;
        private String type;

        @JsonIgnore
        public Object getTypedValue() throws InvalidDevicePropertyTypeException {
            switch (type) {
                case "STRING":
                    return value;
                case "DOUBLE":
                    return Double.valueOf(value);
                default:
                    throw new InvalidDevicePropertyTypeException(type);
            }
        }
    }
}
