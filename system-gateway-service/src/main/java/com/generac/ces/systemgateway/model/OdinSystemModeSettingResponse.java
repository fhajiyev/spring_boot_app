package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.SystemMode;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OdinSystemModeSettingResponse implements Serializable {
    private SystemMode systemMode;
    private boolean cancelOnExternalChange;
}
