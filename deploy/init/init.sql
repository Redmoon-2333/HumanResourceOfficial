-- ============================================================
-- 人力资源中心官网 - 数据库初始化脚本（包含日常活动图片）
-- 生成日期: 2026-02-13
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一部分: 数据库表结构定义
-- ============================================================

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `user_id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
    `username` VARCHAR(20) NOT NULL COMMENT '用户名，3-20字符，唯一',
    `password` VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密存储',
    `name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `role_history` VARCHAR(500) DEFAULT NULL COMMENT '角色历史记录，JSON格式存储历届职务',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 日常活动图片表
DROP TABLE IF EXISTS `daily_image`;
CREATE TABLE `daily_image` (
    `image_id` INT NOT NULL AUTO_INCREMENT COMMENT '图片ID，主键自增',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL地址',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '图片标题',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '图片描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，数值越小越靠前',
    `is_active` BOOLEAN DEFAULT TRUE COMMENT '是否启用状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`image_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常活动图片表';

-- 资料分类表
DROP TABLE IF EXISTS `material_category`;
CREATE TABLE `material_category` (
    `category_id` INT NOT NULL AUTO_INCREMENT COMMENT '分类ID，主键自增',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，数值越小越靠前',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资料一级分类表';

-- 资料子分类表
DROP TABLE IF EXISTS `material_subcategory`;
CREATE TABLE `material_subcategory` (
    `subcategory_id` INT NOT NULL AUTO_INCREMENT COMMENT '子分类ID，主键自增',
    `category_id` INT NOT NULL COMMENT '所属一级分类ID',
    `subcategory_name` VARCHAR(50) NOT NULL COMMENT '子分类名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`subcategory_id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_subcategory_category` FOREIGN KEY (`category_id`)
        REFERENCES `material_category` (`category_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资料二级分类表';

-- 资料表
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
    `material_id` INT NOT NULL AUTO_INCREMENT COMMENT '资料ID，主键自增',
    `category_id` INT NOT NULL COMMENT '一级分类ID',
    `subcategory_id` INT NOT NULL COMMENT '二级分类ID',
    `material_name` VARCHAR(100) NOT NULL COMMENT '资料名称',
    `description` TEXT DEFAULT NULL COMMENT '资料描述',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件存储URL',
    `file_size` INT DEFAULT NULL COMMENT '文件大小(字节)',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型(扩展名)',
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `uploader_id` INT DEFAULT NULL COMMENT '上传者用户ID',
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    PRIMARY KEY (`material_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_subcategory_id` (`subcategory_id`),
    KEY `idx_uploader_id` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资料表';

-- 活动表
DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
    `activity_id` INT NOT NULL AUTO_INCREMENT COMMENT '活动ID，主键自增',
    `activity_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `background` TEXT DEFAULT NULL COMMENT '活动背景',
    `significance` TEXT DEFAULT NULL COMMENT '活动意义',
    `purpose` TEXT DEFAULT NULL COMMENT '活动目的',
    `process` TEXT DEFAULT NULL COMMENT '活动流程',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

-- 活动图片表
DROP TABLE IF EXISTS `activity_image`;
CREATE TABLE `activity_image` (
    `image_id` INT NOT NULL AUTO_INCREMENT COMMENT '图片ID，主键自增',
    `activity_id` INT NOT NULL COMMENT '关联活动ID',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '图片描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`image_id`),
    KEY `idx_activity_id` (`activity_id`),
    CONSTRAINT `fk_image_activity` FOREIGN KEY (`activity_id`)
        REFERENCES `activity` (`activity_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动图片表';

-- 往届活动表
DROP TABLE IF EXISTS `past_activity`;
CREATE TABLE `past_activity` (
    `past_activity_id` INT NOT NULL AUTO_INCREMENT COMMENT '往届活动ID，主键自增',
    `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `push_url` VARCHAR(500) DEFAULT NULL COMMENT '推送链接URL',
    `year` INT DEFAULT NULL COMMENT '活动年份',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`past_activity_id`),
    KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='往届活动表';

-- 激活码表
DROP TABLE IF EXISTS `activation_code`;
CREATE TABLE `activation_code` (
    `code_id` INT NOT NULL AUTO_INCREMENT COMMENT '激活码ID，主键自增',
    `code` VARCHAR(50) NOT NULL COMMENT '激活码',
    `creator_id` INT NOT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` ENUM('未使用', '已使用') DEFAULT '未使用' COMMENT '激活码状态',
    `user_id` INT DEFAULT NULL COMMENT '使用者用户ID',
    `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    PRIMARY KEY (`code_id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='激活码表';

-- ============================================================
-- 第二部分: 测试数据
-- ============================================================

-- 用户测试数据
INSERT INTO `user` (`user_id`, `username`, `password`, `name`, `role_history`) VALUES
(1, 'hucongyucpp2', '$2a$10$GgQSKQXKaJKBnyxqYcwMd.BFrXrpT.f.3fHugVTOU/RiMeF2pPQYS', '胡淙煜', '["2024级部员&2025级部长"]');

-- 日常活动图片测试数据
INSERT INTO `daily_image` (`image_id`, `image_url`, `title`, `description`, `sort_order`, `is_active`) VALUES
(1, '/uploads/daily/2026/02/tea_party_01.jpg', '部门例会茶歇时光', '每周例会后的温馨茶歇时间', 1, TRUE),
(2, '/uploads/daily/2026/02/birthday_cake_01.jpg', '成员生日庆祝', '为部门成员精心准备的生日蛋糕', 2, TRUE),
(3, '/uploads/daily/2026/02/tree_hole_session_01.jpg', '树洞倾诉时光', '每周固定时段的树洞环节', 3, TRUE),
(4, '/uploads/daily/2026/02/board_game_night_01.jpg', '桌游欢乐时光', '部门团建活动之桌游之夜', 4, TRUE),
(5, '/uploads/daily/2026/02/team_dinner_01.jpg', '部门聚餐时刻', '月度部门聚餐，放松心情享受美食', 5, TRUE),
(6, '/uploads/daily/2026/02/cozy_room_01.jpg', '温馨小屋聚会', '在舒适的休息室里聊天谈心', 6, TRUE),
(7, '/uploads/daily/2026/02/workshop_01.jpg', '技能培训现场', '定期举办的专业技能培训', 7, TRUE),
(8, '/uploads/daily/2026/02/mentor_mentee_01.jpg', '师徒结对指导', '资深成员一对一指导新成员', 8, TRUE);

-- 资料分类测试数据
INSERT INTO `material_category` (`category_id`, `category_name`, `sort_order`) VALUES
(1, '规章制度', 1),
(2, '工作文档', 2),
(3, '培训资料', 3),
(4, '活动资料', 4),
(5, '宣传素材', 5);

-- 资料子分类测试数据
INSERT INTO `material_subcategory` (`subcategory_id`, `category_id`, `subcategory_name`, `sort_order`) VALUES
(1, 1, '章程条例', 1),
(2, 1, '管理办法', 2),
(3, 1, '考核制度', 3),
(4, 2, '会议记录', 1),
(5, 2, '工作计划', 2),
(6, 2, '工作总结', 3),
(7, 3, '新人培训', 1),
(8, 3, '技能培训', 2),
(9, 4, '活动策划', 1),
(10, 4, '活动总结', 2),
(11, 5, '海报设计', 1),
(12, 5, '视频素材', 2);

-- 资料测试数据
INSERT INTO `material` (`material_id`, `category_id`, `subcategory_id`, `material_name`, `description`, `file_url`, `file_size`, `file_type`, `uploader_id`, `download_count`) VALUES
(1, 1, 1, '学生会章程', '学生会组织章程', '/materials/rules/charter.pdf', 102400, 'pdf', 1, 156),
(2, 1, 2, '财务管理办法', '学生会财务管理相关规定', '/materials/rules/finance.pdf', 51200, 'pdf', 1, 89),
(3, 1, 3, '成员考核制度', '学生会成员考核评分标准', '/materials/rules/assessment.pdf', 30720, 'pdf', 1, 234),
(4, 2, 4, '2024年第一次例会记录', '2024年春季学期第一次全体例会记录', '/materials/docs/meeting_202401.pdf', 25600, 'pdf', 1, 45),
(5, 2, 5, '2024年工作计划', '学生会2024年度工作计划', '/materials/docs/plan_2024.docx', 40960, 'docx', 1, 78);

-- 活动测试数据
INSERT INTO `activity` (`activity_id`, `activity_name`, `background`, `significance`, `purpose`, `process`) VALUES
(1, '2024年迎新晚会', '每年新生入学后举办的大型文艺活动', '促进新老生交流，展示学生会风采', '为新生提供展示自我的平台', '1. 前期宣传\n2. 节目征集\n3. 场地布置\n4. 正式演出\n5. 后期总结'),
(2, '2024年春季招新', '新学期开始，为学生会注入新鲜血液', '保证学生会工作的连续性', '招募有热情、有能力的同学', '1. 制定方案\n2. 宣传推广\n3. 报名收集\n4. 面试选拔\n5. 公示结果');

-- 活动图片测试数据
INSERT INTO `activity_image` (`image_id`, `activity_id`, `image_url`, `description`, `sort_order`) VALUES
(1, 1, '/uploads/activity/2024/welcome_01.jpg', '迎新晚会现场全景', 1),
(2, 1, '/uploads/activity/2024/welcome_02.jpg', '开场舞蹈表演', 2),
(3, 2, '/uploads/activity/2024/recruit_01.jpg', '招新宣传摊位', 1);

-- 往届活动测试数据
INSERT INTO `past_activity` (`past_activity_id`, `title`, `cover_image`, `push_url`, `year`) VALUES
(1, '2023年迎新晚会精彩回顾', '/uploads/past/2023/welcome.jpg', 'https://mp.weixin.qq.com/example1', 2023),
(2, '2023年春季招新圆满结束', '/uploads/past/2023/recruit.jpg', 'https://mp.weixin.qq.com/example2', 2023),
(3, '2022年校运会志愿服务纪实', '/uploads/past/2022/sports.jpg', 'https://mp.weixin.qq.com/example3', 2022);

-- 激活码测试数据
INSERT INTO `activation_code` (`code_id`, `code`, `creator_id`, `status`, `user_id`, `use_time`, `expire_time`) VALUES
(1, 'HR2024-ABCD-1234', 1, '未使用', NULL, NULL, DATE_ADD(NOW(), INTERVAL 30 DAY)),
(2, 'HR2024-EFGH-5678', 1, '未使用', NULL, NULL, DATE_ADD(NOW(), INTERVAL 30 DAY)),
(3, 'HR2023-USED-0001', 1, '已使用', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- ============================================================
-- 第三部分: 索引优化
-- ============================================================

ALTER TABLE `material` ADD FULLTEXT INDEX `ft_material_name_desc` (`material_name`, `description`);
ALTER TABLE `activity` ADD FULLTEXT INDEX `ft_activity_name` (`activity_name`);

-- ============================================================
-- 第四部分: 视图定义
-- ============================================================

CREATE OR REPLACE VIEW `v_material_detail` AS
SELECT
    m.material_id,
    m.material_name,
    m.description,
    m.file_url,
    m.file_size,
    m.file_type,
    m.upload_time,
    m.download_count,
    mc.category_name,
    ms.subcategory_name,
    u.name AS uploader_name
FROM `material` m
LEFT JOIN `material_category` mc ON m.category_id = mc.category_id
LEFT JOIN `material_subcategory` ms ON m.subcategory_id = ms.subcategory_id
LEFT JOIN `user` u ON m.uploader_id = u.user_id;

CREATE OR REPLACE VIEW `v_user_roles` AS
SELECT
    user_id,
    username,
    name,
    role_history,
    CASE
        WHEN role_history LIKE '%超级管理员%' THEN 'admin'
        WHEN role_history LIKE '%主任%' OR role_history LIKE '%部长%' THEN 'minister'
        ELSE 'member'
    END AS role_level
FROM `user`;

CREATE OR REPLACE VIEW `v_active_daily_images` AS
SELECT
    image_id,
    image_url,
    title,
    description,
    sort_order,
    create_time
FROM `daily_image`
WHERE is_active = TRUE
ORDER BY sort_order ASC, create_time ASC;

SET FOREIGN_KEY_CHECKS = 1;
