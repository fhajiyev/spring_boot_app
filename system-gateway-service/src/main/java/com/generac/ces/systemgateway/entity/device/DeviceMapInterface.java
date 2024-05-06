package com.generac.ces.systemgateway.entity.device;

import com.generac.ces.systemgateway.model.enums.DeviceType;
import java.sql.Timestamp;

public interface DeviceMapInterface {
    Timestamp getFirstUpdate();

    Timestamp getLastHeard();

    void setLastHeard(Timestamp lastHeard);

    String getDeviceId();

    String getInverterId();

    Integer getSt();

    String getStateText();

    DeviceType getDeviceType();

    String getManufacturer();

    String getModel();

    String getVersion();

    String getSerialNumber();

    String getNameplate();
}
