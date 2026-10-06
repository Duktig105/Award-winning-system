
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `application_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application_file` (
  `file_id` int NOT NULL AUTO_INCREMENT COMMENT '文件ID（主键）',
  `application_id` int NOT NULL COMMENT '关联奖项申请表ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件原始名称（如获奖证书.pdf）',
  `file_path` varchar(512) NOT NULL COMMENT '文件存储路径（服务器/OSS路径）',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型（如pdf/jpg/png/docx）',
  `file_size` int NOT NULL COMMENT '文件大小（单位：字节）',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文件上传时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_application_id` (`application_id`),
  CONSTRAINT `application_file_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `award_application` (`application_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖项申请的资料证明文件表（支持多文件）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `application_risk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application_risk` (
  `risk_id` int NOT NULL AUTO_INCREMENT COMMENT '风险ID（主键）',
  `application_id` int NOT NULL COMMENT '关联申请ID',
  `risk_level` varchar(20) NOT NULL DEFAULT 'none' COMMENT '综合风险等级（high/medium/low/none）',
  `need_manual_review` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要人工复核（0/1）',
  `risk_reasons` text COMMENT '风险原因JSON数组',
  `manual_mark` varchar(20) DEFAULT NULL COMMENT '人工标记（normal_reuse-正常复用/abnormal_duplicate-异常重复/undetermined-无法判断）',
  `manual_remark` varchar(500) DEFAULT NULL COMMENT '人工处理备注',
  `manual_reviewer` varchar(50) DEFAULT NULL COMMENT '审核人用户名',
  `manual_time` datetime DEFAULT NULL COMMENT '人工标记时间',
  `final_opinion` varchar(500) DEFAULT NULL COMMENT '审核人员最终意见',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`risk_id`),
  UNIQUE KEY `uk_application_id` (`application_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申请风险评估与人工处理结果表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `application_teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application_teacher` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关联ID（主键）',
  `teacher_name` varchar(50) NOT NULL COMMENT '指导老师姓名（与mentor表无关）',
  `teacher_department` varchar(50) NOT NULL DEFAULT '' COMMENT '指导老师所在学院（与mentor表无关）',
  `teacher_no` varchar(20) NOT NULL DEFAULT '' COMMENT '指导老师工号（与mentor表无关）',
  `application_id` int NOT NULL COMMENT '关联申请表ID',
  PRIMARY KEY (`id`),
  KEY `application_id` (`application_id`),
  CONSTRAINT `application_teacher_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `award_application` (`application_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申请的指导老师关联表（支持多个指导老师，与mentor表无关）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `award_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `award_application` (
  `application_id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID（主键）',
  `competition_id` int NOT NULL COMMENT '关联竞赛表ID',
  `team_id` int DEFAULT NULL COMMENT '关联团队ID（团队申请时非空，个人申请时为NULL）',
  `member_role` varchar(20) DEFAULT NULL COMMENT '团队角色快照(队长/技术骨干/核心成员/普通成员,空则按团队信息推导)',
  `student_id` int NOT NULL COMMENT '申请人ID（关联学生表）',
  `project_name` varchar(100) NOT NULL COMMENT '项目名称（获奖项目的具体名称）',
  `award_quantity` int NOT NULL DEFAULT '1' COMMENT '获奖数量（学生无需填写，导师统计维护，团体赛默认1）',
  `award_person_count` int NOT NULL DEFAULT '0' COMMENT '获奖人数（学生无需填写，按团队成员数自动统计/导师复核）',
  `contact` varchar(50) NOT NULL COMMENT '负责人联系方式',
  `competition_level` varchar(20) NOT NULL COMMENT '竞赛级别',
  `award_rank` varchar(20) NOT NULL COMMENT '获奖等次（A/B/C/D）',
  `award_level` varchar(20) NOT NULL COMMENT '获奖等级（如一等奖/金奖）',
  `award_time` date NOT NULL COMMENT '获奖时间',
  `application_status` varchar(50) NOT NULL COMMENT '申请状态（pending-待审核, approved-已通过, rejected-已拒绝, returned-已打回）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
  `application_number` varchar(20) DEFAULT NULL COMMENT '申请编号（格式：SJ+日期，如SJ20251129）',
  `breakthrough` tinyint(1) NOT NULL DEFAULT '0' COMMENT '历史性突破奖标记(特殊系数×1.5,管理员认定)',
  PRIMARY KEY (`application_id`),
  UNIQUE KEY `application_number` (`application_number`),
  KEY `competition_id` (`competition_id`),
  KEY `team_id` (`team_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `award_application_ibfk_1` FOREIGN KEY (`competition_id`) REFERENCES `competition` (`competition_id`) ON DELETE CASCADE,
  CONSTRAINT `award_application_ibfk_2` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`) ON DELETE SET NULL,
  CONSTRAINT `award_application_ibfk_3` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_application_status` CHECK ((`application_status` in (_utf8mb4'pending',_utf8mb4'approved',_utf8mb4'rejected',_utf8mb4'returned')))
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖项申请表（含项目名称，支持团队/个人申请）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `badge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `badge` (
  `badge_id` int NOT NULL AUTO_INCREMENT,
  `badge_code` varchar(50) NOT NULL COMMENT '勋章代码',
  `badge_name` varchar(50) NOT NULL COMMENT '勋章名称',
  `description` varchar(500) DEFAULT NULL COMMENT '勋章说明',
  `icon` varchar(200) DEFAULT NULL COMMENT '图标',
  `unlock_condition` varchar(1000) NOT NULL COMMENT '解锁条件JSON',
  `unlock_threshold` int NOT NULL DEFAULT '1' COMMENT '解锁阈值',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示顺序',
  `status` varchar(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`badge_id`),
  UNIQUE KEY `uk_badge_code` (`badge_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='勋章配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `certificate_fingerprint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate_fingerprint` (
  `fingerprint_id` int NOT NULL AUTO_INCREMENT COMMENT '指纹ID（主键）',
  `application_id` int NOT NULL COMMENT '关联申请ID',
  `file_id` int NOT NULL COMMENT '关联文件ID',
  `sha256` char(64) NOT NULL COMMENT '文件SHA-256哈希（完全重复判断依据）',
  `phash` varchar(16) DEFAULT NULL COMMENT '感知哈希（16位hex，图片文件才有）',
  `image_width` int DEFAULT NULL COMMENT '标准化图片宽',
  `image_height` int DEFAULT NULL COMMENT '标准化图片高',
  `standard_path` varchar(512) DEFAULT NULL COMMENT '标准化图片的网络相对路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`fingerprint_id`),
  UNIQUE KEY `uk_file_id` (`file_id`),
  KEY `idx_sha256` (`sha256`),
  KEY `idx_application_id` (`application_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书文件指纹表（查重基础数据）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `competition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition` (
  `competition_id` int NOT NULL AUTO_INCREMENT COMMENT '竞赛ID（主键）',
  `competition_name` varchar(50) NOT NULL COMMENT '竞赛名称',
  `category_id` int DEFAULT NULL COMMENT '所属竞赛类别',
  `award_rank` varchar(100) NOT NULL COMMENT '获奖等次（A/B/C/D）',
  `grade` varchar(2) DEFAULT NULL COMMENT '目录等次A/B/C/D',
  `override_level` varchar(10) DEFAULT NULL COMMENT '计分级别覆盖(如大创年会)',
  `override_grade` varchar(2) DEFAULT NULL COMMENT '计分等次覆盖',
  `base_score` decimal(6,1) DEFAULT NULL COMMENT '竞赛基础分=等次基础分×级别系数',
  `series_code` varchar(60) DEFAULT NULL COMMENT '赛事系列码(防重复计分)',
  `catalog_year` int DEFAULT NULL COMMENT '目录版本年份',
  `competition_type` varchar(20) DEFAULT '团体赛',
  `min_team_size` int NOT NULL DEFAULT '1',
  `max_team_size` int NOT NULL DEFAULT '10',
  `team_open_time` datetime DEFAULT NULL,
  `team_close_time` datetime DEFAULT NULL,
  `team_enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`competition_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1027 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='竞赛信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `competition_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_category` (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `category_name` varchar(50) NOT NULL COMMENT '类别名称',
  `description` varchar(500) DEFAULT NULL COMMENT '类别说明',
  `icon` varchar(200) DEFAULT NULL COMMENT '类别图标(URL或SVG名)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '类别排序',
  `status` varchar(20) NOT NULL DEFAULT 'enabled' COMMENT '状态 enabled/disabled',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_category_name` (`category_name`),
  KEY `idx_category_status` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='竞赛类别表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `competition_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_skill` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` int NOT NULL COMMENT '竞赛类别ID',
  `competition_id` int DEFAULT NULL COMMENT '竞赛ID(竞赛级覆盖映射,为空=类别默认)',
  `skill_id` int NOT NULL COMMENT '技能ID',
  `contribution` int NOT NULL DEFAULT '10' COMMENT '技能贡献值(每次获奖+经验)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '技能权重(0-1,和=1)',
  `upgrade_rule` varchar(500) DEFAULT NULL COMMENT '技能升级规则(JSON简版描述)',
  `team_role` varchar(100) DEFAULT NULL COMMENT '团队角色(如前端负责人)',
  `team_role_weight` int NOT NULL DEFAULT '50' COMMENT '该技能在团队角色中的贡献权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cat_comp_skill` (`category_id`,`competition_id`,`skill_id`),
  KEY `idx_competition_skill_category` (`category_id`),
  KEY `idx_competition_skill_skill` (`skill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='竞赛与技能对应关系';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `duplicate_check_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `duplicate_check_record` (
  `check_id` int NOT NULL AUTO_INCREMENT COMMENT '查重记录ID（主键）',
  `application_id` int NOT NULL COMMENT '本次查重关联的申请ID',
  `file_id` int NOT NULL COMMENT '本次查重的文件ID',
  `sha256` char(64) DEFAULT NULL COMMENT '文件SHA-256',
  `phash` varchar(16) DEFAULT NULL COMMENT '文件pHash',
  `exact_duplicate` tinyint NOT NULL DEFAULT '0' COMMENT '是否完全重复（0否/1是）',
  `similar_count` int NOT NULL DEFAULT '0' COMMENT '相似历史证书数量',
  `max_similarity` decimal(5,4) DEFAULT NULL COMMENT '最高相似度（0~1）',
  `check_status` varchar(20) NOT NULL DEFAULT 'done' COMMENT '查重状态（pending/running/done/failed）',
  `check_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查重时间',
  PRIMARY KEY (`check_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书查重结果记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `duplicate_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `duplicate_match` (
  `match_id` int NOT NULL AUTO_INCREMENT COMMENT '匹配ID（主键）',
  `check_id` int NOT NULL COMMENT '关联查重记录ID',
  `application_id` int NOT NULL COMMENT '发起查重的申请ID',
  `file_id` int NOT NULL COMMENT '发起查重的文件ID',
  `matched_application_id` int NOT NULL COMMENT '被匹配到的历史申请ID',
  `matched_file_id` int NOT NULL COMMENT '被匹配到的历史文件ID',
  `matched_student_id` int DEFAULT NULL COMMENT '被匹配申请的申请人学生ID',
  `match_type` varchar(20) NOT NULL COMMENT '匹配类型（exact-完全相同/phash-感知相似）',
  `similarity` decimal(5,4) NOT NULL COMMENT '相似度（exact为1.0000）',
  `team_related` tinyint NOT NULL DEFAULT '0' COMMENT '双方是否同一团队成员（0否/1是）',
  `handled` tinyint NOT NULL DEFAULT '0' COMMENT '是否已被人工处理（避免反复报警）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`match_id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_matched_application_id` (`matched_application_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书相似匹配明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `honor_score_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `honor_score_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT,
  `rule_version` varchar(10) DEFAULT 'v1.0' COMMENT '规则版本',
  `param_group` varchar(30) DEFAULT NULL COMMENT '参数组',
  `param_key` varchar(50) DEFAULT NULL COMMENT '参数键',
  `param_value` varchar(100) DEFAULT NULL COMMENT '参数取值',
  `param_remark` varchar(300) DEFAULT NULL COMMENT '参数说明',
  `competition_level` varchar(50) NOT NULL COMMENT '竞赛级别(国家级/省级/校级/院级)',
  `award_rank` varchar(20) NOT NULL COMMENT '获奖等次(A/B/C/D)',
  `base_score` int NOT NULL DEFAULT '0' COMMENT '基础分',
  `award_ratio` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '获奖系数',
  `is_team` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否团队',
  `team_ratio` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '团队系数',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `enable_time` datetime DEFAULT NULL COMMENT '启用时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_param` (`rule_version`,`param_group`,`param_key`),
  KEY `idx_rule_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='荣誉积分规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `honor_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `honor_tag` (
  `tag_id` int NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) NOT NULL COMMENT '标签名称',
  `tag_type` varchar(50) NOT NULL COMMENT '标签类型(award_level/competition_direction/role/growth/ability)',
  `description` varchar(500) DEFAULT NULL COMMENT '标签说明',
  `condition_rule` varchar(1000) NOT NULL COMMENT '生成条件(JSON)',
  `icon` varchar(200) DEFAULT NULL COMMENT '标签图标',
  `status` varchar(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_name_type` (`tag_name`,`tag_type`),
  KEY `idx_tag_type_status` (`tag_type`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='荣誉标签配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `mentor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mentor` (
  `mentor_id` int NOT NULL AUTO_INCREMENT COMMENT '导师ID（主键）',
  `mentor_name` varchar(20) NOT NULL COMMENT '导师姓名',
  `department` varchar(50) NOT NULL DEFAULT '' COMMENT '所在学院',
  `mentor_no` varchar(20) NOT NULL COMMENT '导师工号（唯一）',
  PRIMARY KEY (`mentor_id`),
  UNIQUE KEY `uk_mentor_no` (`mentor_no`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导师信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ocr_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ocr_record` (
  `ocr_id` int NOT NULL AUTO_INCREMENT COMMENT 'OCR记录ID（主键）',
  `application_id` int NOT NULL COMMENT '关联申请ID',
  `file_id` int NOT NULL COMMENT '关联文件ID',
  `ocr_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'OCR状态（pending/success/failed/skipped）',
  `raw_text` mediumtext COMMENT 'OCR识别原文（按行拼接）',
  `recognized_name` varchar(100) DEFAULT NULL COMMENT '识别出的姓名',
  `recognized_competition` varchar(200) DEFAULT NULL COMMENT '识别出的竞赛名称',
  `recognized_award_level` varchar(100) DEFAULT NULL COMMENT '识别出的获奖等级',
  `recognized_award_time` varchar(50) DEFAULT NULL COMMENT '识别出的获奖时间',
  `recognized_certificate_no` varchar(100) DEFAULT NULL COMMENT '识别出的证书编号',
  `field_confidence` varchar(500) DEFAULT NULL COMMENT '各字段置信度JSON，如{"name":0.92}',
  `overall_confidence` decimal(5,2) DEFAULT NULL COMMENT '整体置信度（0~1）',
  `compare_result` varchar(20) DEFAULT NULL COMMENT '与申报字段比对结果（consistent/inconsistent/undetermined）',
  `compare_detail` text COMMENT '逐字段比对明细JSON',
  `error_message` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '识别时间',
  PRIMARY KEY (`ocr_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_cert_no` (`recognized_certificate_no`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OCR证书识别记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `risk_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `risk_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT COMMENT '规则ID（主键）',
  `rule_key` varchar(50) NOT NULL COMMENT '规则标识（唯一）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `rule_type` varchar(20) NOT NULL DEFAULT 'flag' COMMENT '规则类型（flag-开关型/threshold-阈值型）',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值（阈值型规则使用）',
  `threshold_unit` varchar(50) DEFAULT NULL COMMENT '阈值说明（如：相似度0~1）',
  `config_json` varchar(1000) DEFAULT NULL COMMENT '扩展配置JSON（如OCR比对字段列表）',
  `risk_level` varchar(20) NOT NULL DEFAULT 'medium' COMMENT '触发后的风险等级（high/medium/low）',
  `need_manual_review` tinyint NOT NULL DEFAULT '1' COMMENT '触发后是否需要人工复核（0/1）',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用（0/1）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风险规则配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `skill_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_category` (
  `skill_category_id` int NOT NULL AUTO_INCREMENT COMMENT '技能方向ID',
  `category_name` varchar(50) NOT NULL COMMENT '方向名称',
  `parent_id` int DEFAULT NULL COMMENT '父级方向ID(预留多级)',
  `description` varchar(500) DEFAULT NULL COMMENT '方向说明',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` varchar(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`skill_category_id`),
  UNIQUE KEY `uk_skill_category_name` (`category_name`),
  KEY `idx_skill_category_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能方向表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `skill_node_manual_unlock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_node_manual_unlock` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` int NOT NULL COMMENT '学生ID',
  `node_id` int NOT NULL COMMENT '技能树节点ID',
  `unlocked_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点亮时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_node` (`student_id`,`node_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生手动点亮的技能树节点（无门槛节点）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `skill_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_tag` (
  `skill_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(40) NOT NULL,
  `skill_category_id` int DEFAULT NULL COMMENT '所属技能方向ID',
  `description` varchar(500) DEFAULT NULL COMMENT '技能说明',
  `level` varchar(20) NOT NULL DEFAULT 'basic' COMMENT '技能等级 basic/intermediate/advanced',
  `icon` varchar(200) DEFAULT NULL COMMENT '技能图标',
  `allow_self_eval` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许学生自评',
  `verified_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '仅获奖验证(与allow_self_eval互斥)',
  `award_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否只能由获奖记录验证',
  `status` varchar(20) NOT NULL DEFAULT 'enabled' COMMENT '状态 enabled/disabled',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`skill_id`),
  UNIQUE KEY `uk_skill_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `skill_tree_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_tree_node` (
  `node_id` int NOT NULL AUTO_INCREMENT,
  `skill_category_id` int DEFAULT NULL COMMENT '所属技能方向',
  `parent_id` int DEFAULT NULL COMMENT '父节点ID(顶级为NULL)',
  `skill_id` int DEFAULT NULL COMMENT '关联技能ID(叶子节点)',
  `name` varchar(50) NOT NULL COMMENT '节点名称',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '节点层级 1/2/3',
  `description` varchar(500) DEFAULT NULL,
  `icon` varchar(200) DEFAULT NULL,
  `unlock_threshold` int NOT NULL DEFAULT '0' COMMENT '解锁阈值(经验值)',
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'enabled',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `auto_unlock` tinyint NOT NULL DEFAULT '0' COMMENT '无门槛自动点亮：所有账号默认点亮并计入掌握技能',
  PRIMARY KEY (`node_id`),
  KEY `idx_node_parent` (`parent_id`),
  KEY `idx_node_skill_category` (`skill_category_id`),
  KEY `idx_node_skill` (`skill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能树节点';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `student_id` int NOT NULL AUTO_INCREMENT COMMENT '学生ID（主键）',
  `student_number` varchar(20) NOT NULL COMMENT '学生学号',
  `student_name` varchar(20) NOT NULL COMMENT '学生姓名',
  `grade` varchar(10) NOT NULL COMMENT '年级',
  `major` varchar(30) NOT NULL COMMENT '专业',
  `class_name` varchar(30) NOT NULL DEFAULT '' COMMENT '班级',
  `college` varchar(50) DEFAULT NULL COMMENT '所在学院',
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1260 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_badge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_badge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `badge_id` int NOT NULL,
  `progress` int NOT NULL DEFAULT '0' COMMENT '当前进度',
  `unlocked_at` datetime DEFAULT NULL COMMENT '解锁时间',
  `is_representative` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否代表勋章',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_badge` (`student_id`,`badge_id`),
  KEY `idx_student_badge_student` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生勋章';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_honor_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_honor_score` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL COMMENT '学生ID',
  `total_score` int NOT NULL DEFAULT '0' COMMENT '当前总积分',
  `level_score` int NOT NULL DEFAULT '0' COMMENT '当年/学期累计积分(可清零)',
  `last_calc_time` datetime DEFAULT NULL COMMENT '上次计算时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_score` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生积分总表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_honor_score_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_honor_score_log` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL COMMENT '学生ID',
  `application_id` int DEFAULT NULL COMMENT '获奖申请ID',
  `score` int NOT NULL DEFAULT '0' COMMENT '积分(可负)',
  `score_type` varchar(50) NOT NULL DEFAULT 'award' COMMENT '积分类型 award/manual/deduct',
  `description` varchar(500) DEFAULT NULL COMMENT '积分描述',
  `rule_snapshot` varchar(500) DEFAULT NULL COMMENT '规则快照',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_log_student_time` (`student_id`,`create_time`),
  KEY `idx_log_application` (`application_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生积分明细';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_honor_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_honor_tag` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `tag_id` int NOT NULL,
  `awarded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_tag` (`student_id`,`tag_id`),
  KEY `idx_student_tag_student` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=413 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生获得的荣誉标签';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_import_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_import_detail` (
  `detail_id` int NOT NULL AUTO_INCREMENT COMMENT '明细ID（主键）',
  `import_id` int NOT NULL COMMENT '关联导入ID',
  `row_num` int DEFAULT NULL COMMENT 'Excel行号（从1开始，含表头）',
  `student_number` varchar(30) DEFAULT NULL COMMENT '学号',
  `student_name` varchar(30) DEFAULT NULL COMMENT '姓名',
  `grade` varchar(20) DEFAULT NULL COMMENT '年级',
  `major` varchar(50) DEFAULT NULL COMMENT '专业',
  `class_name` varchar(50) DEFAULT NULL COMMENT '班级',
  `college` varchar(50) DEFAULT NULL COMMENT '学院',
  `action` varchar(20) DEFAULT NULL COMMENT '处理动作（insert/update/fail/skip）',
  `message` varchar(500) DEFAULT NULL COMMENT '处理结果说明',
  PRIMARY KEY (`detail_id`),
  KEY `idx_import_id` (`import_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生Excel导入明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_import_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_import_record` (
  `import_id` int NOT NULL AUTO_INCREMENT COMMENT '导入ID（主键）',
  `file_name` varchar(255) DEFAULT NULL COMMENT '上传的文件名',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总行数',
  `insert_count` int NOT NULL DEFAULT '0' COMMENT '新增数量',
  `update_count` int NOT NULL DEFAULT '0' COMMENT '更新数量',
  `skip_count` int NOT NULL DEFAULT '0' COMMENT '跳过数量（无变化）',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '失败数量',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `import_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  PRIMARY KEY (`import_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生Excel导入记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_skill` (
  `student_id` int NOT NULL,
  `skill_id` int NOT NULL,
  `self_eval_level` varchar(20) NOT NULL DEFAULT 'beginner' COMMENT '自评等级',
  `verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已验证',
  `experience` int NOT NULL DEFAULT '0' COMMENT '技能经验值',
  `source` varchar(50) NOT NULL DEFAULT 'self_eval' COMMENT '技能来源',
  `application_id` int DEFAULT NULL COMMENT '来源获奖申请ID',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`student_id`,`skill_id`),
  KEY `fk_student_skill_skill` (`skill_id`),
  CONSTRAINT `fk_student_skill_skill` FOREIGN KEY (`skill_id`) REFERENCES `skill_tag` (`skill_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_student_skill_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `student_team_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_team_profile` (
  `profile_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `bio` varchar(500) DEFAULT NULL,
  `specialties` varchar(500) DEFAULT NULL,
  `competition_experience` varchar(1500) DEFAULT NULL,
  `portfolio_url` varchar(500) DEFAULT NULL,
  `weekly_hours` int NOT NULL DEFAULT '0',
  `preferred_roles` varchar(500) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `show_bio` tinyint(1) NOT NULL DEFAULT '0',
  `show_portfolio` tinyint(1) NOT NULL DEFAULT '0',
  `show_weekly_hours` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_team_profile_student` (`student_id`),
  CONSTRAINT `fk_team_profile_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `system_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_notification` (
  `notification_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `type` varchar(30) NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` varchar(500) NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notification_user_read` (`user_id`,`is_read`,`create_time`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team` (
  `team_id` int NOT NULL AUTO_INCREMENT COMMENT '团队ID（主键，自增生成）',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '团队名称（可后补）',
  `leader_id` int DEFAULT NULL COMMENT '关联学生表ID（负责人，可后选）',
  `competition_id` int NOT NULL COMMENT '关联竞赛表ID（必选，绑定竞赛）',
  `has_sort` tinyint(1) NOT NULL DEFAULT '0' COMMENT '团队成员是否有排序（0=无，1=有）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `description` varchar(1000) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'recruiting',
  `recruiting` tinyint(1) NOT NULL DEFAULT '1',
  `target_size` int NOT NULL DEFAULT '2',
  `version` int NOT NULL DEFAULT '0',
  `vibe_tags` varchar(200) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `locked_time` datetime DEFAULT NULL,
  `removed_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`team_id`),
  KEY `leader_id` (`leader_id`),
  KEY `competition_id` (`competition_id`),
  CONSTRAINT `team_ibfk_1` FOREIGN KEY (`leader_id`) REFERENCES `student` (`student_id`) ON DELETE SET NULL,
  CONSTRAINT `team_ibfk_2` FOREIGN KEY (`competition_id`) REFERENCES `competition` (`competition_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队表（支持先建团队ID、后补名称/负责人、再加成员）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_audit_log` (
  `audit_id` bigint NOT NULL AUTO_INCREMENT,
  `operator_user_id` int NOT NULL,
  `team_id` int DEFAULT NULL,
  `action` varchar(50) NOT NULL,
  `detail` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`audit_id`),
  KEY `idx_audit_team_time` (`team_id`,`create_time`),
  KEY `fk_audit_user` (`operator_user_id`),
  CONSTRAINT `fk_audit_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`operator_user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_member` (
  `team_member_id` int NOT NULL AUTO_INCREMENT COMMENT '成员关联ID（主键）',
  `team_id` int NOT NULL COMMENT '关联团队表ID',
  `student_id` int DEFAULT NULL COMMENT '本院学生ID（外院/外校留空，关联学生表取信息）',
  `is_leader` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否负责人（0=否，1=是；允许全0）',
  `sort_order` int DEFAULT NULL COMMENT '排序序号（团队has_sort=1时必填，否则留空）',
  `external_name` varchar(20) DEFAULT NULL COMMENT '外院/外校学生姓名',
  `external_number` varchar(20) DEFAULT NULL COMMENT '外院/外校学生学号',
  `external_school` varchar(50) DEFAULT NULL COMMENT '外院/外校学生学校/学院',
  `role_name` varchar(50) DEFAULT NULL,
  `member_status` varchar(20) NOT NULL DEFAULT 'accepted',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `position_id` int DEFAULT NULL,
  PRIMARY KEY (`team_member_id`),
  KEY `idx_team_sort` (`team_id`,`sort_order`) COMMENT '团队+排序联合索引，优化查询',
  KEY `student_id` (`student_id`),
  CONSTRAINT `team_member_ibfk_1` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`) ON DELETE CASCADE,
  CONSTRAINT `team_member_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队成员表（区分本院/外院/外校学生）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_position` (
  `position_id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL,
  `title` varchar(50) NOT NULL,
  `vacancies` int NOT NULL DEFAULT '1',
  `requirements` varchar(500) DEFAULT NULL,
  `required_skills` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'open',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`position_id`),
  KEY `idx_position_team_status` (`team_id`,`status`),
  CONSTRAINT `fk_position_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_recommendation_weights`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_recommendation_weights` (
  `config_id` int NOT NULL,
  `skill_weight` int NOT NULL,
  `role_weight` int NOT NULL,
  `time_weight` int NOT NULL,
  `experience_weight` int NOT NULL,
  `background_weight` int NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `updated_by` int DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  KEY `fk_team_weights_user` (`updated_by`),
  CONSTRAINT `fk_team_weights_user` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_recommendation_weights_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_recommendation_weights_history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL,
  `skill_weight` int NOT NULL,
  `role_weight` int NOT NULL,
  `time_weight` int NOT NULL,
  `experience_weight` int NOT NULL,
  `background_weight` int NOT NULL,
  `operator_user_id` int NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `fk_team_weights_history_user` (`operator_user_id`),
  CONSTRAINT `fk_team_weights_history_user` FOREIGN KEY (`operator_user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_report` (
  `report_id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL,
  `reporter_user_id` int NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `resolution` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_time` datetime DEFAULT NULL,
  PRIMARY KEY (`report_id`),
  KEY `idx_report_status` (`status`,`create_time`),
  KEY `fk_report_team` (`team_id`),
  KEY `fk_report_user` (`reporter_user_id`),
  CONSTRAINT `fk_report_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_user` FOREIGN KEY (`reporter_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_request` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL,
  `student_id` int NOT NULL,
  `position_id` int DEFAULT NULL,
  `request_type` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `message` varchar(500) DEFAULT NULL,
  `operator_id` int NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  KEY `idx_request_student_status` (`student_id`,`status`),
  KEY `idx_request_team_status` (`team_id`,`status`),
  KEY `fk_request_position` (`position_id`),
  CONSTRAINT `fk_request_position` FOREIGN KEY (`position_id`) REFERENCES `team_position` (`position_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_request_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_request_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_role_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_role_skill` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '团队角色名称',
  `skill_id` int NOT NULL COMMENT '推荐技能',
  `weight` int NOT NULL DEFAULT '50' COMMENT '推荐权重',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_skill` (`role_name`,`skill_id`),
  KEY `idx_role_skill_role` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队角色与技能对应';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `username` varchar(30) NOT NULL COMMENT '登录账号（唯一）',
  `password` varchar(255) NOT NULL,
  `role` set('student','mentor','admin') NOT NULL COMMENT '用户角色（可多选，用逗号分隔）',
  `student_id` int DEFAULT NULL COMMENT '关联学生表ID（当角色包含student时非空）',
  `mentor_id` int DEFAULT NULL COMMENT '关联导师表ID（当角色包含mentor时非空）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` enum('enabled','disabled') NOT NULL DEFAULT 'enabled' COMMENT '账号状态',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `student_id` (`student_id`),
  KEY `mentor_id` (`mentor_id`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
  CONSTRAINT `user_ibfk_2` FOREIGN KEY (`mentor_id`) REFERENCES `mentor` (`mentor_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_at_least_one_role` CHECK ((`role` <> _utf8mb4'')),
  CONSTRAINT `chk_mentor_role` CHECK ((((find_in_set(_utf8mb4'mentor',`role`) > 0) and (`mentor_id` is not null)) or ((find_in_set(_utf8mb4'mentor',`role`) = 0) and (`mentor_id` is null)))),
  CONSTRAINT `chk_student_role` CHECK ((((find_in_set(_utf8mb4'student',`role`) > 0) and (`student_id` is not null)) or ((find_in_set(_utf8mb4'student',`role`) = 0) and (`student_id` is null))))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户主表（支持多角色，单表实现）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 DROP PROCEDURE IF EXISTS `add_skill_tag_column` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `add_skill_tag_column`(IN column_name_value VARCHAR(64), IN definition_value VARCHAR(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'skill_tag' AND column_name = column_name_value
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `skill_tag` ADD COLUMN `', column_name_value, '` ', definition_value);
    PREPARE statement_value FROM @ddl;
    EXECUTE statement_value;
    DEALLOCATE PREPARE statement_value;
  END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

