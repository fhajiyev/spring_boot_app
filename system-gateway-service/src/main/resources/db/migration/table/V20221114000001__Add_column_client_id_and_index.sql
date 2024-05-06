ALTER TABLE subscription_audit ADD client_id varchar(128) NOT NULL;

ALTER TABLE subscription_audit ADD INDEX subscription_id_client_id_idx (subscription_id, client_id);