package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.SystemMode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@Builder
public class ActiveSystemModeOdinResponse extends OdinResponse {
    private List<SystemMode> actSysMds;
}
