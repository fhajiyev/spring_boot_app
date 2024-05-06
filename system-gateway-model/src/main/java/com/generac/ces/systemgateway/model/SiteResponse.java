package com.generac.ces.systemgateway.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class SiteResponse implements Serializable {
    private static final long serialVersionUID = 2405172041950251807L;

    private String siteId;
    private String systemId;
    private String serialNumber;
}
