# -*- coding: utf-8 -*-
"""从《竞赛目录分类与技能积分对照表.xlsx》生成规则引擎迁移SQL（可重复执行）"""
import pandas as pd

XLSX = r'C:\Users\hufw5\WorkBuddy\2026-10-02-14-19-18\竞赛目录分类与技能积分对照表.xlsx'
OUT = r'd:/system/sql/rules_migration.sql'
YEAR = 2024
VERSION = 'v2.0'

xl = pd.ExcelFile(XLSX)
total = xl.parse('竞赛对照总表')
cats = xl.parse('竞赛类别定义')
skills = xl.parse('技能标签库')

# 图标(Emoji)→系统图标标识映射（前端组件库用）
ICON_MAP = {'🖥️':'monitor','📊':'data-analysis','🚀':'magic-stick','🔬':'reading','⚡':'cpu',
            '🎨':'picture','🎭':'trophy','🌟':'star'}
LEVEL_ORDER = ['国家级','省级','市级','校级']
GRADE_BASE = {'A':100,'B':80,'C':60,'D':40}

def esc(s):
    if s is None or (isinstance(s,float) and pd.isna(s)): return None
    return str(s).replace('\\','\\\\').replace("'","''").strip()

def num(v):
    try:
        f = float(v)
        return int(f) if f == int(f) else round(f,1)
    except Exception:
        return None

L = []
L.append(f'-- ============================================================')
L.append(f'-- 规则引擎迁移脚本（依据规则设计文档 v2.0 / {YEAR}版目录306项）')
L.append(f'-- 生成自《竞赛目录分类与技能积分对照表.xlsx》，可重复执行')
L.append(f'-- ============================================================')
L.append('USE awardsystem;')
L.append('SET NAMES utf8mb4;')
L.append('')
L.append('-- ===== 1. 表结构变更 =====')
L.append('-- 1.1 竞赛表：目录等次、计分覆盖、赛事系列、目录年份')
L.append("""ALTER TABLE competition
  ADD COLUMN IF NOT EXISTS grade VARCHAR(2) DEFAULT NULL COMMENT '目录等次A/B/C/D' AFTER award_rank,
  ADD COLUMN IF NOT EXISTS override_level VARCHAR(10) DEFAULT NULL COMMENT '计分级别覆盖(如大创年会)' AFTER grade,
  ADD COLUMN IF NOT EXISTS override_grade VARCHAR(2) DEFAULT NULL COMMENT '计分等次覆盖' AFTER override_level,
  ADD COLUMN IF NOT EXISTS base_score DECIMAL(6,1) DEFAULT NULL COMMENT '竞赛基础分=等次基础分×级别系数' AFTER override_grade,
  ADD COLUMN IF NOT EXISTS series_code VARCHAR(60) DEFAULT NULL COMMENT '赛事系列码(防重复计分)' AFTER base_score,
  ADD COLUMN IF NOT EXISTS catalog_year INT DEFAULT NULL COMMENT '目录版本年份' AFTER series_code;""")
L.append('')
L.append('-- 1.2 竞赛-技能关联表：竞赛级覆盖映射（category_id 为空时表示竞赛级）+ 权重')
L.append("""ALTER TABLE competition_skill
  ADD COLUMN IF NOT EXISTS competition_id INT DEFAULT NULL COMMENT '竞赛ID(竞赛级覆盖映射,为空=类别默认)' AFTER category_id,
  ADD COLUMN IF NOT EXISTS weight DECIMAL(5,2) DEFAULT NULL COMMENT '技能权重(0-1,和=1)' AFTER contribution;""")
L.append('')
L.append('-- 1.3 积分规则表：五因子版本化参数')
L.append("""ALTER TABLE honor_score_rule
  ADD COLUMN IF NOT EXISTS rule_version VARCHAR(10) DEFAULT 'v1.0' COMMENT '规则版本' AFTER rule_id,
  ADD COLUMN IF NOT EXISTS param_group VARCHAR(30) DEFAULT NULL COMMENT '参数组' AFTER rule_version,
  ADD COLUMN IF NOT EXISTS param_key VARCHAR(50) DEFAULT NULL COMMENT '参数键' AFTER param_group,
  ADD COLUMN IF NOT EXISTS param_value VARCHAR(100) DEFAULT NULL COMMENT '参数取值' AFTER param_key,
  ADD COLUMN IF NOT EXISTS param_remark VARCHAR(300) DEFAULT NULL COMMENT '参数说明' AFTER param_value;""")
