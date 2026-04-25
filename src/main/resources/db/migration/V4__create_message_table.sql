CREATE TABLE `message` (
  `message_id`   BIGINT NOT NULL AUTO_INCREMENT,
  `receiver_id`  INT NOT NULL          COMMENT '接收者 user_id',
  `sender_id`    INT NULL              COMMENT '发送者 user_id，系统消息为 NULL',
  `type`         VARCHAR(32) NOT NULL  COMMENT 'TASK_ASSIGNED / TASK_REMIND / TASK_COMPLETED / ROLE_CHANGED / SYSTEM',
  `title`        VARCHAR(128) NOT NULL,
  `content`      TEXT NOT NULL,
  `ref_type`     VARCHAR(32) NULL      COMMENT '关联对象类型，如 TASK',
  `ref_id`       BIGINT NULL           COMMENT '关联对象 ID',
  `is_read`      TINYINT(1) NOT NULL DEFAULT 0,
  `read_time`    DATETIME NULL,
  `deleted`      TINYINT(1) NOT NULL DEFAULT 0  COMMENT '逻辑删除：0 未删 / 1 已删',
  `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_receiver_unread` (`receiver_id`, `is_read`, `deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信表';
