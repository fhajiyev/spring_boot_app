package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.system.control.subcontrol.PvLinkControl;
import com.generac.ces.systemgateway.model.common.DeviceState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PvlSettingResponseDto {
    private String rcpn;
    private DeviceState state;
    private PvLinkControl.PVLinkSetting.PVRSSState pvrssState;
    private double minimumInputVoltage;
    private PvLinkControl.PVLinkSetting.NumberOfSubstrings numberOfSubstrings;
}
