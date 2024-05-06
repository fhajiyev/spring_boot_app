package com.generac.ces.systemgateway.repository.device;

import com.generac.ces.systemgateway.entity.device.DeviceMap;
import com.generac.ces.systemgateway.entity.device.SystemFamily;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceDataRepository {
    List<DeviceMap> findR1DeviceMap(Collection<String> inverterIds);

    List<DeviceMap> findR2DeviceMap(Collection<String> inverterIds);

    List<SystemFamily> findSystemFamiliesForSystemIds(Collection<String> systemIds);
}
