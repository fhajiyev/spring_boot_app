package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.helper.DeviceCompositeKey;
import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ParameterTimestampValueMap<T> implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Map<DeviceCompositeKey, ParameterTimestampValue> entryMap = new HashMap<>();

    public void addParameterTimestampValue(
            DeviceCompositeKey parameter, Object value, OffsetDateTime offsetDateTime) {
        ParameterTimestampValue timestampValue = new ParameterTimestampValue(offsetDateTime, value);
        entryMap.put(parameter, timestampValue);
    }

    public ParameterTimestampValue getParameterTimestampValue(DeviceCompositeKey parameter) {
        return Optional.ofNullable(parameter).map(entryMap::get).orElse(null);
    }

    public Map<DeviceCompositeKey, ParameterTimestampValue> getEntryMap() {
        return entryMap;
    }

    @Data
    @AllArgsConstructor
    public static class ParameterTimestampValue implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private OffsetDateTime timestamp;
        private Object value;
    }

    public OffsetDateTime getLatestTimestamp() {
        return entryMap.values().stream()
                .map(ParameterTimestampValue::getTimestamp)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.MIN);
    }
}
