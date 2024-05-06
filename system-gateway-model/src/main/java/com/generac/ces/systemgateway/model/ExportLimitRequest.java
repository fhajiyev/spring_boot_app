package com.generac.ces.systemgateway.model;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportLimitRequest implements Serializable {
    @NotNull private String inverterId;
    @NotNull private Boolean exportOverride;
    private Long exportLimit;
}
