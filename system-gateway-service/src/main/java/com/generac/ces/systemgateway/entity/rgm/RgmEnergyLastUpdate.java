package com.generac.ces.systemgateway.entity.rgm;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.Data;

@Data
@Entity
@IdClass(RgmEnergyId.class)
public class RgmEnergyLastUpdate {

    @Id
    @Column(name = "system_id")
    private String systemId;

    @Id
    @Column(name = "five_minute")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp timestampLocal;
}
