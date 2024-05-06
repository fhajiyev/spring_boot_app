package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.StreamInterval;
import com.generac.ces.systemgateway.model.common.SystemType;
import lombok.Data;

@Data
public class OdinStartStreamRequest {
    private Integer durationSecs;
    private StreamInterval interval;
    private SystemType systemType;
}
