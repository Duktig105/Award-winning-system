-- ============================================================
-- 荣誉标签细分：一等奖/二等奖/三等奖 按竞赛级别（国家级/省级/校级）拆分
-- 旧标签：一等奖 / 二等奖 / 三等奖（不分级别）
-- 新标签：国家级一等奖 ... 校级三等奖（共 9 个）
-- ============================================================

-- 1. 清理旧的不分级别标签及其学生关联
DELETE FROM student_honor_tag
WHERE tag_id IN (SELECT tag_id FROM honor_tag WHERE tag_type='award_level' AND tag_name IN ('一等奖','二等奖','三等奖'));

DELETE FROM honor_tag
WHERE tag_type='award_level' AND tag_name IN ('一等奖','二等奖','三等奖');

-- 2. 插入按级别细分的新标签（等次 A/B/C = 一/二/三等奖）
INSERT IGNORE INTO honor_tag(tag_name, tag_type, description, condition_rule, sort_order) VALUES
('国家级一等奖', 'award_level', '获得国家级竞赛一等奖', '{"type":"award_rank","award_rank":"A","competition_level":"国家级"}', 1),
('国家级二等奖', 'award_level', '获得国家级竞赛二等奖', '{"type":"award_rank","award_rank":"B","competition_level":"国家级"}', 2),
('国家级三等奖', 'award_level', '获得国家级竞赛三等奖', '{"type":"award_rank","award_rank":"C","competition_level":"国家级"}', 3),
('省级一等奖', 'award_level', '获得省级竞赛一等奖', '{"type":"award_rank","award_rank":"A","competition_level":"省级"}', 4),
('省级二等奖', 'award_level', '获得省级竞赛二等奖', '{"type":"award_rank","award_rank":"B","competition_level":"省级"}', 5),
('省级三等奖', 'award_level', '获得省级竞赛三等奖', '{"type":"award_rank","award_rank":"C","competition_level":"省级"}', 6),
('校级一等奖', 'award_level', '获得校级竞赛一等奖', '{"type":"award_rank","award_rank":"A","competition_level":"校级"}', 7),
('校级二等奖', 'award_level', '获得校级竞赛二等奖', '{"type":"award_rank","award_rank":"B","competition_level":"校级"}', 8),
('校级三等奖', 'award_level', '获得校级竞赛三等奖', '{"type":"award_rank","award_rank":"C","competition_level":"校级"}', 9);

-- 3. 其余标签排序顺延
UPDATE honor_tag SET sort_order = sort_order + 8 WHERE tag_type <> 'award_level' AND sort_order > 3;
