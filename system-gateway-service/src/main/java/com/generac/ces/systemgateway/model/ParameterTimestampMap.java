package com.generac.ces.systemgateway.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ParameterTimestampMap {
    private Map<String, Instant> parameterTimestampMap = new HashMap<>();

    public void addParameterTimestamp(List<String> parameters) {
        Instant now = Instant.now();
        for (String parameter : parameters) {
            parameterTimestampMap.put(parameter, now);
        }
    }

    public Instant getTimestamp(String parameter) {
        return parameterTimestampMap.get(parameter);
    }

    public Map<String, Instant> getParameterTimestampMap() {
        return parameterTimestampMap;
    }
}
