-- ====================================================================
-- 获奖系统扩展模块数据库迁移脚本
-- 涵盖：竞赛类别、技能标签、技能关联、学生技能档案、荣誉标签、积分、勋章、技能树
-- 创建时间：2026-09-29
-- 不破坏已有数据，使用 IF NOT EXISTS / 条件检查 安全迁移
-- ====================================================================

USE awardsystem;

-- 1. 竞赛类别表
CREATE TABLE IF NOT EXISTS competition_category (
  category_id INT NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  category_name VARCHAR(50) NOT NULL COMMENT '类别名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '类别说明',
  icon VARCHAR(200) DEFAULT NULL COMMENT '类别图标(URL或SVG名)',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '类别排序',
  status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态 enabled/disabled',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_category_name (category_name),
  KEY idx_category_status (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛类别表';

-- 2. 技能方向表（编程、数学建模等）
CREATE TABLE IF NOT EXISTS skill_category (
  skill_category_id INT NOT NULL AUTO_INCREMENT COMMENT '技能方向ID',
  category_name VARCHAR(50) NOT NULL COMMENT '方向名称',
  parent_id INT DEFAULT NULL COMMENT '父级方向ID(预留多级)',
  description VARCHAR(500) DEFAULT NULL COMMENT '方向说明',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_category_id),
  UNIQUE KEY uk_skill_category_name (category_name),
  KEY idx_skill_category_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能方向表';

-- 3. 扩展技能标签 skill_tag（首次执行；若重复执行报 Duplicate column 可忽略对应行）
ALTER TABLE `skill_tag` ADD COLUMN `skill_category_id` INT DEFAULT NULL COMMENT '所属技能方向ID';
ALTER TABLE `skill_tag` ADD COLUMN `description` VARCHAR(500) DEFAULT NULL COMMENT '技能说明';
ALTER TABLE `skill_tag` ADD COLUMN `level` VARCHAR(20) NOT NULL DEFAULT 'basic' COMMENT '技能等级 basic/intermediate/advanced';
ALTER TABLE `skill_tag` ADD COLUMN `icon` VARCHAR(200) DEFAULT NULL COMMENT '技能图标';
ALTER TABLE `skill_tag` ADD COLUMN `allow_self_eval` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许学生自评';
ALTER TABLE `skill_tag` ADD COLUMN `award_only` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否只能由获奖记录验证';
ALTER TABLE `skill_tag` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态 enabled/disabled';
ALTER TABLE `skill_tag` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序';
ALTER TABLE `skill_tag` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 4. 扩展学生技能 student_skill
ALTER TABLE `student_skill` ADD COLUMN `self_eval_level` VARCHAR(20) NOT NULL DEFAULT 'beginner' COMMENT '自评等级 beginner/intermediate/advanced';
ALTER TABLE `student_skill` ADD COLUMN `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已验证';
ALTER TABLE `student_skill` ADD COLUMN `experience` INT NOT NULL DEFAULT 0 COMMENT '技能经验值';
ALTER TABLE `student_skill` ADD COLUMN `source` VARCHAR(50) NOT NULL DEFAULT 'self_eval' COMMENT '技能来源 self_eval/award/team';
ALTER TABLE `student_skill` ADD COLUMN `application_id` INT DEFAULT NULL COMMENT '来源获奖申请ID';
ALTER TABLE `student_skill` ADD COLUMN `note` VARCHAR(255) DEFAULT NULL COMMENT '备注';
ALTER TABLE `student_skill` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 5. 竞赛与技能关联表（技能贡献值、升级规则、团队角色）
CREATE TABLE IF NOT EXISTS competition_skill (
  id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id INT NOT NULL COMMENT '竞赛类别ID',
  skill_id INT NOT NULL COMMENT '技能ID',
  contribution INT NOT NULL DEFAULT 10 COMMENT '技能贡献值(每次获奖+经验)',
  upgrade_rule VARCHAR(500) DEFAULT NULL COMMENT '技能升级规则(JSON简版描述)',
  team_role VARCHAR(100) DEFAULT NULL COMMENT '团队角色(如前端负责人)',
  team_role_weight INT NOT NULL DEFAULT 50 COMMENT '该技能在团队角色中的贡献权重',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_competition_skill (category_id, skill_id),
  KEY idx_competition_skill_category (category_id),
  KEY idx_competition_skill_skill (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛与技能对应关系';

-- 6. 积分规则表
CREATE TABLE IF NOT EXISTS honor_score_rule (
  rule_id INT NOT NULL AUTO_INCREMENT,
  competition_level VARCHAR(50) NOT NULL COMMENT '竞赛级别(国家级/省级/校级/院级)',
  award_rank VARCHAR(20) NOT NULL COMMENT '获奖等次(A/B/C/D)',
  base_score INT NOT NULL DEFAULT 0 COMMENT '基础分',
  award_ratio DECIMAL(5,2) NOT NULL DEFAULT '1.00' COMMENT '获奖系数',
  is_team TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否团队',
  team_ratio DECIMAL(5,2) NOT NULL DEFAULT '1.00' COMMENT '团队系数',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  enable_time DATETIME DEFAULT NULL COMMENT '启用时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_rule_level_rank_team (competition_level, award_rank, is_team),
  KEY idx_rule_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='荣誉积分规则表';

-- 7. 学生积分总表
CREATE TABLE IF NOT EXISTS student_honor_score (
  id INT NOT NULL AUTO_INCREMENT,
  student_id INT NOT NULL COMMENT '学生ID',
  total_score INT NOT NULL DEFAULT 0 COMMENT '当前总积分',
  level_score INT NOT NULL DEFAULT 0 COMMENT '当年/学期累计积分(可清零)',
  last_calc_time DATETIME DEFAULT NULL COMMENT '上次计算时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_score (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生积分总表';

-- 8. 学生积分明细表
CREATE TABLE IF NOT EXISTS student_honor_score_log (
  log_id INT NOT NULL AUTO_INCREMENT,
  student_id INT NOT NULL COMMENT '学生ID',
  application_id INT DEFAULT NULL COMMENT '获奖申请ID',
  score INT NOT NULL DEFAULT 0 COMMENT '积分(可负)',
  score_type VARCHAR(50) NOT NULL DEFAULT 'award' COMMENT '积分类型 award/manual/deduct',
  description VARCHAR(500) DEFAULT NULL COMMENT '积分描述',
  rule_snapshot VARCHAR(500) DEFAULT NULL COMMENT '规则快照',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_log_student_time (student_id, create_time),
  KEY idx_log_application (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生积分明细';

-- 9. 荣誉标签表
CREATE TABLE IF NOT EXISTS honor_tag (
  tag_id INT NOT NULL AUTO_INCREMENT,
  tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
  tag_type VARCHAR(50) NOT NULL COMMENT '标签类型(award_level/competition_direction/role/growth/ability)',
  description VARCHAR(500) DEFAULT NULL COMMENT '标签说明',
  condition_rule VARCHAR(1000) NOT NULL COMMENT '生成条件(JSON)',
  icon VARCHAR(200) DEFAULT NULL COMMENT '标签图标',
  status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tag_id),
  UNIQUE KEY uk_tag_name_type (tag_name, tag_type),
  KEY idx_tag_type_status (tag_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='荣誉标签配置表';

-- 10. 学生荣誉标签关联
CREATE TABLE IF NOT EXISTS student_honor_tag (
  id INT NOT NULL AUTO_INCREMENT,
  student_id INT NOT NULL,
  tag_id INT NOT NULL,
  awarded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_tag (student_id, tag_id),
  KEY idx_student_tag_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生获得的荣誉标签';

-- 11. 勋章表
CREATE TABLE IF NOT EXISTS badge (
  badge_id INT NOT NULL AUTO_INCREMENT,
  badge_code VARCHAR(50) NOT NULL COMMENT '勋章代码',
  badge_name VARCHAR(50) NOT NULL COMMENT '勋章名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '勋章说明',
  icon VARCHAR(200) DEFAULT NULL COMMENT '图标',
  unlock_condition VARCHAR(1000) NOT NULL COMMENT '解锁条件JSON',
  unlock_threshold INT NOT NULL DEFAULT 1 COMMENT '解锁阈值',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  status VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (badge_id),
  UNIQUE KEY uk_badge_code (badge_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勋章配置表';

-- 12. 学生勋章
CREATE TABLE IF NOT EXISTS student_badge (
  id INT NOT NULL AUTO_INCREMENT,
  student_id INT NOT NULL,
  badge_id INT NOT NULL,
  progress INT NOT NULL DEFAULT 0 COMMENT '当前进度',
  unlocked_at DATETIME DEFAULT NULL COMMENT '解锁时间',
  is_representative TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否代表勋章',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_badge (student_id, badge_id),
  KEY idx_student_badge_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生勋章';

-- 13. 技能树节点表（方向→一级节点→二级节点）
CREATE TABLE IF NOT EXISTS skill_tree_node (
  node_id INT NOT NULL AUTO_INCREMENT,
  skill_category_id INT DEFAULT NULL COMMENT '所属技能方向',
  parent_id INT DEFAULT NULL COMMENT '父节点ID(顶级为NULL)',
  skill_id INT DEFAULT NULL COMMENT '关联技能ID(叶子节点)',
  name VARCHAR(50) NOT NULL COMMENT '节点名称',
  level TINYINT NOT NULL DEFAULT 1 COMMENT '节点层级 1/2/3',
  description VARCHAR(500) DEFAULT NULL,
  icon VARCHAR(200) DEFAULT NULL,
  unlock_threshold INT NOT NULL DEFAULT 0 COMMENT '解锁阈值(经验值)',
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (node_id),
  KEY idx_node_parent (parent_id),
  KEY idx_node_skill_category (skill_category_id),
  KEY idx_node_skill (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能树节点';

-- 14. 团队角色与技能对应
CREATE TABLE IF NOT EXISTS team_role_skill (
  id INT NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL COMMENT '团队角色名称',
  skill_id INT NOT NULL COMMENT '推荐技能',
  weight INT NOT NULL DEFAULT 50 COMMENT '推荐权重',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_skill (role_name, skill_id),
  KEY idx_role_skill_role (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队角色与技能对应';

-- ============================================================
-- 初始化数据：竞赛类别
-- ============================================================
INSERT IGNORE INTO competition_category(category_name, description, icon, sort_order) VALUES
('程序设计', '编程与算法类竞赛，如ACM、蓝桥杯等', 'monitor', 1),
('数学建模', '数学分析与建模类竞赛，如国赛、美赛', 'data-analysis', 2),
('创新创业', '创业类项目展示与路演竞赛', 'magic-stick', 3),
('科研创新', '科研课题与论文竞赛', 'reading', 4),
('电子设计', '嵌入式、电子电路设计类竞赛', 'cpu', 5),
('视觉设计', 'UI设计、海报、视频与视觉表达类', 'picture', 6),
('文体活动', '文艺、体育活动与竞赛', 'trophy', 7),
('综合素质', '综合能力类，含演讲、英语等', 'star', 8);

-- ============================================================
-- 初始化数据：技能方向
-- ============================================================
INSERT IGNORE INTO skill_category(category_name, description, sort_order) VALUES
('编程与开发', '软件开发、算法与项目实现', 1),
('数学分析', '数学建模与数据分析', 2),
('设计与创意', '创意策划与视觉表达', 3),
('商业与表达', '商业分析与路演表达', 4),
('科研能力', '科研课题与论文撰写', 5),
('电子硬件', '电子电路与嵌入式开发', 6);

-- ============================================================
-- 初始化数据：技能标签
-- ============================================================
INSERT IGNORE INTO skill_tag(name, skill_category_id, description, level, allow_self_eval, award_only, sort_order) VALUES
('编程', 1, '编程语言与代码实现能力', 'basic', 1, 0, 1),
('算法', 1, '算法设计与优化能力', 'basic', 1, 0, 2),
('项目开发', 1, '完整项目的设计与开发', 'intermediate', 1, 0, 3),
('数学分析', 2, '数学建模与统计分析', 'intermediate', 1, 0, 4),
('数据处理', 2, '数据清洗与可视化', 'basic', 1, 0, 5),
('论文撰写', 2, '学术论文撰写', 'intermediate', 1, 0, 6),
('创意策划', 3, '创意构思与方案策划', 'basic', 1, 0, 7),
('商业分析', 4, '商业模式与市场分析', 'intermediate', 1, 0, 8),
('路演表达', 4, '现场路演与答辩', 'basic', 1, 0, 9),
('视觉表达', 3, '平面与视觉设计', 'basic', 1, 0, 10),
('UI设计', 3, '界面与交互设计', 'intermediate', 1, 0, 11),
('视频制作', 3, '视频拍摄与剪辑', 'basic', 1, 0, 12);

-- ============================================================
-- 初始化数据：竞赛与技能关联
-- ============================================================
INSERT IGNORE INTO competition_skill(category_id, skill_id, contribution, team_role)
SELECT cc.category_id, st.skill_id, 10, NULL FROM (
    SELECT '程序设计' category_name, '编程' skill_name UNION ALL
    SELECT '程序设计', '算法' UNION ALL
    SELECT '程序设计', '项目开发' UNION ALL
    SELECT '数学建模', '数学分析' UNION ALL
    SELECT '数学建模', '数据处理' UNION ALL
    SELECT '数学建模', '论文撰写' UNION ALL
    SELECT '创新创业', '创意策划' UNION ALL
    SELECT '创新创业', '商业分析' UNION ALL
    SELECT '创新创业', '路演表达' UNION ALL
    SELECT '视觉设计', 'UI设计' UNION ALL
    SELECT '视觉设计', '视觉表达' UNION ALL
    SELECT '视觉设计', '视频制作'
  ) m JOIN competition_category cc ON cc.category_name=m.category_name
    JOIN skill_tag st ON st.name=m.skill_name;

-- ============================================================
-- 初始化数据：积分规则
-- ============================================================
INSERT IGNORE INTO honor_score_rule(competition_level, award_rank, base_score, award_ratio, is_team, team_ratio, enabled) VALUES
('国家级', 'A', 100, 1.50, 0, 1.00, 1),
('国家级', 'B', 100, 1.20, 0, 1.00, 1),
('国家级', 'C', 100, 1.00, 0, 1.00, 1),
('国家级', 'D', 100, 0.70, 0, 1.00, 1),
('省级',   'A', 60,  1.50, 0, 1.00, 1),
('省级',   'B', 60,  1.20, 0, 1.00, 1),
('省级',   'C', 60,  1.00, 0, 1.00, 1),
('省级',   'D', 60,  0.70, 0, 1.00, 1),
('校级',   'A', 30,  1.50, 0, 1.00, 1),
('校级',   'B', 30,  1.20, 0, 1.00, 1),
('校级',   'C', 30,  1.00, 0, 1.00, 1),
('校级',   'D', 30,  0.70, 0, 1.00, 1),
('院级',   'A', 15,  1.50, 0, 1.00, 1),
('院级',   'B', 15,  1.20, 0, 1.00, 1),
('院级',   'C', 15,  1.00, 0, 1.00, 1),
('院级',   'D', 15,  0.70, 0, 1.00, 1);

INSERT IGNORE INTO honor_score_rule(competition_level, award_rank, base_score, award_ratio, is_team, team_ratio, enabled) VALUES
('国家级', 'A', 100, 1.50, 1, 0.80, 1),
('国家级', 'B', 100, 1.20, 1, 0.80, 1),
('国家级', 'C', 100, 1.00, 1, 0.80, 1),
('国家级', 'D', 100, 0.70, 1, 0.80, 1),
('省级',   'A', 60,  1.50, 1, 0.80, 1),
('省级',   'B', 60,  1.20, 1, 0.80, 1),
('省级',   'C', 60,  1.00, 1, 0.80, 1),
('省级',   'D', 60,  0.70, 1, 0.80, 1);

-- ============================================================
-- 初始化数据：勋章(8枚)
-- ============================================================
INSERT IGNORE INTO badge(badge_code, badge_name, description, icon, unlock_condition, unlock_threshold, sort_order) VALUES
('first_award', '首次获奖', '完成第一次获奖申请并通过审核', 'star-on', '{"type":"first_award"}', 1, 1),
('national_honor', '国家荣誉', '获得国家级竞赛奖项', 'medal', '{"type":"award_level","competition_level":"国家级"}', 1, 2),
('provincial_expert', '省赛达人', '获得3次省级及以上奖项', 'trophy', '{"type":"award_count","competition_level":"省级","min_count":3}', 3, 3),
('team_star', '团队之星', '以队长身份参与团队获奖', 'user', '{"type":"team_leader_award"}', 1, 4),
('versatile', '多面手', '在3个不同竞赛类别获得奖项', 'collection', '{"type":"category_count","min_count":3}', 3, 5),
('pioneer', '竞赛领航员', '累计获得5个奖项', 'guide', '{"type":"award_count","min_count":5}', 5, 6),
('golden_partner', '黄金搭档', '在同一团队获奖3次', 'handshake', '{"type":"team_partner_count","min_count":3}', 3, 7),
('continuous_growth', '持续进步', '连续3个不同学期有获奖记录', 'aim', '{"type":"continuous_semesters","min_count":3}', 3, 8);

-- ============================================================
-- 初始化数据：荣誉标签
-- ============================================================
INSERT IGNORE INTO honor_tag(tag_name, tag_type, description, condition_rule, sort_order) VALUES
('国家级一等奖', 'award_level', '获得国家级竞赛一等奖', '{"type":"award_rank","award_rank":"A","competition_level":"国家级"}', 1),
('国家级二等奖', 'award_level', '获得国家级竞赛二等奖', '{"type":"award_rank","award_rank":"B","competition_level":"国家级"}', 2),
('国家级三等奖', 'award_level', '获得国家级竞赛三等奖', '{"type":"award_rank","award_rank":"C","competition_level":"国家级"}', 3),
('省级一等奖', 'award_level', '获得省级竞赛一等奖', '{"type":"award_rank","award_rank":"A","competition_level":"省级"}', 4),
('省级二等奖', 'award_level', '获得省级竞赛二等奖', '{"type":"award_rank","award_rank":"B","competition_level":"省级"}', 5),
('省级三等奖', 'award_level', '获得省级竞赛三等奖', '{"type":"award_rank","award_rank":"C","competition_level":"省级"}', 6),
('校级一等奖', 'award_level', '获得校级竞赛一等奖', '{"type":"award_rank","award_rank":"A","competition_level":"校级"}', 7),
('校级二等奖', 'award_level', '获得校级竞赛二等奖', '{"type":"award_rank","award_rank":"B","competition_level":"校级"}', 8),
('校级三等奖', 'award_level', '获得校级竞赛三等奖', '{"type":"award_rank","award_rank":"C","competition_level":"校级"}', 9),
('程序设计新秀', 'competition_direction', '在程序设计方向有获奖记录', '{"type":"category_has_award","category_name":"程序设计"}', 4),
('数学建模达人', 'competition_direction', '在数学建模方向有获奖记录', '{"type":"category_has_award","category_name":"数学建模"}', 5),
('创新创业先锋', 'competition_direction', '在创新创业方向有获奖记录', '{"type":"category_has_award","category_name":"创新创业"}', 6),
('科研小将', 'competition_direction', '在科研创新方向有获奖记录', '{"type":"category_has_award","category_name":"科研创新"}', 7),
('电子设计专家', 'competition_direction', '在电子设计方向有获奖记录', '{"type":"category_has_award","category_name":"电子设计"}', 8),
('视觉创意达人', 'competition_direction', '在视觉设计方向有获奖记录', '{"type":"category_has_award","category_name":"视觉设计"}', 9),
('团队领袖', 'role', '以队长身份获得团队奖项', '{"type":"team_leader_award"}', 10),
('技术骨干', 'ability', '具备3项以上已验证技能', '{"type":"verified_skill_count","min_count":3}', 11),
('新星崛起', 'growth', '入学第一年获得奖项', '{"type":"first_year_award"}', 12);

-- ============================================================
-- 初始化数据：技能树（示例：程序设计方向）
-- ============================================================
-- 顶级节点：程序设计
INSERT IGNORE INTO skill_tree_node(skill_category_id, parent_id, name, level, description, sort_order) VALUES
(1, NULL, '编程基础', 1, '掌握基础编程语法与开发工具', 1),
(1, NULL, '算法能力', 1, '数据结构与算法设计能力', 2),
(1, NULL, '项目工程', 1, '完整项目开发与协作能力', 3);

-- 二级节点：编程基础下（用变量获取父节点ID，避免复杂子查询）
SET @prog_base := (SELECT node_id FROM skill_tree_node WHERE name='编程基础' AND level=1 LIMIT 1);
INSERT INTO skill_tree_node(parent_id, skill_category_id, skill_id, name, level, description, unlock_threshold, sort_order)
SELECT @prog_base, 1, st.skill_id, m.name, 2, m.description, m.threshold, m.sort_order FROM (
    SELECT '编程入门' name, '掌握至少一门编程语言基础语法' description, 0 threshold, 1 sort_order
    UNION ALL SELECT '面向对象', '掌握面向对象编程思想', 30, 2
    UNION ALL SELECT '项目开发', '能独立完成小型项目', 60, 3
) m LEFT JOIN skill_tag st ON st.name='编程' LIMIT 3;
UPDATE skill_tree_node SET skill_id = (SELECT skill_id FROM skill_tag WHERE name='编程' LIMIT 1)
WHERE parent_id = @prog_base AND skill_id IS NULL;

SET @algo_base := (SELECT node_id FROM skill_tree_node WHERE name='算法能力' AND level=1 LIMIT 1);
INSERT INTO skill_tree_node(parent_id, skill_category_id, skill_id, name, level, description, unlock_threshold, sort_order)
SELECT @algo_base, 1, st.skill_id, m.name, 2, m.description, m.threshold, m.sort_order FROM (
    SELECT '排序与搜索' name, '基础排序与查找算法' description, 10 threshold, 1 sort_order
    UNION ALL SELECT '动态规划', '动态规划思想与应用', 80, 2
) m LEFT JOIN skill_tag st ON st.name='算法';
UPDATE skill_tree_node SET skill_id = (SELECT skill_id FROM skill_tag WHERE name='算法' LIMIT 1)
WHERE parent_id = @algo_base AND skill_id IS NULL;

SET @proj_base := (SELECT node_id FROM skill_tree_node WHERE name='项目工程' AND level=1 LIMIT 1);
INSERT INTO skill_tree_node(parent_id, skill_category_id, skill_id, name, level, description, unlock_threshold, sort_order)
SELECT @proj_base, 1, st.skill_id, m.name, 2, m.description, m.threshold, m.sort_order FROM (
    SELECT '项目协作' name, '参与团队项目并完成交付' description, 20 threshold, 1 sort_order
    UNION ALL SELECT '独立项目', '独立设计并交付完整项目', 120, 2
) m LEFT JOIN skill_tag st ON st.name='项目开发';
UPDATE skill_tree_node SET skill_id = (SELECT skill_id FROM skill_tag WHERE name='项目开发' LIMIT 1)
WHERE parent_id = @proj_base AND skill_id IS NULL;

-- ============================================================
-- 初始化完成
-- ============================================================