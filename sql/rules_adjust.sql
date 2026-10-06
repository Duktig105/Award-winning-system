-- ============================================================
-- 规则调整脚本（按《竞赛技能积分系统规则设计文档》全量对齐）
-- 1. competition 补 category_id 并从竞赛级映射回填
-- 2. competition_skill 唯一键修正（类别默认行与竞赛级行不再互相覆盖）
-- 3. 类别默认映射权重按文档3.2修正（权重和=1.0）
-- 4. honor_score_rule 唯一键切换为 v2.0 参数键 + 特殊系数/校级计分开关参数行
-- 5. award_application 补 member_role / breakthrough（历史性突破认定）列
-- 6. 技能树节点对齐新 8方向 / 50技能体系
-- 可重复执行
-- ============================================================
USE awardsystem;
SET NAMES utf8mb4;

-- ===== 1. competition.category_id =====
ALTER TABLE competition ADD COLUMN IF NOT EXISTS category_id INT DEFAULT NULL COMMENT '所属竞赛类别' AFTER competition_name;

-- 1.1 回填：优先从竞赛级覆盖映射反推（306项目录已全部有竞赛级映射）
UPDATE competition c
JOIN (SELECT competition_id, MAX(category_id) cid FROM competition_skill WHERE competition_id IS NOT NULL GROUP BY competition_id) m
  ON m.competition_id = c.competition_id
SET c.category_id = m.cid
WHERE c.category_id IS NULL;

-- 1.2 关键词兜底（手工新增且未做竞赛级映射的竞赛）
UPDATE competition c JOIN competition_category cc ON cc.category_name='电子设计' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%电子%' OR c.competition_name LIKE '%机器人%' OR c.competition_name LIKE '%嵌入式%' OR c.competition_name LIKE '%智能汽车%' OR c.competition_name LIKE '%ICT%' OR c.competition_name LIKE '%物联网%' OR c.competition_name LIKE '%集成电路%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='数学建模' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%数学建模%' OR c.competition_name LIKE '%统计建模%' OR c.competition_name LIKE '%数据挖掘%' OR c.competition_name LIKE '%数据分析%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='程序设计' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%ACM%' OR c.competition_name LIKE '%ICPC%' OR c.competition_name LIKE '%CCPC%' OR c.competition_name LIKE '%蓝桥%' OR c.competition_name LIKE '%程序设计%' OR c.competition_name LIKE '%算法%' OR c.competition_name LIKE '%软件%' OR c.competition_name LIKE '%信息安全%' OR c.competition_name LIKE '%CTF%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='创新创业' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%创新创业%' OR c.competition_name LIKE '%创业%' OR c.competition_name LIKE '%商业%' OR c.competition_name LIKE '%营销%' OR c.competition_name LIKE '%金融%' OR c.competition_name LIKE '%财会%' OR c.competition_name LIKE '%市场调查%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='科研创新' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%挑战杯%' OR c.competition_name LIKE '%课外学术%' OR c.competition_name LIKE '%实验%' OR c.competition_name LIKE '%力学%' OR c.competition_name LIKE '%大创%' OR c.competition_name LIKE '%生命科学%' OR c.competition_name LIKE '%基础医学%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='视觉设计' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%广告%' OR c.competition_name LIKE '%设计%' OR c.competition_name LIKE '%插画%' OR c.competition_name LIKE '%数字媒体%' OR c.competition_name LIKE '%工业设计%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='文体活动' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%音乐%' OR c.competition_name LIKE '%舞蹈%' OR c.competition_name LIKE '%体育%' OR c.competition_name LIKE '%运动%' OR c.competition_name LIKE '%艺术展演%' OR c.competition_name LIKE '%美术%' OR c.competition_name LIKE '%书法%');
UPDATE competition c JOIN competition_category cc ON cc.category_name='综合素质' SET c.category_id=cc.category_id
WHERE c.category_id IS NULL AND (c.competition_name LIKE '%英语%' OR c.competition_name LIKE '%翻译%' OR c.competition_name LIKE '%演讲%' OR c.competition_name LIKE '%写作%' OR c.competition_name LIKE '%词汇%' OR c.competition_name LIKE '%教学技能%' OR c.competition_name LIKE '%模拟法庭%' OR c.competition_name LIKE '%职业技能%');

-- ===== 2. 唯一键修正（过程保证可重复执行） =====
DROP PROCEDURE IF EXISTS rules_adjust_idx;
DELIMITER $$
CREATE PROCEDURE rules_adjust_idx()
BEGIN
  -- competition_skill：旧键 (category_id, skill_id) 会让竞赛级行覆盖类别默认行
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='competition_skill' AND index_name='uk_competition_skill') THEN
    ALTER TABLE competition_skill DROP INDEX uk_competition_skill;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='competition_skill' AND index_name='uk_cat_comp_skill') THEN
    ALTER TABLE competition_skill ADD UNIQUE KEY uk_cat_comp_skill (category_id, competition_id, skill_id);
  END IF;
  -- honor_score_rule：v1 键 (级别,等次,团队) 阻碍 v2.0 参数行插入，切换为参数键
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='honor_score_rule' AND index_name='uk_rule_level_rank_team') THEN
    ALTER TABLE honor_score_rule DROP INDEX uk_rule_level_rank_team;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='honor_score_rule' AND index_name='uk_rule_param') THEN
    ALTER TABLE honor_score_rule ADD UNIQUE KEY uk_rule_param (rule_version, param_group, param_key);
  END IF;
