package com.generac.ces.systemgateway.repository.rgm;

import com.generac.ces.systemgateway.entity.rgm.RgmEnergyLastUpdate;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class RgmRepositoryImpl implements RgmRepository {

    @Autowired
    @Qualifier("clickhouseEM") private EntityManager em;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String RGM_LAST_UPDATES_BY_SYSTEM_IDS =
            "SELECT "
                    + "system_id, "
                    + "max(timestamp_local) as five_minute "
                    + "FROM "
                    + "pwr.system_energy_rgm "
                    + "WHERE "
                    + "system_id IN (:systemIds) "
                    + "AND timestamp_local > '%s' "
                    + "GROUP BY system_id";

    @Override
    public List<RgmEnergyLastUpdate> findLastUpdateBySystemIdIn(
            List<String> systemIds, Timestamp startTime) {
        Query query =
                em.createNativeQuery(
                        String.format(
                                RGM_LAST_UPDATES_BY_SYSTEM_IDS,
                                startTime.toLocalDateTime().format(dtf)),
                        RgmEnergyLastUpdate.class);
        query.setParameter("systemIds", systemIds);
        return query.getResultList();
    }
}
