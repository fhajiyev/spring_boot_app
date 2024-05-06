CREATE TABLE IF NOT EXISTS rate_limit (
  id                    INT NOT NULL AUTO_INCREMENT,
  system_type           VARCHAR(10) NOT NULL, -- ESS,PWRMICRO,HEM
  subscriber_type       VARCHAR(10) NOT NULL, -- PWRVIEW,SUNNOVA
  resource_type         VARCHAR(25) NOT NULL, -- ENERGY_RECORDSET_1HZ
  daily_limit_seconds   INT NOT NULL,
  weekly_limit_seconds  INT NOT NULL,
  monthly_limit_seconds INT NOT NULL,

  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id)
);