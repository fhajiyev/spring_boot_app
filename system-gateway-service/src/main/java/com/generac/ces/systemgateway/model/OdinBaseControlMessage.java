package com.generac.ces.systemgateway.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class OdinBaseControlMessage implements Serializable {
    private UUID controlMessageId = UUID.randomUUID();
}
