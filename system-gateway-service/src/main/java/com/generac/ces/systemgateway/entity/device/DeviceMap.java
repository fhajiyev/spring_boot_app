package com.generac.ces.systemgateway.entity.device;

import com.generac.ces.systemgateway.model.enums.DeviceType;
import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(DeviceMapId.class)
public class DeviceMap implements DeviceMapInterface, Serializable {
    @Column(name = "first_update")
    private Timestamp firstUpdate;

    @Column(name = "last_heard")
    private Timestamp lastHeard;

    @Id
    @Column(name = "device_id")
    private String deviceId;

    @Id
    @Column(name = "inverter_id")
    private String inverterId;

    @Id
    @Column(name = "state_code")
    private Integer st;

    @Column(name = "state_text")
    private String stateText;

    @Id
    @Column(name = "device_type")
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    private String manufacturer;
    private String model;
    private String version;

    @Column(name = "serial_number")
    private String serialNumber;

    private String nameplate;
}
