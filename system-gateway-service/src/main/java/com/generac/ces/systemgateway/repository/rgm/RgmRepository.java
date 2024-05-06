package com.generac.ces.systemgateway.repository.rgm;

import com.generac.ces.systemgateway.entity.rgm.RgmEnergyLastUpdate;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface RgmRepository {
    List<RgmEnergyLastUpdate> findLastUpdateBySystemIdIn(
            List<String> systemIds, Timestamp startTime);
}
