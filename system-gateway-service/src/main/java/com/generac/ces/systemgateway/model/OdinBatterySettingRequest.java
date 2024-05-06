package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.DeviceState;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class OdinBatterySettingRequest {
    ControlMessage controlMessage;

    @Data
    public static class ControlMessage implements Serializable {
        String controlMessageId = UUID.randomUUID().toString();

        private List<String> rcpns;
        private DeviceState state;
        private Double socMax;
        private Double socMin;
        private Double socRsvMax;
        private Double socRsvMin;
        private Double aChaMax;
        private Double aDisChaMax;
    }
}
