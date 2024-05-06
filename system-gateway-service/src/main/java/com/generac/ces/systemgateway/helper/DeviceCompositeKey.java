package com.generac.ces.systemgateway.helper;

import java.io.Serializable;

public record DeviceCompositeKey(String name, String deviceRcpn) implements Serializable {}
