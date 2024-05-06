CREATE TABLE IF NOT EXISTS subscription_status_history (
  id               INT NOT NULL AUTO_INCREMENT,
  subscription_id  INT NOT NULL,
  status           VARCHAR(10) NOT NULL, -- PENDING,SUCCESS,FAILURE
  failure_reason   TEXT NULL,

  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  CONSTRAINT history_subscr_fk_1
     FOREIGN KEY (subscription_id) REFERENCES subscription_audit (id),
  INDEX(subscription_id, status)
);