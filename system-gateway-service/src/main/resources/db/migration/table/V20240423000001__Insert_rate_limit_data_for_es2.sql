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
('ES2', 'PWRVIEW', 'ENERGY_RECORDSET_1HZ', 5184000, 0, 0, 300),
('ES2', 'SUNNOVA', 'ENERGY_RECORDSET_1HZ', 900, 0, 0, 60);