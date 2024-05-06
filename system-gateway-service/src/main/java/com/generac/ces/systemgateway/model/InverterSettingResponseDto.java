package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.generac.ces.system.control.subcontrol.InverterSettingControlOuterClass;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.common.SystemMode;
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
public class InverterSettingResponseDto {
    private String rcpn;
    private DeviceState state;

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
    private List<SystemMode> activeSystemModes;

    private InverterSettingControlOuterClass.InverterSetting.Islanding islanding;
    private InverterSettingControlOuterClass.InverterSetting.ExportOverride exportOverride;
    private InverterSettingControlOuterClass.InverterSetting.NumberOfTransferSwitches
            numberOfTransferSwitches;
    private InverterSettingControlOuterClass.InverterSetting.LoadShedding loadShedding;
    private InverterSettingControlOuterClass.InverterSetting.CTCalibration ctCalibration;
    private InverterSettingControlOuterClass.InverterSetting.GeneratorControlMode
            generatorControlMode;
    private InverterSettingControlOuterClass.InverterSetting.ExportLimitState exportLimitState;
    private Integer exportLimit;
}
