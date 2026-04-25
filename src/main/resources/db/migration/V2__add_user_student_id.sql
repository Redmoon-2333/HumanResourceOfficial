ALTER TABLE `user` ADD COLUMN `student_id` VARCHAR(20) NULL COMMENT '学号';
ALTER TABLE `user` ADD UNIQUE KEY `uk_student_id` (`student_id`);
