-- 修复激活码状态枚举：将字符串存储改为整数存储
UPDATE activation_code SET status = '0' WHERE status = '未使用';
UPDATE activation_code SET status = '1' WHERE status = '已使用';
UPDATE activation_code SET status = '2' WHERE status = '已过期';

ALTER TABLE activation_code MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '激活码状态: 0=未使用, 1=已使用, 2=已过期';