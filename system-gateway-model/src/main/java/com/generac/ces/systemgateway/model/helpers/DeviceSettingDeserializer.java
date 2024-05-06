package com.generac.ces.systemgateway.model.helpers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.io.IOException;

public class DeviceSettingDeserializer extends JsonDeserializer<DeviceSetting> {
    @Override
    public DeviceSetting deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String rawSettingName = p.getValueAsString();
        if (rawSettingName != null) {
            for (DeviceSetting setting : DeviceSetting.values()) {
                if (setting.name().equals(rawSettingName)) {
                    return setting;
                }
            }
        }
        return DeviceSetting.UNKNOWN;
    }
}
