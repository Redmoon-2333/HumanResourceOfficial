CREATE TABLE `role_change_log` (
  `log_id`        BIGINT NOT NULL AUTO_INCREMENT,
  `target_user_id` INT NOT NULL,
  `operator_id`   INT NOT NULL,
  `before_role_history` VARCHAR(500),
  `after_role_history`  VARCHAR(500),
  `reason`        VARCHAR(255) NULL,
  `change_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_target` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份变更日志';
