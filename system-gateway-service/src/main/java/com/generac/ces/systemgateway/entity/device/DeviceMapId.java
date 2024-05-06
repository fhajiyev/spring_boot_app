package com.generac.ces.systemgateway.entity.device;

import com.generac.ces.systemgateway.model.enums.DeviceType;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

@Data
public class DeviceMapId implements Serializable {
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "inverter_id")
    private String inverterId;

    @Column(name = "device_type")
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;
}