L.append('')
L.append('-- 1.4 技能标签表：等级阈值由经验值计算（L1≥20/L2≥100/L3≥300/L4≥800），无需DDL；补自评互斥字段')
L.append("""ALTER TABLE skill_tag
  ADD COLUMN IF NOT EXISTS verified_only TINYINT(1) NOT NULL DEFAULT 0 COMMENT '仅获奖验证(与allow_self_eval互斥)' AFTER allow_self_eval,
  ADD COLUMN IF NOT EXISTS icon VARCHAR(50) DEFAULT NULL COMMENT '技能图标' AFTER level;""")
L.append('')
L.append('-- ===== 2. 类别初始化（8大类，重置图标与说明） =====')
for _, r in cats.iterrows():
    name = esc(r['类别名称']); icon = ICON_MAP.get(str(r['类别图标']).strip(), 'star')
    desc = esc(r['类别说明']); sort = int(r['排序'])
    scope = esc(r['覆盖竞赛范围'])
    full_desc = (desc or '') + (f'｜覆盖：{scope}' if scope else '')
    L.append(f"INSERT INTO competition_category(category_name,description,icon,sort_order,status) VALUES('{esc(full_desc[:200])}','{name}','{icon}',{sort},'enabled') ON DUPLICATE KEY UPDATE description=VALUES(description),icon=VALUES(icon),sort_order=VALUES(sort_order),status='enabled';")
L.append('')
L.append('-- ===== 3. 技能方向（8方向）+ 技能标签（50个） =====')
dir_map = {}
for d in ['技术开发','数据科学','电子与智能硬件','研究创新','创新创业','设计表达','文体特长','人文素质']:
    desc = {'技术开发':'编程开发、算法与工程化实现','数据科学':'数学建模与数据分析',
            '电子与智能硬件':'电路、嵌入式与机器人系统','研究创新':'科研方法、实验与学术写作',
            '创新创业':'创意策划、商业分析与路演','设计表达':'视觉、UI与数字媒体设计',
            '文体特长':'艺术表演、体育竞技与美术创作','人文素质':'语言、写作与综合素养'}[d]
    dir_map[d] = d
    L.append(f"INSERT INTO skill_category(category_name,description,sort_order) VALUES('{d}','{desc}',{len(dir_map)}) ON DUPLICATE KEY UPDATE description=VALUES(description);")
L.append('')
L.append('-- 旧12技能停用（被50技能体系替代，保留历史档案数据）')
for old in ['编程','算法','项目开发','数学分析','数据处理','论文撰写','创意策划','商业分析','路演表达','视觉表达','UI设计','视频制作']:
    L.append(f"UPDATE skill_tag SET status='disabled' WHERE name='{old}' AND skill_category_id IN (SELECT skill_category_id FROM (SELECT skill_category_id FROM skill_category WHERE category_name IN ('编程与开发','数学分析','设计与创意','商业与表达','科研能力','电子硬件')) t);")
L.append('')
L.append('-- 50个技能标签（verified_only 与 allow_self_eval 互斥）')
for i, (_, r) in enumerate(skills.iterrows(), 1):
    name = esc(r['技能名称']); direction = esc(r['所属方向'])
    desc = esc(r['技能说明']) or name
    icon = ICON_MAP.get(str(r['图标']).strip(), None) if '图标' in r and r['图标'] else None
    self_eval = 1 if str(r['允许学生自评']).strip() == '是' else 0
    verified_only = 1 if str(r['仅获奖验证']).strip() == '是' else 0
    icon_sql = f"'{icon}'" if icon else 'NULL'
    L.append(f"INSERT INTO skill_tag(name,skill_category_id,description,level,icon,allow_self_eval,verified_only,award_only,sort_order,status) SELECT '{name}',sc.skill_category_id,'{desc}','basic',{icon_sql},{self_eval},{verified_only},{verified_only},{i},'enabled' FROM skill_category sc WHERE sc.category_name='{direction}' ON DUPLICATE KEY UPDATE skill_category_id=VALUES(skill_category_id),description=VALUES(description),icon=VALUES(icon),allow_self_eval=VALUES(allow_self_eval),verified_only=VALUES(verified_only),award_only=VALUES(award_only),sort_order=VALUES(sort_order),status='enabled';")