END$$
DELIMITER ;
CALL rules_adjust_idx();
DROP PROCEDURE rules_adjust_idx;

-- ===== 3. 类别默认映射权重修正（文档3.2，权重和=1.0） =====
-- 文体活动按文档设计无默认映射（音乐舞蹈→艺术表演 / 体育→体育竞技 / 美术书法→美术创作，逐竞赛做竞赛级映射）

-- 程序设计：编程开发50% 算法设计30% 软件工程20%
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.50 WHERE cc.category_name='程序设计' AND st.name='编程开发' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='程序设计' AND st.name='算法设计' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.20 WHERE cc.category_name='程序设计' AND st.name='软件工程' AND cs.competition_id IS NULL;

-- 数学建模：数学建模40% 数据处理与分析30% 学术写作30%
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.40 WHERE cc.category_name='数学建模' AND st.name='数学建模' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='数学建模' AND st.name='数据处理与分析' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='数学建模' AND st.name='学术写作' AND cs.competition_id IS NULL;

-- 创新创业：创意策划35% 商业分析35% 路演表达30%
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.35 WHERE cc.category_name='创新创业' AND st.name='创意策划' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.35 WHERE cc.category_name='创新创业' AND st.name='商业分析' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='创新创业' AND st.name='路演表达' AND cs.competition_id IS NULL;

-- 科研创新：实验设计35% 学术写作35% 文献调研30%
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.35 WHERE cc.category_name='科研创新' AND st.name='实验设计' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.35 WHERE cc.category_name='科研创新' AND st.name='学术写作' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='科研创新' AND st.name='文献调研' AND cs.competition_id IS NULL;

-- 电子设计：电路设计35% 嵌入式开发35% 系统集成30%
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.35 WHERE cc.category_name='电子设计' AND st.name='电路设计' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.35 WHERE cc.category_name='电子设计' AND st.name='嵌入式开发' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='电子设计' AND st.name='系统集成' AND cs.competition_id IS NULL;

-- 视觉设计：视觉表达50% 创意策划30% UI设计20%（移除视频制作默认行）
DELETE cs FROM competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
WHERE cc.category_name='视觉设计' AND st.name='视频制作' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.50 WHERE cc.category_name='视觉设计' AND st.name='视觉表达' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='视觉设计' AND st.name='创意策划' AND cs.competition_id IS NULL;
INSERT INTO competition_skill(category_id,skill_id,contribution,weight)
SELECT cc.category_id,st.skill_id,10,0.20 FROM competition_category cc JOIN skill_tag st ON st.name='UI设计'
WHERE cc.category_name='视觉设计'
  AND NOT EXISTS (SELECT 1 FROM competition_skill x WHERE x.category_id=cc.category_id AND x.skill_id=st.skill_id AND x.competition_id IS NULL);

-- 综合素质：演讲表达40% 英语能力30% 写作能力30%（移除英语翻译默认行）
DELETE cs FROM competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
WHERE cc.category_name='综合素质' AND st.name='英语翻译' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.40 WHERE cc.category_name='综合素质' AND st.name='演讲表达' AND cs.competition_id IS NULL;
UPDATE competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id JOIN skill_tag st ON st.skill_id=cs.skill_id
SET cs.weight=0.30 WHERE cc.category_name='综合素质' AND st.name='英语能力' AND cs.competition_id IS NULL;
INSERT INTO competition_skill(category_id,skill_id,contribution,weight)
SELECT cc.category_id,st.skill_id,10,0.30 FROM competition_category cc JOIN skill_tag st ON st.name='写作能力'
WHERE cc.category_name='综合素质'
  AND NOT EXISTS (SELECT 1 FROM competition_skill x WHERE x.category_id=cc.category_id AND x.skill_id=st.skill_id AND x.competition_id IS NULL);

