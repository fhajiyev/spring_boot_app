package com.generac.ces.systemgateway.repository.device;

import com.generac.ces.systemgateway.entity.device.*;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class DeviceDataRepositoryImpl implements DeviceDataRepository {

    @Autowired
    @Qualifier("clickhouseEM") EntityManager em;

    private static final String DEVICE_MAP_QUERY =
            "SELECT\n"
                + "    null as first_update,\n"
                + "    s.last_heard as last_heard,\n"
                + "    s.device_id as device_id,\n"
                + "    s.inverter_id as inverter_id,\n"
                + "    r.state_code as state_code,\n"
                + "    r.state_text as state_text,\n"
                + "    s.device_type as device_type,\n"
                + "    CASE WHEN s.device_type = 'LOAD_CONTROLLER' THEN 'Generac' ELSE"
                + " d.manufacturer END as manufacturer,\n"
                + "    CASE WHEN s.device_type = 'LOAD_CONTROLLER' THEN s.device_type ELSE d.model"
                + " END as model,\n"
                + "    d.version as version,\n"
                + "    d.serial_number as serial_number,\n"
                + "    n.nameplate as nameplate\n"
                + "FROM (\n"
                + "    SELECT \n"
                + "        device_id, \n"
                + "        inverter_id, \n"
                + "        device_type,\n"
                + "        argMax(st, created_timestamp_utc) as st,\n"
                + "        argMax(created_timestamp_utc, created_timestamp_utc) as last_heard\n"
                + "    FROM status.device_shadow\n"
                + "    WHERE inverter_id IN :inverterIds\n"
                + "    GROUP BY device_id, inverter_id, device_type\n"
                + "    UNION distinct\n"
                + "    SELECT\n"
                + "        device_id, \n"
                + "        inverter_id, \n"
                + "        device_type, \n"
                + "        null as st,\n"
                + "        argMax(created_timestamp_utc, created_timestamp_utc) as last_heard\n"
                + "    FROM status.ess_device_info\n"
                + "    WHERE inverter_id IN :inverterIds AND NOT is_soft_deleted\n"
                + "    AND device_type = 'RGM'\n"
                + "    GROUP BY device_id, inverter_id, device_type\n"
                + ") s\n"
                + "LEFT JOIN (\n"
                + "    SELECT \n"
                + "        device_id, \n"
                + "        inverter_id,\n"
                + "        argMax(device_type, di.created_timestamp_utc) as device_type,\n"
                + "        argMax(manufacturer, di.created_timestamp_utc) as manufacturer,\n"
                + "        argMax(model, di.created_timestamp_utc) as model,\n"
                + "        argMax(version, di.created_timestamp_utc) as version,\n"
                + "        argMax(serial_number, di.created_timestamp_utc) as serial_number\n"
                + "    FROM status.ess_device_info di\n"
                + "    WHERE inverter_id IN :inverterIds\n"
                + "    GROUP BY device_id, inverter_id\n"
                + ") d\n"
                + "ON s.device_id = d.device_id AND s.inverter_id = d.inverter_id\n"
                + "LEFT JOIN (\n"
                + "    SELECT\n"
                + "        device_id,\n"
                + "        argMax(nameplate, created_timestamp_utc) as nameplate\n"
                + "    FROM status.nameplate\n"
                + "    GROUP BY device_id\n"
                + ") n\n"
                + "ON s.device_id = n.device_id\n"
                + "LEFT JOIN status.rcp_state r\n"
                + "ON r.state_code = bitAnd(s.st, 65520)\n"
                + "SETTINGS \n"
                + "use_query_cache = true,\n"
                + "max_threads = 20\n";

    private static final String SYSTEM_FAMILY_MAP_QUERY =
            "SELECT\n"
                    + "    system_id,\n"
                    + "    argMax(system_family, created_timestamp_utc) as system_family\n"
                    + "FROM metadata.system_family_map\n"
                    + "WHERE system_id IN :systemIds\n"
                    + "GROUP BY system_id\n";

    @Override
    public List<DeviceMap> findR1DeviceMap(Collection<String> inverterIds) {
        Query query = em.createNativeQuery(DEVICE_MAP_QUERY, DeviceMap.class);
        query.setParameter("inverterIds", inverterIds);

        List<DeviceMap> res = query.getResultList();

        for (DeviceMap dm : res) {
            if (dm.getStateText() != null && dm.getStateText().isBlank()) {
                dm.setSt(null);
                dm.setStateText(null);
            }

            if (dm.getManufacturer() != null && dm.getManufacturer().isBlank()) {
                dm.setManufacturer(null);
            }

            if (dm.getModel() != null && dm.getModel().isBlank()) {
                dm.setModel(null);
            }

            if (dm.getVersion() != null && dm.getVersion().isBlank()) {
                dm.setVersion(null);
            }

            if (dm.getSerialNumber() != null && dm.getSerialNumber().isBlank()) {
                dm.setSerialNumber(null);
            }

            if (dm.getNameplate() != null && dm.getNameplate().isBlank()) {
                dm.setNameplate(null);
            }
        }
        res.removeAll(Collections.singleton(null));
        res.sort(Comparator.comparing(DeviceMap::getDeviceId));
        return res;
    }

    @Override
    public List<DeviceMap> findR2DeviceMap(Collection<String> inverterIds) {

        // TODO.
        List<DeviceMap> res = new ArrayList<>(); // = query.getResultList();

        res.removeAll(Collections.singleton(null));
        res.sort(Comparator.comparing(DeviceMap::getDeviceId));
        return res;
    }

    @Override
    public List<SystemFamily> findSystemFamiliesForSystemIds(Collection<String> systemIds) {
        Query query = em.createNativeQuery(SYSTEM_FAMILY_MAP_QUERY, SystemFamily.class);
        query.setParameter("systemIds", systemIds);

        return query.getResultList();
    }
}
