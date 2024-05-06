package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.generac.ces.systemgateway.model.common.DeviceState;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatterySettingResponseDto implements Serializable {

    private List<OdinBatterySetting> setting;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OdinBatterySetting implements Serializable {

        private String rcpn;

        private DeviceState state;

        @JsonUnwrapped BaseBatterySetting batterySetting;
    }
}
