package com.generac.ces.systemgateway.entity.rgm;

import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class RgmEnergyId implements Serializable {

    private String systemId;
    private Timestamp timestampLocal;
}
