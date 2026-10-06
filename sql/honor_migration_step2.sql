-- 增量迁移：补齐缺失字段（可重复执行，报 Duplicate column 可忽略）

USE awardsystem;

ALTER TABLE `skill_tag` ADD COLUMN `level` VARCHAR(20) NOT NULL DEFAULT 'basic' COMMENT '技能等级 basic/intermediate/advanced';
ALTER TABLE `skill_tag` ADD COLUMN `icon` VARCHAR(200) DEFAULT NULL COMMENT '技能图标';
ALTER TABLE `skill_tag` ADD COLUMN `allow_self_eval` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许学生自评';
ALTER TABLE `skill_tag` ADD COLUMN `award_only` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否只能由获奖记录验证';
ALTER TABLE `skill_tag` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态 enabled/disabled';
ALTER TABLE `skill_tag` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序';
ALTER TABLE `skill_tag` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `student_skill` ADD COLUMN `self_eval_level` VARCHAR(20) NOT NULL DEFAULT 'beginner' COMMENT '自评等级';
ALTER TABLE `student_skill` ADD COLUMN `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已验证';
ALTER TABLE `student_skill` ADD COLUMN `experience` INT NOT NULL DEFAULT 0 COMMENT '技能经验值';
ALTER TABLE `student_skill` ADD COLUMN `source` VARCHAR(50) NOT NULL DEFAULT 'self_eval' COMMENT '技能来源';
ALTER TABLE `student_skill` ADD COLUMN `application_id` INT DEFAULT NULL COMMENT '来源获奖申请ID';
ALTER TABLE `student_skill` ADD COLUMN `note` VARCHAR(255) DEFAULT NULL COMMENT '备注';
ALTER TABLE `student_skill` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;