package com.generac.ces.systemgateway.model;

import com.generac.ces.systemgateway.model.common.SystemMode;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemModeRequest {
    String controlMessageId = UUID.randomUUID().toString();
    @NotNull private SystemMode mode;
}
