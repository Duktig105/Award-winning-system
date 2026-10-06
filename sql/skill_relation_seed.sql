-- 补充技能库与竞赛类别-技能关联（可重复执行）

USE awardsystem;

-- 电子硬件方向新增技能
INSERT IGNORE INTO skill_tag(name, skill_category_id, description, level, allow_self_eval, award_only, status, sort_order)
SELECT '电路设计', 6, '模拟/数字电路设计与仿真', 'basic', 1, 0, 'enabled', 1
WHERE NOT EXISTS (SELECT 1 FROM skill_tag WHERE name='电路设计');
INSERT IGNORE INTO skill_tag(name, skill_category_id, description, level, allow_self_eval, award_only, status, sort_order)
SELECT '嵌入式开发', 6, '单片机与嵌入式系统开发', 'intermediate', 1, 0, 'enabled', 2
WHERE NOT EXISTS (SELECT 1 FROM skill_tag WHERE name='嵌入式开发');
INSERT IGNORE INTO skill_tag(name, skill_category_id, description, level, allow_self_eval, award_only, status, sort_order)
SELECT '单片机应用', 6, '常用单片机编程与调试', 'intermediate', 1, 0, 'enabled', 3
WHERE NOT EXISTS (SELECT 1 FROM skill_tag WHERE name='单片机应用');

-- 竞赛类别与技能关联（按类别名 + 技能名定位）
INSERT IGNORE INTO competition_skill(category_id, skill_id, contribution, team_role, team_role_weight)
SELECT cc.category_id, st.skill_id, 10, NULL, 0
FROM competition_category cc JOIN skill_tag st
WHERE (cc.category_name='电子设计' AND st.name IN ('电路设计','嵌入式开发','单片机应用'))
   OR (cc.category_name='科研创新' AND st.name IN ('论文撰写','数据处理','数学分析'))
   OR (cc.category_name='文体活动' AND st.name IN ('视觉表达','视频制作'))
   OR (cc.category_name='综合素质' AND st.name IN ('路演表达','商业分析'));

SELECT cc.category_name, st.name, cs.contribution
FROM competition_skill cs
JOIN competition_category cc ON cc.category_id=cs.category_id
JOIN skill_tag st ON st.skill_id=cs.skill_id
ORDER BY cc.sort_order, st.name;