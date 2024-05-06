package com.generac.ces.systemgateway.integrationtest;

import com.generac.ces.systemgateway.entity.subscription.SubscriptionAudit;
import com.generac.ces.systemgateway.entity.subscription.SubscriptionMetric;
import com.generac.ces.systemgateway.model.common.ResourceType;
import com.generac.ces.systemgateway.model.common.SubscriberType;
import com.generac.ces.systemgateway.model.common.SystemType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Seeder {

    private JdbcTemplate mysqlJdbcTemplate;
    private JdbcTemplate clickhouseJdbcTemplate;

    @Autowired
    public void setAuditJdbcTemplate(@Qualifier("sgDataSource") DataSource dataSource) {
        this.mysqlJdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Autowired
    public void setClickhouseJdbcTemplate(@Qualifier("chDataSource") DataSource dataSource) {
        this.clickhouseJdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void cleanDB() {
        log.info("START: cleanDB");
        mysqlJdbcTemplate.execute("DELETE FROM systemgateway.subscription_audit");
        mysqlJdbcTemplate.execute("DELETE FROM systemgateway.subscription_metric");
        clickhouseJdbcTemplate.execute("TRUNCATE TABLE status.ess_device_info");
        clickhouseJdbcTemplate.execute("TRUNCATE TABLE status.device_shadow");
        clickhouseJdbcTemplate.execute("TRUNCATE TABLE status.nameplate");
        log.info("END: cleanDB");
    }

    public void populateClickhouseFromFile(String sql) {
        clickhouseJdbcTemplate.execute(sql);
    }

    public List<SubscriptionAudit> getAllAuditRecords() {
        log.info("START: get all audit records");
        List<SubscriptionAudit> auditList =
                mysqlJdbcTemplate.query(
                        "SELECT * FROM systemgateway.subscription_audit", new AuditRowMapper());
        log.info("END: get all audit records");
        return auditList;
    }

    public List<SubscriptionMetric> getAllMetricRecords() {
        log.info("START: get all metric records");
        List<SubscriptionMetric> metricList =
                mysqlJdbcTemplate.query(
                        "SELECT * FROM systemgateway.subscription_metric", new MetricRowMapper());
        log.info("END: get all metric records");
        return metricList;
    }

    private class AuditRowMapper implements RowMapper<SubscriptionAudit> {

        @Override
        public SubscriptionAudit mapRow(ResultSet rs, int rowNum) throws SQLException {

            SubscriptionAudit audit =
                    SubscriptionAudit.builder()
                            .id(rs.getLong("id"))
                            .subscriptionId(rs.getString("subscription_id"))
                            .systemType(SystemType.valueOf(rs.getString("system_type")))
                            .subscriberType(SubscriberType.valueOf(rs.getString("subscriber_type")))
                            .resourceType(ResourceType.valueOf(rs.getString("resource_type")))
                            .hostId(rs.getString("host_id"))
                            .durationSec(rs.getInt("duration_seconds"))
                            .expiresAt(rs.getTimestamp("expires_at"))
                            .clientId(rs.getString("client_id"))
                            .build();

            return audit;
        }
    }

    private class MetricRowMapper implements RowMapper<SubscriptionMetric> {

        @Override
        public SubscriptionMetric mapRow(ResultSet rs, int rowNum) throws SQLException {

            SubscriptionMetric metric =
                    SubscriptionMetric.builder()
                            .id(rs.getLong("id"))
                            .systemId(rs.getString("system_id"))
                            .systemType(SystemType.valueOf(rs.getString("system_type")))
                            .subscriberType(SubscriberType.valueOf(rs.getString("subscriber_type")))
                            .resourceType(ResourceType.valueOf(rs.getString("resource_type")))
                            .totalDailyDurationSec(rs.getInt("total_daily_duration_seconds"))
                            .expiresAt(rs.getTimestamp("expires_at"))
                            .build();

            return metric;
        }
    }
}
