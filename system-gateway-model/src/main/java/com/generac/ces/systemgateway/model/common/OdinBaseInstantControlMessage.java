package com.generac.ces.systemgateway.model.common;

import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class OdinBaseInstantControlMessage implements Serializable {
    private UUID controlMessageId = UUID.randomUUID();
}
