INSERT INTO rate_limit (
 system_type,
 subscriber_type,
 resource_type,
 daily_limit_seconds,
 weekly_limit_seconds,
 monthly_limit_seconds,
 max_duration_seconds
)
VALUES
('ESS', 'PWRVIEW', 'ENERGY_RECORDSET_1HZ', 172800, 0, 0, 300),
('ESS', 'SUNNOVA', 'ENERGY_RECORDSET_1HZ', 900, 0, 0, 60);