-- ===== 4. v2.0 参数行（含特殊系数/校级计分开关，幂等） =====
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','竞赛等次基础分','A等','100','目录A等次竞赛的基准分',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','竞赛等次基础分','B等','80','目录B等次竞赛的基准分',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','竞赛等次基础分','C等','60','目录C等次竞赛的基准分',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','竞赛等次基础分','D等','40','目录D等次竞赛的基准分',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','竞赛级别系数','国家级','1.0','目录登记为国家级的竞赛',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','竞赛级别系数','省级','0.7','目录登记为省级的竞赛',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖层次系数','国际级获奖','1.2','国际级阶段/决赛获奖',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖层次系数','与竞赛最高级别一致','1.0','与目录最高级别一致',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖层次系数','低于竞赛最高级别','0.6','低于最高级别一级',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖层次系数','校级','0.2','默认不计分，仅记录',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖等级系数','特等奖/冠军','1.2','含金量高于一等奖',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖等级系数','一等奖/金奖/亚军','1.0',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖等级系数','二等奖/银奖/季军','0.8',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖等级系数','三等奖/铜奖','0.6',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖等级系数','优秀奖/优胜奖/入围奖','0.3',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','获奖等级系数','参与未获奖','0.05','默认关闭，可配置开启',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','团队角色系数','队长/第一完成人','1.0',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','团队角色系数','技术骨干/第2-3完成人','1.0',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','团队角色系数','核心成员','0.8',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','团队角色系数','普通成员','0.6',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','团队角色系数','个人参赛（无团队）','1.0',NULL,1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','特殊系数','常规情况','1.0','默认',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','特殊系数','历史性突破奖','1.5','学校/学院首次获得某竞赛最高奖，管理员手动认定，每竞赛每年最多一次',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','技能经验放大系数','全局','2.0','技能经验值=个人荣誉积分×权重×该系数',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','技能等级阈值','L1 了解','20','经验值≥20',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','技能等级阈值','L2 熟练','100','经验值≥100',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','技能等级阈值','L3 精通','300','经验值≥300',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','技能等级阈值','L4 专家','800','经验值≥800',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','反刷分规则','单竞赛单届次','取最高','同届同作品多次获奖只取最高',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','反刷分规则','单学年积分上限','1000','超出照常记录不计入排名',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','反刷分规则','同类别占比上限','40','单类别积分占比上限40%',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','反刷分规则','异常复核','学期获奖>10项','触发人工复核',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;
INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('','',0,1.00,0,1.00,'v2.0','反刷分规则','校级计分开关','关闭','开启后校级获奖按0.2系数计分',1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark),enabled=1;

-- ===== 5. award_application 扩展列（角色快照 + 历史突破认定） =====
ALTER TABLE award_application
  ADD COLUMN IF NOT EXISTS member_role VARCHAR(20) DEFAULT NULL COMMENT '团队角色快照(队长/技术骨干/核心成员/普通成员,空则按团队信息推导)' AFTER team_id,
  ADD COLUMN IF NOT EXISTS breakthrough TINYINT(1) NOT NULL DEFAULT 0 COMMENT '历史性突破奖标记(特殊系数×1.5,管理员认定)';

-- ===== 6. 技能树对齐 8方向/50技能 =====
-- 6.1 旧体系节点停用（新体系节点与其余节点保留）
UPDATE skill_tree_node n SET n.status='disabled'
WHERE n.status='enabled'
  AND NOT (
    (n.level=1 AND n.name IN ('技术开发','数据科学','电子与智能硬件','研究创新','创新创业','设计表达','文体特长','人文素质'))
    OR (n.level=2 AND EXISTS (SELECT 1 FROM skill_tag st WHERE st.skill_id=n.skill_id AND st.status='enabled'))
  );
-- 6.2 8个方向一级节点
INSERT INTO skill_tree_node(skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status)
SELECT sc.skill_category_id,NULL,NULL,sc.category_name,1,sc.description,NULL,0,sc.sort_order,'enabled'
FROM skill_category sc
WHERE sc.category_name IN ('技术开发','数据科学','电子与智能硬件','研究创新','创新创业','设计表达','文体特长','人文素质')
  AND NOT EXISTS (SELECT 1 FROM skill_tree_node n WHERE n.name=sc.category_name AND n.level=1 AND n.status='enabled');
-- 6.3 50个技能叶子节点（解锁阈值=L1阈值20）
INSERT INTO skill_tree_node(skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status)
SELECT st.skill_category_id,d.node_id,st.skill_id,st.name,2,st.description,st.icon,20,st.sort_order,'enabled'
FROM skill_tag st
JOIN skill_category sc ON sc.skill_category_id=st.skill_category_id
JOIN skill_tree_node d ON d.name=sc.category_name AND d.level=1 AND d.status='enabled'
WHERE st.status='enabled'
  AND NOT EXISTS (SELECT 1 FROM skill_tree_node n WHERE n.skill_id=st.skill_id AND n.level=2 AND n.status='enabled');

-- ===== 7. 校验统计 =====
SELECT '竞赛已归类数' item, COUNT(*) v FROM competition WHERE category_id IS NOT NULL AND catalog_year=2024
UNION ALL SELECT '类别默认映射数', COUNT(*) FROM competition_skill WHERE competition_id IS NULL
UNION ALL SELECT '竞赛级映射数', COUNT(*) FROM competition_skill WHERE competition_id IS NOT NULL
UNION ALL SELECT 'v2.0参数行数', COUNT(*) FROM honor_score_rule WHERE param_group IS NOT NULL AND enabled=1
UNION ALL SELECT '技能树新节点数', COUNT(*) FROM skill_tree_node WHERE status='enabled';
