CREATE TABLE `task` (
  `task_id`       BIGINT NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(200) NOT NULL,
  `description`   TEXT NULL,
  `creator_id`    INT NOT NULL             COMMENT '部长/副部长 user_id',
  `creator_year`  INT NOT NULL             COMMENT '创建时创建者所属届别',
  `target_year`   INT NOT NULL             COMMENT '任务面向的届别',
  `due_time`      DATETIME NULL            COMMENT '截止时间',
  `priority`      TINYINT NOT NULL DEFAULT 1 COMMENT '0 低 / 1 中 / 2 高',
  `status`        VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / CLOSED',
  `remind_cooldown_minutes` INT NOT NULL DEFAULT 60 COMMENT '催促冷却分钟数',
  `deleted`       TINYINT(1) NOT NULL DEFAULT 0  COMMENT '逻辑删除：0 未删 / 1 已删',
  `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_target_year_status` (`target_year`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

CREATE TABLE `task_assignment` (
  `assignment_id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id`       BIGINT NOT NULL,
  `assignee_id`   INT NOT NULL,
  `status`        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / DONE',
  `done_time`     DATETIME NULL,
  `done_remark`   VARCHAR(500) NULL,
  `last_remind_time` DATETIME NULL       COMMENT '最近一次催促时间',
  `remind_count`  INT NOT NULL DEFAULT 0,
  `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uk_task_assignee` (`task_id`, `assignee_id`),
  KEY `idx_assignee_status` (`assignee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务指派表';

CREATE TABLE `task_remind_log` (
  `log_id`        BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT NOT NULL,
  `operator_id`   INT NOT NULL,
  `remind_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `channel`       VARCHAR(16) NOT NULL COMMENT 'INAPP / EMAIL / BOTH',
  PRIMARY KEY (`log_id`),
  KEY `idx_assignment` (`assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催促历史';
