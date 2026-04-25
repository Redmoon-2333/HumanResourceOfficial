-- ============================================================
-- 人力资源中心官网 - 数据库初始化脚本
-- 基于生产环境备份 (hrofficial-full-backup.sql 2026-04-19)
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
-- 第二部分: 生产数据（来自 2026-04-19 备份）
-- ============================================================

-- 用户数据
-- 注意：password 字段为 BCrypt 哈希值占位符，首次部署时需要替换为实际哈希值
INSERT INTO `user` (`user_id`, `username`, `password`, `name`, `role_history`, `create_time`, `update_time`) VALUES
(1, 'hucongyucpp2', '{{BCRYPT_HASH}}', '胡淙煜', '["2024级部员&2025级部长"]', '2026-04-14 20:25:09', '2026-04-14 20:25:09');

-- 激活码数据
INSERT INTO `activation_code` (`code_id`, `code`, `creator_id`, `create_time`, `status`, `user_id`, `use_time`, `expire_time`) VALUES
(1, 'HR2024-ABCD-1234', 1, '2026-04-14 20:25:09', '未使用', NULL, NULL, '2026-05-14 20:25:09'),
(2, 'HR2024-EFGH-5678', 1, '2026-04-14 20:25:09', '未使用', NULL, NULL, '2026-05-14 20:25:09'),
(3, 'HR2023-USED-0001', 1, '2026-04-14 20:25:09', '已使用', 1, '2026-04-04 20:25:09', '2026-05-14 20:25:09');

-- 资料分类数据
INSERT INTO `material_category` (`category_id`, `category_name`, `sort_order`, `create_time`) VALUES
(1, '规章制度', 1, '2026-04-14 20:25:09'),
(2, '工作文档', 2, '2026-04-14 20:25:09'),
(3, '培训资料', 3, '2026-04-14 20:25:09'),
(4, '活动资料', 4, '2026-04-14 20:25:09');

-- 资料子分类数据
INSERT INTO `material_subcategory` (`subcategory_id`, `category_id`, `subcategory_name`, `sort_order`, `create_time`) VALUES
(1, 1, '章程条例', 1, '2026-04-14 20:25:09'),
(2, 1, '管理办法', 2, '2026-04-14 20:25:09'),
(3, 1, '考核制度', 3, '2026-04-14 20:25:09'),
(4, 2, '会议记录', 1, '2026-04-14 20:25:09'),
(5, 2, '工作计划', 2, '2026-04-14 20:25:09'),
(6, 2, '工作总结', 3, '2026-04-14 20:25:09'),
(7, 3, '新人培训', 1, '2026-04-14 20:25:09'),
(8, 3, '技能培训', 2, '2026-04-14 20:25:09'),
(9, 4, '活动策划', 1, '2026-04-14 20:25:09'),
(10, 4, '活动总结', 2, '2026-04-14 20:25:09');

-- 资料数据
INSERT INTO `material` (`material_id`, `category_id`, `subcategory_id`, `material_name`, `description`, `file_url`, `file_size`, `file_type`, `upload_time`, `uploader_id`, `download_count`) VALUES
(2, 1, 2, '财务管理办法', '学生会财务管理相关规定', '/materials/rules/finance.pdf', 51200, 'pdf', '2026-04-14 20:25:09', 1, 89),
(3, 1, 3, '成员考核制度', '学生会成员考核评分标准', '/materials/rules/assessment.pdf', 30720, 'pdf', '2026-04-14 20:25:09', 1, 234),
(4, 2, 4, '2024年第一次例会记录', '2024年春季学期第一次全体例会记录', '/materials/docs/meeting_202401.pdf', 25600, 'pdf', '2026-04-14 20:25:09', 1, 45),
(5, 2, 5, '2024年工作计划', '学生会2024年度工作计划', '/materials/docs/plan_2024.docx', 40960, 'docx', '2026-04-14 20:25:09', 1, 78);

