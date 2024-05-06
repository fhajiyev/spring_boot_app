package com.generac.ces.systemgateway.model;

import java.io.Serializable;
import java.util.UUID;
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
public class SystemResponse implements Serializable {
    private static final long serialVersionUID = 2405172041950251807L;

    // ess and micro
    private UUID systemId;

    // ess
    private String siteId;
    private String rcpId;
    private String beaconRcpn;

    // ess and micro
    private String hostDeviceId;
}
