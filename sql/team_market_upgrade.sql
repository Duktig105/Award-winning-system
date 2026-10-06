-- ============================================================
-- 组队模块升级：团队氛围标签 + 广场满员下架（2026-10-06）
-- 执行库：awardsystem
-- ============================================================

-- 1) 团队氛围标签（vibe tags）
--    前端以逗号分隔字符串存取：列表读取 split(',')，创建/编辑提交 join(',')
ALTER TABLE team ADD COLUMN vibe_tags VARCHAR(200) NULL AFTER version;

-- 2) 满员自动下架说明（无需结构变更，逻辑在 TeamService.acceptMember 中）：
--    队长“同意”使队伍满员（目标人数已满 或 所有开放岗位招满）时：
--      UPDATE team SET recruiting=0, status='closed'
--      并把该队所有 pending 请求置为 expired，写 team_audit_log(action='TEAM_FULL_DELIST')
--    下架是持久化动作，成员后续退出不会自动重新上架，需队长在“我的队伍”手动重开招募。

-- 3) 广场可见性口径（marketplace 默认查询条件，无结构变更）：
--    仅当 队伍 recruiting=1 且 status='recruiting'
--      且 已接受成员数 < LEAST(target_size, max_team_size)
--      且 存在“占位数 < 名额”的开放岗位
--    时才出现在组队广场；卡片同时返回派生字段 recruitState：
--      fresh   = 尚无成员落位（占位数全为 0）
--      partial = 已有成员落位且仍有空缺（前端展示“队长意向职位部分已满…”提示条）
--      full    = 已无任何名额（不会出现在广场）