-- 活动数据
INSERT INTO `activity` (`activity_id`, `activity_name`, `background`, `significance`, `purpose`, `process`, `create_time`, `update_time`) VALUES
(3, '软件工程学院学生会招新活动', '\n软件工程学院学生会人力资源中心负责学生会成员的招募工作，通过宣讲会和面试选拔优秀学生加入学生会各部门。\n', '为学生会注入新鲜血液，选拔有能力、有热情的同学加入学生会，推动学生会工作的持续发展。', '向新生介绍学生会的组织架构和各部门职能，吸引优秀学生报名加入学生会，通过面试选拔合适的人才。\n', '宣讲会：介绍学生会各部门职能、工作内容\n\n报名表收集：收集有意向同学的报名信息\n\n面试安排：组织各部门面试官进行面试\n\n结果公布：公布录取名单\n\n', '2026-04-15 01:09:58', '2026-04-15 01:09:58'),
(4, '软件工程学院团校开学典礼', '团校是培养优秀共青团员的重要平台，开学典礼是团校学员正式开启学习之旅的重要仪式。', '增进学员对团校的了解，促进团校成员之间的相互熟悉，培养团校学员团结合作的精神，增加学员之间的友谊，充分弘扬软件工程学院\"求实创新\"的良好风气。', '使学员在团校中\"学会思考，学会团结，学会学习，学会创新\"，发扬软件学院\"求实奋发\"的精神，强化学员为学院争光的意识，培养学员自我学习的能力，向中国共产党输送优秀的年轻人才。\n', '主持人宣布开学典礼开始，全体起立唱团歌\n领导、老师致辞\n学员代表发言\n破冰活动（团队建设游戏）\n主持人宣布开学典礼结束\n', '2026-04-15 01:53:05', '2026-04-15 01:53:05'),
(5, '企业参观活动', '\n为帮助学生了解互联网企业的实际工作环境和企业文化，人力资源中心组织学生参观腾讯公司。\n', '让学生近距离接触知名互联网企业，了解行业发展趋势，拓宽视野，为未来的职业规划提供参考。', '了解一线公司的发展历程和企业文化，感受互联网企业的创新氛围，激发学生的学习热情和创新意识。\n', '集合签到前往公司\n参观企业展厅\n与企业代表座谈交流\n合影留念\n返回学校\n', '2026-04-15 01:54:41', '2026-04-15 01:54:41'),
(6, '定向越野', '为培养团校成员爱国主义精神，帮助团校成员深入了解红色经典、红色文化，增进团校成员的沟通和对团校人文氛围的感受。', '\n通过定向越野活动，加强团校凝聚力，推广定向越野运动，向团校成员普及定向知识。\n', '培养团校成员爱国主义精神，帮助团校成员深入了解红色经典、红色文化，增进团校成员的沟通，加强团校凝聚力，推广定向越野运动。\n', '前期准备：选定打卡点、制作活动预告推送、物资采买\n活动当天：签到集合、定向越野进行、计分颁奖\n后期总结：撰写推送总结\n', '2026-04-15 01:56:09', '2026-04-15 01:56:09'),
(7, '技能培训沙龙', '提升学生的实用技能，人力资源中心举办多种领域的技能培训沙龙。', '帮助学生掌握实用技能，提升综合素质。', '讲解实用技能的基础知识和技巧、流程和方法，提升学生各项综合能力。\n', '讲师介绍\n 基础知识讲解、技巧分享\n 实操练习\n互动交流\n总结\n', '2026-04-15 02:03:59', '2026-04-15 02:03:59'),
(8, ' 一站到底知识竞赛', '为提升团校成员对共青团知识的掌握程度，增强团员的责任感与使命感，同时培养学生的知识储备、逻辑思维与团队协作能力。', '通过竞赛的形式，激发团员学习团知识的热情，活跃校园文化氛围。', '提升团校成员对共青团知识的掌握程度，增强团员的责任感与使命感，培养学生的知识储备、逻辑思维与团队协作能力，加强团校凝聚力。\n', ' 题库准备与确定\n参赛队伍分组\n知识竞赛现场进行（抢答形式）\n 成绩结算\n颁奖环节\n', '2026-04-15 02:05:09', '2026-04-15 02:05:09'),
(9, '杰出青年评选', '为表彰在各方面表现突出的学生，树立青年榜样，人力资源中心举办杰出青年评选活动。', '激励学生全面发展，树立优秀榜样，营造积极向上的校园氛围。', '评选出在学业、社会实践、创新创业等方面表现突出的学生，树立青年榜样，激励更多同学追求卓越。\n', '线上报名和材料提交\n线上投票\n线下答辩\n综合评选\n公布结果并颁奖\n', '2026-04-15 02:07:29', '2026-04-15 02:07:29'),
(10, '团校结业典礼', '团校学员完成一学期的学习和活动后，举办结业典礼总结学习成果，表彰优秀学员。', '总结团校学习成果，表彰优秀学员，为团校学习画上圆满句号。', '总结团校学习成果，表彰优秀学员，颁发结业证书和优秀学员证书，为团校学习画上圆满句号。', '主持人宣布结业典礼开始\n回顾团校学习历程\n优秀学员表彰\n颁发结业证书\n领导总结致辞\n合影留念\n', '2026-04-15 02:08:52', '2026-04-15 02:08:52');

-- 往届活动数据
INSERT INTO `past_activity` (`past_activity_id`, `title`, `cover_image`, `push_url`, `year`, `create_time`) VALUES
(1, '2023年迎新晚会精彩回顾', '/uploads/past/2023/welcome.jpg', 'https://mp.weixin.qq.com/example1', 2023, '2026-04-14 20:25:09'),
(2, '2023年春季招新圆满结束', '/uploads/past/2023/recruit.jpg', 'https://mp.weixin.qq.com/example2', 2023, '2026-04-14 20:25:09'),
(3, '2022年校运会志愿服务纪实', '/uploads/past/2022/sports.jpg', 'https://mp.weixin.qq.com/example3', 2022, '2026-04-14 20:25:09');

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
