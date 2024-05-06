package com.generac.ces.systemgateway.entity.device;

import com.generac.ces.system.SystemFamilyOuterClass;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class SystemFamily implements Serializable {
    @Id
    @Column(name = "system_id")
    private String systemId;

    @Id
    @Column(name = "system_family")
    @Enumerated(EnumType.STRING)
    private SystemFamilyOuterClass.SystemFamily systemFamily;
}
