-- =====================================================================
-- 证书查重 / OCR预检 / 风险规则 / 团队证书例外 / 学生Excel导入 模块迁移脚本
-- 执行方式: mysql -u<user> -p awardsystem < certificate_risk_migration.sql
-- =====================================================================
USE awardsystem;

-- 1. 证书文件指纹表（SHA-256 + pHash）
CREATE TABLE IF NOT EXISTS `certificate_fingerprint` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书文件指纹表（查重基础数据）';

-- 2. 证书查重记录表（每次查重的结果）
CREATE TABLE IF NOT EXISTS `duplicate_check_record` (
  `check_id` int NOT NULL AUTO_INCREMENT COMMENT '查重记录ID（主键）',
  `application_id` int NOT NULL COMMENT '本次查重关联的申请ID',
  `file_id` int NOT NULL COMMENT '本次查重的文件ID',
  `sha256` char(64) DEFAULT NULL COMMENT '文件SHA-256',
  `phash` varchar(16) DEFAULT NULL COMMENT '文件pHash',
  `exact_duplicate` tinyint NOT NULL DEFAULT 0 COMMENT '是否完全重复（0否/1是）',
  `similar_count` int NOT NULL DEFAULT 0 COMMENT '相似历史证书数量',
  `max_similarity` decimal(5,4) DEFAULT NULL COMMENT '最高相似度（0~1）',
  `check_status` varchar(20) NOT NULL DEFAULT 'done' COMMENT '查重状态（pending/running/done/failed）',
  `check_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查重时间',
  PRIMARY KEY (`check_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书查重结果记录表';

-- 3. 证书相似匹配明细表（一对多：一次查重命中多条历史）
CREATE TABLE IF NOT EXISTS `duplicate_match` (
  `match_id` int NOT NULL AUTO_INCREMENT COMMENT '匹配ID（主键）',
  `check_id` int NOT NULL COMMENT '关联查重记录ID',
  `application_id` int NOT NULL COMMENT '发起查重的申请ID',
  `file_id` int NOT NULL COMMENT '发起查重的文件ID',
  `matched_application_id` int NOT NULL COMMENT '被匹配到的历史申请ID',
  `matched_file_id` int NOT NULL COMMENT '被匹配到的历史文件ID',
  `matched_student_id` int DEFAULT NULL COMMENT '被匹配申请的申请人学生ID',
  `match_type` varchar(20) NOT NULL COMMENT '匹配类型（exact-完全相同/phash-感知相似）',
  `similarity` decimal(5,4) NOT NULL COMMENT '相似度（exact为1.0000）',
  `team_related` tinyint NOT NULL DEFAULT 0 COMMENT '双方是否同一团队成员（0否/1是）',
  `handled` tinyint NOT NULL DEFAULT 0 COMMENT '是否已被人工处理（避免反复报警）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`match_id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_matched_application_id` (`matched_application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书相似匹配明细表';

-- 4. OCR识别记录表
CREATE TABLE IF NOT EXISTS `ocr_record` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OCR证书识别记录表';

-- 5. 风险规则配置表
CREATE TABLE IF NOT EXISTS `risk_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT COMMENT '规则ID（主键）',
  `rule_key` varchar(50) NOT NULL COMMENT '规则标识（唯一）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `rule_type` varchar(20) NOT NULL DEFAULT 'flag' COMMENT '规则类型（flag-开关型/threshold-阈值型）',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值（阈值型规则使用）',
  `threshold_unit` varchar(50) DEFAULT NULL COMMENT '阈值说明（如：相似度0~1）',
  `config_json` varchar(1000) DEFAULT NULL COMMENT '扩展配置JSON（如OCR比对字段列表）',
  `risk_level` varchar(20) NOT NULL DEFAULT 'medium' COMMENT '触发后的风险等级（high/medium/low）',
  `need_manual_review` tinyint NOT NULL DEFAULT 1 COMMENT '触发后是否需要人工复核（0/1）',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用（0/1）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风险规则配置表';

-- 默认风险规则
INSERT INTO `risk_rule` (`rule_key`, `rule_name`, `description`, `rule_type`, `threshold_value`, `threshold_unit`, `config_json`, `risk_level`, `need_manual_review`, `enabled`) VALUES
('EXACT_DUPLICATE', '完全相同文件', '证书文件SHA-256完全相同（非同团队成员）时触发', 'flag', NULL, NULL, NULL, 'high', 1, 1),
('PHASH_SIMILAR', 'pHash相似度阈值', '证书图片感知哈希相似度达到阈值（非同团队成员）时触发', 'threshold', 0.90, '相似度0~1，越大越严格', NULL, 'high', 1, 1),
('NAME_MISMATCH', '姓名不一致', 'OCR识别姓名与申报人姓名不一致时触发', 'flag', NULL, NULL, NULL, 'high', 1, 1),
('AWARD_MISMATCH', '奖项不一致', 'OCR识别获奖等级与申报获奖等级不一致时触发', 'flag', NULL, NULL, NULL, 'medium', 1, 1),
('TIME_MISMATCH', '时间不一致', 'OCR识别获奖时间与申报获奖时间不一致时触发', 'flag', NULL, NULL, NULL, 'medium', 1, 1),
('CERT_NO_DUPLICATE', '证书编号重复', 'OCR识别的证书编号与其他申请重复时触发', 'flag', NULL, NULL, NULL, 'high', 1, 1),
('OCR_LOW_CONFIDENCE', 'OCR识别置信度阈值', 'OCR整体置信度低于阈值或识别失败时触发（转人工审核）', 'threshold', 0.80, '置信度0~1', NULL, 'medium', 1, 1),
('OCR_COMPARE_FIELDS', 'OCR比对字段配置', '控制OCR预检参与比对的申报字段', 'flag', NULL, NULL, '["name","competition","awardLevel","awardTime"]', 'low', 0, 1)
ON DUPLICATE KEY UPDATE `rule_name` = VALUES(`rule_name`);

-- 6. 申请风险与人工处理表
CREATE TABLE IF NOT EXISTS `application_risk` (
  `risk_id` int NOT NULL AUTO_INCREMENT COMMENT '风险ID（主键）',
  `application_id` int NOT NULL COMMENT '关联申请ID',
  `risk_level` varchar(20) NOT NULL DEFAULT 'none' COMMENT '综合风险等级（high/medium/low/none）',
  `need_manual_review` tinyint NOT NULL DEFAULT 0 COMMENT '是否需要人工复核（0/1）',
  `risk_reasons` text COMMENT '风险原因JSON数组',
  `manual_mark` varchar(20) DEFAULT NULL COMMENT '人工标记（normal_reuse-正常复用/abnormal_duplicate-异常重复/undetermined-无法判断）',
  `manual_remark` varchar(500) DEFAULT NULL COMMENT '人工处理备注',
  `manual_reviewer` varchar(50) DEFAULT NULL COMMENT '审核人用户名',
  `manual_time` datetime DEFAULT NULL COMMENT '人工标记时间',
  `final_opinion` varchar(500) DEFAULT NULL COMMENT '审核人员最终意见',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`risk_id`),
  UNIQUE KEY `uk_application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申请风险评估与人工处理结果表';

-- 7. 学生导入记录表
CREATE TABLE IF NOT EXISTS `student_import_record` (
  `import_id` int NOT NULL AUTO_INCREMENT COMMENT '导入ID（主键）',
  `file_name` varchar(255) DEFAULT NULL COMMENT '上传的文件名',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总行数',
  `insert_count` int NOT NULL DEFAULT 0 COMMENT '新增数量',
  `update_count` int NOT NULL DEFAULT 0 COMMENT '更新数量',
  `skip_count` int NOT NULL DEFAULT 0 COMMENT '跳过数量（无变化）',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数量',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `import_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  PRIMARY KEY (`import_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生Excel导入记录表';

-- 8. 学生导入明细表
CREATE TABLE IF NOT EXISTS `student_import_detail` (
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