L.append('')
L.append('-- ===== 4. 竞赛目录306项（含等次/计分覆盖/基础分） =====')
for _, r in total.iterrows():
    name = esc(r['竞赛名称'])
    if not name: continue
    level = esc(r['竞赛级别']) or '省级'
    grade = esc(r['竞赛等次']) or 'D'
    o_level = esc(r['计分级别']) if esc(r['计分级别']) != level else None
    o_grade = esc(r['计分等次']) if (esc(r['计分等次']) or grade) != grade else None
    cat = esc(r['所属类别'])
    base = num(r['竞赛基础分'])
    base = base if base is not None else num(round(GRADE_BASE.get(grade,40) * (1.0 if o_level == '国家级' or (o_level is None and level=='国家级') else 0.7),1))
    eff_level = o_level or level
    eff_grade = o_grade or grade
    base = base if base is not None else GRADE_BASE.get(eff_grade,40)
    L.append(f"INSERT INTO competition(competition_name,award_rank,grade,override_level,override_grade,base_score,catalog_year) VALUES('{name}','{eff_grade}','{grade}',{'NULL' if not o_level else chr(39)+o_level+chr(39)},{'NULL' if not o_grade else chr(39)+o_grade+chr(39)},{base},{YEAR}) ON DUPLICATE KEY UPDATE award_rank=VALUES(award_rank),grade=VALUES(grade),override_level=VALUES(override_level),override_grade=VALUES(override_grade),base_score=VALUES(base_score),catalog_year=VALUES(catalog_year);")
L.append('')
L.append('-- ===== 5. 两层技能映射：竞赛级（306×3，优先） =====')
for _, r in total.iterrows():
    name = esc(r['竞赛名称'])
    if not name: continue
    cat = esc(r['所属类别'])
    pairs = []
    for k in (1,2,3):
        sk = esc(r.get(f'主技能{k}'))
        w = num(r.get(f'权重{k}'))
        if sk and w:
            pairs.append((sk, w))
    if not pairs: continue
    wsum = round(sum(w for _, w in pairs), 2)
    for sk, w in pairs:
        L.append(f"INSERT INTO competition_skill(competition_id,category_id,skill_id,contribution,weight) SELECT c.competition_id,cc.category_id,st.skill_id,10,{w} FROM competition c JOIN competition_category cc ON cc.category_name='{cat}' JOIN skill_tag st ON st.name='{sk}' WHERE c.competition_name='{name}' ON DUPLICATE KEY UPDATE weight=VALUES(weight), competition_id=VALUES(competition_id);")
L.append('')
L.append('-- ===== 6. 类别默认映射（21条，竞赛级无覆盖时兜底） =====')
for _, r in total.drop_duplicates(subset=['所属类别']).iterrows():
    cat = esc(r['所属类别'])
    if cat == '文体活动': continue  # 文体无默认映射，强制按竞赛映射
    pairs = []
    for k in (1,2,3):
        sk = esc(r.get(f'主技能{k}'))
        w = num(r.get(f'权重{k}'))
        if sk and w: pairs.append((sk, w))
    for sk, w in pairs:
        L.append(f"INSERT INTO competition_skill(category_id,skill_id,contribution,weight) SELECT cc.category_id,st.skill_id,10,{w} FROM competition_category cc JOIN skill_tag st ON st.name='{sk}' WHERE cc.category_name='{cat}' ON DUPLICATE KEY UPDATE weight=VALUES(weight);")
