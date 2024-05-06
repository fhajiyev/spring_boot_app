CREATE TABLE IF NOT EXISTS subscription_audit (
  id               INT NOT NULL AUTO_INCREMENT,
  subscription_id  VARCHAR(36) NOT NULL, -- uuid
  system_type      VARCHAR(10) NOT NULL, -- ESS,PWRMICRO,HEM
  subscriber_type  VARCHAR(10) NOT NULL, -- PWRVIEW,SUNNOVA
  resource_type    VARCHAR(25) NOT NULL, -- ENERGY_RECORDSET_1HZ
  system_id        VARCHAR(36) NOT NULL, -- uuid
  host_id          VARCHAR(20) NOT NULL,
  duration_seconds INT NOT NULL,
  expires_at       DATETIME NOT NULL,

  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE (subscription_id),
  INDEX(subscriber_type, system_type, resource_type, system_id, created_at)
);