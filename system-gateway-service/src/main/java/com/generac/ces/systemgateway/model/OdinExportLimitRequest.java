package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.generac.ces.systemgateway.model.common.OdinBaseInstantControlMessage;
import lombok.Data;

@Data
public class OdinExportLimitRequest {
    ControlMessage controlMessage;

    @Data
    public static class ControlMessage extends OdinBaseInstantControlMessage {
        @JsonUnwrapped ExportLimitRequest exportLimitRequest;
    }
}