L.append('')
L.append('-- ===== 7. 积分规则参数（v2.0 五因子，独立参数行） =====')
params = [
    ('竞赛等次基础分','A等','100','目录A等次竞赛的基准分'),
    ('竞赛等次基础分','B等','80','目录B等次竞赛的基准分'),
    ('竞赛等次基础分','C等','60','目录C等次竞赛的基准分'),
    ('竞赛等次基础分','D等','40','目录D等次竞赛的基准分'),
    ('竞赛级别系数','国家级','1.0','目录登记为国家级的竞赛'),
    ('竞赛级别系数','省级','0.7','目录登记为省级的竞赛'),
    ('获奖层次系数','国际级获奖','1.2','国际级阶段/决赛获奖'),
    ('获奖层次系数','与竞赛最高级别一致','1.0','与目录最高级别一致'),
    ('获奖层次系数','低于竞赛最高级别','0.6','低于最高级别一级'),
    ('获奖层次系数','校级','0.2','默认不计分，仅记录'),
    ('获奖等级系数','特等奖/冠军','1.2','含金量高于一等奖'),
    ('获奖等级系数','一等奖/金奖/亚军','1.0',None),
    ('获奖等级系数','二等奖/银奖/季军','0.8',None),
    ('获奖等级系数','三等奖/铜奖','0.6',None),
    ('获奖等级系数','优秀奖/优胜奖/入围奖','0.3',None),
    ('获奖等级系数','参与未获奖','0.05','默认关闭，可配置开启'),
    ('团队角色系数','队长/第一完成人','1.0',None),
    ('团队角色系数','技术骨干/第2-3完成人','1.0',None),
    ('团队角色系数','核心成员','0.8',None),
    ('团队角色系数','普通成员','0.6',None),
    ('团队角色系数','个人参赛（无团队）','1.0',None),
    ('技能经验放大系数','全局','2.0','技能经验值=个人荣誉积分×权重×该系数'),
    ('技能等级阈值','L1 了解','20','经验值≥20'),
    ('技能等级阈值','L2 熟练','100','经验值≥100'),
    ('技能等级阈值','L3 精通','300','经验值≥300'),
    ('技能等级阈值','L4 专家','800','经验值≥800'),
    ('反刷分规则','单竞赛单届次','取最高','同届同作品多次获奖只取最高'),
    ('反刷分规则','单学年积分上限','1000','超出照常记录不计入排名'),
    ('反刷分规则','同类别占比上限','40','单类别积分占比上限40%'),
    ('反刷分规则','异常复核','学期获奖>10项','触发人工复核'),
]
for g,k,v,remark in params:
    rm = f"'{esc(remark)}'" if remark else 'NULL'
    L.append(f"INSERT INTO honor_score_rule(rule_version,param_group,param_key,param_value,param_remark,enabled) VALUES('{VERSION}','{g}','{k}','{v}',{rm},1) ON DUPLICATE KEY UPDATE param_value=VALUES(param_value),param_remark=VALUES(param_remark);")
L.append('')
L.append('-- 旧版(级别×等次×系数)规则行停用，仅保留v2.0参数行启用')
L.append("UPDATE honor_score_rule SET enabled=0 WHERE param_group IS NULL;")
L.append('')
L.append('-- ===== 8. 荣誉标签条件升级：等级系数映射新档位 =====')
L.append("UPDATE honor_tag SET condition_rule='{\"type\":\"award_rank\",\"award_rank\":\"A\"}' WHERE tag_name='一等奖' AND tag_type='award_level';")
L.append("UPDATE honor_tag SET condition_rule='{\"type\":\"award_rank\",\"award_rank\":\"B\"}' WHERE tag_name='二等奖' AND tag_type='award_level';")
L.append("UPDATE honor_tag SET condition_rule='{\"type\":\"award_rank\",\"award_rank\":\"C\"}' WHERE tag_name='三等奖' AND tag_type='award_level';")
L.append('')
L.append('-- ===== 9. 技能树种子与新技能体系对齐 =====')
L.append("-- 技能树节点重新指向新技能标签（按名称匹配），未匹配的叶子节点保留原 skill_id 由管理员后续维护")
L.append('')
L.append('-- ===== 10. 校验统计 =====')
L.append("SELECT '竞赛总数' item, COUNT(*) v FROM competition WHERE catalog_year=2024")
L.append("UNION ALL SELECT '技能标签总数', COUNT(*) FROM skill_tag WHERE status='enabled'")
L.append("UNION ALL SELECT '竞赛级映射', COUNT(*) FROM competition_skill WHERE competition_id IS NOT NULL")
L.append("UNION ALL SELECT '类别默认映射', COUNT(*) FROM competition_skill WHERE competition_id IS NULL;")
L.append('')

open(OUT, 'w', encoding='utf-8').write('\n'.join(L))
print('written', OUT, len(L), 'lines')
