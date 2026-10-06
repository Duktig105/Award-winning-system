package com.example.service;

import com.example.auth.AuthContext;
import com.example.entity.*;
import com.example.team.TeamBusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 荣誉系统业务层
 * 涵盖：竞赛类别、技能标签、竞赛-技能关联、学生技能档案、积分、勋章、荣誉标签、技能树
 */
@Service
public class HonorSystemService {
    private final JdbcTemplate jdbc;
    private final HonorScoreEngine scoreEngine;
    public HonorSystemService(JdbcTemplate jdbc, HonorScoreEngine scoreEngine){
        this.jdbc = jdbc;
        this.scoreEngine = scoreEngine;
    }

    // ========================================================
    // 竞赛类别管理
    // ========================================================
    public List<Map<String,Object>> listCategories(String status){
        String sql = "SELECT category_id categoryId,category_name categoryName,description,icon,sort_order sortOrder,status FROM competition_category";
        return jdbc.queryForList(sql + " WHERE (? IS NULL OR status=?) ORDER BY sort_order,category_id", status, status);
    }

    public int createCategory(Map<String,Object> body){
        requireAdmin();
        String name = str(body,"categoryName");
        if (name == null) throw new TeamBusinessException("VALIDATION_ERROR","类别名称不能为空");
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM competition_category WHERE category_name=?", Integer.class, name);
        if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","类别名称已存在");
        return jdbc.update("INSERT INTO competition_category(category_name,description,icon,sort_order,status) VALUES(?,?,?,?,?)",
                name,
                str(body,"description"),
                str(body,"icon"),
                intOr(body,"sortOrder",0),
                strOr(body,"status","enabled"));
    }

    public int updateCategory(Map<String,Object> body){
        requireAdmin();
        Integer id = intOr(body,"categoryId",null);
        if (id == null) throw new TeamBusinessException("VALIDATION_ERROR","categoryId不能为空");
        return jdbc.update("UPDATE competition_category SET category_name=?,description=?,icon=?,sort_order=?,status=? WHERE category_id=?",
                str(body,"categoryName"), str(body,"description"), str(body,"icon"),
                intOr(body,"sortOrder",0), strOr(body,"status","enabled"), id);
    }

    public int disableCategory(int id){
        requireAdmin();
        return jdbc.update("UPDATE competition_category SET status='disabled' WHERE category_id=?", id);
    }

    // ========================================================
    // 技能方向
    // ========================================================
    public List<Map<String,Object>> listSkillCategories(){
        return jdbc.queryForList("SELECT skill_category_id skillCategoryId,category_name categoryName,description,sort_order sortOrder,status FROM skill_category ORDER BY sort_order,skill_category_id");
    }

    // ========================================================
    // 技能标签管理
    // ========================================================
    public List<Map<String,Object>> listSkillTags(Integer categoryId, String status){
        StringBuilder sql = new StringBuilder(
            "SELECT s.skill_id skillId,s.name,s.skill_category_id skillCategoryId,sc.category_name categoryName," +
            " s.description,s.level,s.icon,s.allow_self_eval allowSelfEval,s.award_only awardOnly," +
            " s.sort_order sortOrder,s.status " +
            "FROM skill_tag s LEFT JOIN skill_category sc ON sc.skill_category_id=s.skill_category_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (categoryId != null) { sql.append(" AND s.skill_category_id=?"); args.add(categoryId); }
        if (status != null) { sql.append(" AND s.status=?"); args.add(status); }
        sql.append(" ORDER BY s.sort_order,s.skill_id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public int createSkillTag(Map<String,Object> body){
        requireAdmin();
        String name = str(body,"name");
        if (name == null) throw new TeamBusinessException("VALIDATION_ERROR","技能名称不能为空");
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM skill_tag WHERE name=?", Integer.class, name);
        if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","技能名称已存在");
        // 仅获奖验证与允许自评互斥：awardOnly 开启时强制关闭自评
        boolean awardOnly = boolOr(body,"awardOnly",false);
        boolean allowSelfEval = awardOnly ? false : boolOr(body,"allowSelfEval",true);
        return jdbc.update("INSERT INTO skill_tag(name,skill_category_id,description,level,icon,allow_self_eval,award_only,verified_only,sort_order,status) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?)",
                name, intOr(body,"skillCategoryId",null), str(body,"description"),
                strOr(body,"level","basic"), str(body,"icon"),
                allowSelfEval ? 1 : 0,
                awardOnly ? 1 : 0,
                awardOnly ? 1 : 0,
                intOr(body,"sortOrder",0), strOr(body,"status","enabled"));
    }

    public int updateSkillTag(Map<String,Object> body){
        requireAdmin();
        Integer id = intOr(body,"skillId",null);
        if (id == null) throw new TeamBusinessException("VALIDATION_ERROR","skillId不能为空");
        // 仅获奖验证与允许自评互斥：awardOnly 开启时强制关闭自评
        boolean awardOnly = boolOr(body,"awardOnly",false);
        boolean allowSelfEval = awardOnly ? false : boolOr(body,"allowSelfEval",true);
        return jdbc.update("UPDATE skill_tag SET skill_category_id=?,description=?,level=?,icon=?,allow_self_eval=?,award_only=?,verified_only=?,sort_order=?,status=? WHERE skill_id=?",
                intOr(body,"skillCategoryId",null), str(body,"description"),
                strOr(body,"level","basic"), str(body,"icon"),
                allowSelfEval ? 1 : 0,
                awardOnly ? 1 : 0,
                awardOnly ? 1 : 0,
                intOr(body,"sortOrder",0), strOr(body,"status","enabled"), id);
    }

    public int disableSkillTag(int id){
        requireAdmin();
        return jdbc.update("UPDATE skill_tag SET status='disabled' WHERE skill_id=?", id);
    }

    // ========================================================
    // 竞赛-技能关联
    // ========================================================
    public List<Map<String,Object>> listCompetitionSkillRelations(Integer categoryId, String mapType){
        StringBuilder sql = new StringBuilder(
            "SELECT cs.id, cc.category_id categoryId, cc.category_name categoryName," +
            " cs.competition_id competitionId, c.competition_name competitionName," +
            " cs.skill_id skillId, st.name skillName, st.level," +
            " cs.contribution, cs.weight, cs.upgrade_rule upgradeRule, cs.team_role teamRole, cs.team_role_weight teamRoleWeight" +
            " FROM competition_skill cs" +
            " JOIN competition_category cc ON cc.category_id=cs.category_id" +
            " JOIN skill_tag st ON st.skill_id=cs.skill_id" +
            " LEFT JOIN competition c ON c.competition_id=cs.competition_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (categoryId != null) { sql.append(" AND cc.category_id=?"); args.add(categoryId); }
        if ("default".equals(mapType)) sql.append(" AND cs.competition_id IS NULL");
        else if ("override".equals(mapType)) sql.append(" AND cs.competition_id IS NOT NULL");
        sql.append(" ORDER BY cc.sort_order, cs.competition_id IS NULL DESC, cs.competition_id, cs.id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    /**
     * 保存竞赛-技能映射（v2.0 两层）：
     * competitionId 为空 = 类别默认映射；非空 = 竞赛级覆盖映射（优先级更高）。
     * weight 为技能权重（0~1，同一映射目标下权重之和应为 1.0，引擎对非 1 值按比例归一化兜底）。
     */
    public int saveCompetitionSkillRelation(Map<String,Object> body){
        requireAdmin();
        Integer categoryId = intOr(body,"categoryId",null);
        Integer skillId = intOr(body,"skillId",null);
        if (categoryId == null || skillId == null) throw new TeamBusinessException("VALIDATION_ERROR","categoryId 和 skillId 不能为空");
        Integer competitionId = intOr(body,"competitionId",null);
        double weight = doubleOr(body,"weight",0.3);
        if (weight <= 0 || weight > 1) throw new TeamBusinessException("VALIDATION_ERROR","权重必须在 0~1 之间");
        Integer id = intOr(body,"id",null);
        if (id == null) {
            Integer dup;
            if (competitionId == null) {
                dup = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM competition_skill WHERE category_id=? AND skill_id=? AND competition_id IS NULL",
                    Integer.class, categoryId, skillId);
            } else {
                dup = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM competition_skill WHERE category_id=? AND skill_id=? AND competition_id=?",
                    Integer.class, categoryId, skillId, competitionId);
            }
            if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","该映射目标下已存在该技能关联");
            int n = jdbc.update("INSERT INTO competition_skill(category_id,competition_id,skill_id,contribution,weight,upgrade_rule,team_role,team_role_weight) VALUES(?,?,?,?,?,?,?,?)",
                    categoryId, competitionId, skillId,
                    intOr(body,"contribution",10), weight,
                    str(body,"upgradeRule"),
                    str(body,"teamRole"),
                    intOr(body,"teamRoleWeight",50));
            // 竞赛级覆盖时同步竞赛的类别归属（保证按类别兜底逻辑一致）
            if (competitionId != null) {
                jdbc.update("UPDATE competition SET category_id=? WHERE competition_id=? AND (category_id IS NULL OR category_id<>?)",
                        categoryId, competitionId, categoryId);
            }
            return n;
        } else {
            return jdbc.update("UPDATE competition_skill SET category_id=?,competition_id=?,skill_id=?,contribution=?,weight=?,upgrade_rule=?,team_role=?,team_role_weight=? WHERE id=?",
                    categoryId, competitionId, skillId,
                    intOr(body,"contribution",10), weight,
                    str(body,"upgradeRule"),
                    str(body,"teamRole"),
                    intOr(body,"teamRoleWeight",50), id);
        }
    }

    public int deleteCompetitionSkillRelation(int id){
        requireAdmin();
        return jdbc.update("DELETE FROM competition_skill WHERE id=?", id);
    }

    // ========================================================
    // 竞赛目录维护（类别归属 / 目录等次 / 计分覆盖 / 基础分）
    // ========================================================

    /** 竞赛下拉选项（竞赛级覆盖映射、目录维护用），支持关键字过滤，最多返回 500 条 */
    public List<Map<String,Object>> competitionOptions(String keyword){
        StringBuilder sql = new StringBuilder(
            "SELECT c.competition_id competitionId, c.competition_name competitionName, c.category_id categoryId," +
            " cc.category_name categoryName, c.grade, c.award_rank awardRank," +
            " c.override_level overrideLevel, c.override_grade overrideGrade, c.base_score baseScore" +
            " FROM competition c LEFT JOIN competition_category cc ON cc.category_id=c.category_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) { sql.append(" AND c.competition_name LIKE ?"); args.add("%" + keyword.trim() + "%"); }
        sql.append(" ORDER BY c.competition_id LIMIT 500");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    /**
     * 保存竞赛目录信息（v2.0）：所属类别、目录等次(A/B/C/D)、计分级别/等次覆盖、基础分。
     * baseScore 为空时按 等次基础分×级别系数 自动计算（等次取 override_grade 优先）。
     */
    @Transactional
    public int saveCompetitionCatalog(Map<String,Object> body){
        requireAdmin();
        Integer competitionId = intOr(body,"competitionId",null);
        if (competitionId == null) throw new TeamBusinessException("VALIDATION_ERROR","competitionId不能为空");
        Integer categoryId = intOr(body,"categoryId",null);
        if (categoryId == null) throw new TeamBusinessException("VALIDATION_ERROR","categoryId不能为空");
        String grade = str(body,"grade");
        if (grade != null && !grade.isBlank() && !List.of("A","B","C","D").contains(grade))
            throw new TeamBusinessException("VALIDATION_ERROR","目录等次只能为 A/B/C/D");
        String overrideLevel = str(body,"overrideLevel");
        String overrideGrade = str(body,"overrideGrade");
        if (overrideGrade != null && !overrideGrade.isBlank() && !List.of("A","B","C","D").contains(overrideGrade))
            throw new TeamBusinessException("VALIDATION_ERROR","计分等次覆盖只能为 A/B/C/D");
        // 基础分：显式传入优先，否则按 有效等次 × 计分级别 自动计算
        Double baseScore = body.get("baseScore") instanceof Number n ? n.doubleValue() : null;
        String effectiveGrade = (overrideGrade != null && !overrideGrade.isBlank()) ? overrideGrade : grade;
        String effectiveLevel = (overrideLevel != null && !overrideLevel.isBlank()) ? overrideLevel : str(body,"level");
        if (baseScore == null && effectiveGrade != null && !effectiveGrade.isBlank() && effectiveLevel != null && !effectiveLevel.isBlank()) {
            baseScore = scoreEngine.gradeBase(effectiveGrade) * scoreEngine.levelFactor(effectiveLevel);
            baseScore = Math.round(baseScore * 10) / 10.0;
        }
        int n = jdbc.update(
            "UPDATE competition SET category_id=?, grade=?, override_level=?, override_grade=?, base_score=? WHERE competition_id=?",
            categoryId,
            (grade != null && !grade.isBlank()) ? grade : null,
            (overrideLevel != null && !overrideLevel.isBlank()) ? overrideLevel : null,
            (overrideGrade != null && !overrideGrade.isBlank()) ? overrideGrade : null,
            baseScore, competitionId);
        // 同步该类别默认映射到竞赛级（新归类竞赛自动继承，保证不漏）
        return n;
    }

    // ========================================================
    // 获奖认定（角色快照 + 历史性突破特殊系数，审核通过前设置生效）
    // ========================================================

    /** 获奖申请列表（认定用）：可按状态过滤，含角色快照与突破标记 */
    public List<Map<String,Object>> listApplicationsForMarking(String status, String keyword){
        requireAdmin();
        StringBuilder sql = new StringBuilder(
            "SELECT a.application_id applicationId, a.application_number applicationNumber, a.project_name projectName," +
            " a.competition_level competitionLevel, a.award_rank awardRank, a.award_level awardLevel," +
            " a.application_status applicationStatus, a.member_role memberRole, a.breakthrough breakthrough," +
            " DATE_FORMAT(a.award_time,'%Y-%m-%d') awardTime, c.competition_name competitionName," +
            " s.student_number studentNumber, s.student_name studentName, s.college" +
            " FROM award_application a" +
            " LEFT JOIN competition c ON c.competition_id=a.competition_id" +
            " LEFT JOIN student s ON s.student_id=a.student_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND a.application_status=?"); args.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (c.competition_name LIKE ? OR s.student_name LIKE ? OR s.student_number LIKE ? OR a.application_number LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw); args.add(kw); args.add(kw); args.add(kw);
        }
        sql.append(" ORDER BY a.application_id DESC LIMIT 200");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    /** 获奖认定：设置团队角色快照与历史性突破标记（特殊系数×1.5） */
    public int markApplication(Map<String,Object> body){
        requireAdmin();
        Integer applicationId = intOr(body,"applicationId",null);
        if (applicationId == null) throw new TeamBusinessException("VALIDATION_ERROR","applicationId不能为空");
        String memberRole = str(body,"memberRole");
        if (memberRole != null && !memberRole.isBlank()
                && !List.of("队长","技术骨干","核心成员","普通成员").contains(memberRole))
            throw new TeamBusinessException("VALIDATION_ERROR","角色只能为 队长/技术骨干/核心成员/普通成员");
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM award_application WHERE application_id=?", Integer.class, applicationId);
        if (exists == null || exists == 0) throw new TeamBusinessException("NOT_FOUND","获奖申请不存在");
        boolean breakthrough = boolOr(body,"breakthrough",false);
        return jdbc.update("UPDATE award_application SET member_role=?, breakthrough=? WHERE application_id=?",
                (memberRole != null && !memberRole.isBlank()) ? memberRole : null,
                breakthrough ? 1 : 0, applicationId);
    }

    // ========================================================
    // 学生技能档案
    // ========================================================
    public List<Map<String,Object>> studentSkills(int studentId){
        ensureStudent(studentId);
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT ss.student_id studentId, ss.skill_id skillId, st.name skillName, st.level," +
            " st.skill_category_id skillCategoryId, sc.category_name categoryName," +
            " st.allow_self_eval allowSelfEval, st.award_only awardOnly," +
            " ss.self_eval_level selfEvalLevel, ss.experience, ss.source," +
            " ss.application_id applicationId, ss.verified, ss.note, ss.update_time updateTime," +
            " aa.application_number applicationNumber, aa.competition_id competitionId," +
            " c.competition_name competitionName, aa.award_rank awardRank, aa.award_level awardLevel" +
            " FROM student_skill ss" +
            " JOIN skill_tag st ON st.skill_id=ss.skill_id" +
            " LEFT JOIN skill_category sc ON sc.skill_category_id=st.skill_category_id" +
            " LEFT JOIN award_application aa ON aa.application_id=ss.application_id" +
            " LEFT JOIN competition c ON c.competition_id=aa.competition_id" +
            " WHERE ss.student_id=? ORDER BY ss.verified DESC, ss.experience DESC, st.name", studentId);
        // v2.0：L1~L4 等级由经验值自动计算（阈值可配置）
        double[] thresholds = scoreEngine.skillLevelThresholds();
        for (Map<String,Object> r : rows) {
            double exp = doubleVal(r.get("experience"));
            int lv = scoreEngine.skillLevel(exp);
            r.put("skillLevel", lv);
            r.put("skillLevelName", scoreEngine.skillLevelName(lv));
            r.put("nextLevelGap", scoreEngine.nextLevelGap(exp));
            r.put("levelThresholds", thresholds);
        }
        return rows;
    }

    /**
     * 学生新增自评技能
     */
    public int addSelfEvalSkill(Map<String,Object> body){
        int sid = requireStudent();
        Integer skillId = intOr(body,"skillId",null);
        if (skillId == null) throw new TeamBusinessException("VALIDATION_ERROR","skillId不能为空");
        List<Map<String,Object>> tags = jdbc.queryForList("SELECT name,allow_self_eval allowSelfEval,award_only awardOnly,status FROM skill_tag WHERE skill_id=?", skillId);
        if (tags.isEmpty()) throw new TeamBusinessException("NOT_FOUND","技能不存在");
        Map<String,Object> tag = tags.get(0);
        if (!"enabled".equals(String.valueOf(tag.get("status")))) throw new TeamBusinessException("FORBIDDEN","技能已停用");
        if (Boolean.TRUE.equals(tag.get("awardOnly"))) throw new TeamBusinessException("FORBIDDEN","该技能只能由获奖记录验证");
        if (!Boolean.TRUE.equals(tag.get("allowSelfEval"))) throw new TeamBusinessException("FORBIDDEN","该技能不允许学生自评");
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM student_skill WHERE student_id=? AND skill_id=?", Integer.class, sid, skillId);
        if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","技能已存在");
        String level = strOr(body,"selfEvalLevel","beginner");
        return jdbc.update("INSERT INTO student_skill(student_id,skill_id,self_eval_level,experience,source,verified) VALUES(?,?,?,?,?,?)",
                sid, skillId, level, intOr(body,"experience",0), "self_eval", 0);
    }

    /**
     * 学生更新自评技能（熟练程度）
     */
    public int updateSelfEvalSkill(Map<String,Object> body){
        int sid = requireStudent();
        Integer skillId = intOr(body,"skillId",null);
        if (skillId == null) throw new TeamBusinessException("VALIDATION_ERROR","skillId不能为空");
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT source,verified FROM student_skill WHERE student_id=? AND skill_id=?", sid, skillId);
        if (rows.isEmpty()) throw new TeamBusinessException("NOT_FOUND","技能档案中不存在该技能");
        Map<String,Object> row = rows.get(0);
        if (!"self_eval".equals(String.valueOf(row.get("source"))) || Boolean.TRUE.equals(row.get("verified"))) {
            throw new TeamBusinessException("FORBIDDEN","仅自评未验证的技能可编辑");
        }
        return jdbc.update("UPDATE student_skill SET self_eval_level=?,experience=?,note=? WHERE student_id=? AND skill_id=?",
                strOr(body,"selfEvalLevel","beginner"), intOr(body,"experience",0), str(body,"note"), sid, skillId);
    }

    /**
     * 学生移除自评技能
     */
    public int removeStudentSkill(int skillId){
        int sid = requireStudent();
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT source,verified FROM student_skill WHERE student_id=? AND skill_id=?", sid, skillId);
        if (rows.isEmpty()) throw new TeamBusinessException("NOT_FOUND","技能档案中不存在该技能");
        Map<String,Object> row = rows.get(0);
        if (!"self_eval".equals(String.valueOf(row.get("source"))) || Boolean.TRUE.equals(row.get("verified"))) {
            throw new TeamBusinessException("FORBIDDEN","已验证技能不能直接删除");
        }
        return jdbc.update("DELETE FROM student_skill WHERE student_id=? AND skill_id=?", sid, skillId);
    }

    /**
     * 组队推荐查询技能信息（包含等级、是否验证）
     */
    public List<Map<String,Object>> studentSkillsForRecommendation(int studentId){
        return jdbc.queryForList(
            "SELECT st.skill_id skillId, st.name skillName, ss.self_eval_level level, ss.experience, ss.verified" +
            " FROM skill_tag st LEFT JOIN student_skill ss ON ss.skill_id=st.skill_id AND ss.student_id=?" +
            " WHERE st.status='enabled' ORDER BY (ss.experience IS NULL), ss.experience DESC, st.name", studentId);
    }

    // ========================================================
    // 积分规则（v2.0 五因子参数行：等次基础分/级别系数/层次系数/等级系数/角色系数/特殊系数/放大系数/等级阈值/反刷分规则）
    // ========================================================
    public List<Map<String,Object>> listHonorScoreRules(){
        return jdbc.queryForList(
            "SELECT rule_id ruleId, rule_version ruleVersion, param_group paramGroup, param_key paramKey," +
            " param_value paramValue, param_remark paramRemark, enabled, update_time updateTime" +
            " FROM honor_score_rule WHERE param_group IS NOT NULL ORDER BY param_group, rule_id");
    }

    public int saveHonorScoreRule(Map<String,Object> body){
        requireAdmin();
        // v2.0 参数行模式
        if (body.get("paramGroup") != null || body.get("paramKey") != null) {
            String group = strOr(body,"paramGroup","");
            String key = strOr(body,"paramKey","");
            String value = strOr(body,"paramValue","");
            if (group.isBlank() || key.isBlank() || value.isBlank())
                throw new TeamBusinessException("VALIDATION_ERROR","参数组、参数项、参数取值均不能为空");
            Integer ruleId = intOr(body,"ruleId",null);
            if (ruleId == null) {
                Integer dup = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM honor_score_rule WHERE rule_version='v2.0' AND param_group=? AND param_key=?",
                    Integer.class, group, key);
                if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","该参数已存在");
                return jdbc.update(
                    "INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,rule_version,param_group,param_key,param_value,param_remark,enabled)" +
                    " VALUES('','',0,1.00,0,1.00,'v2.0',?,?,?,?,?)",
                    group, key, value, str(body,"paramRemark"), boolOr(body,"enabled",true) ? 1 : 0);
            }
            return jdbc.update(
                "UPDATE honor_score_rule SET param_group=?,param_key=?,param_value=?,param_remark=?,enabled=? WHERE rule_id=? AND param_group IS NOT NULL",
                group, key, value, str(body,"paramRemark"), boolOr(body,"enabled",true) ? 1 : 0, ruleId);
        }
        // 兼容旧版（级别×等次×系数）行编辑
        Integer ruleId = intOr(body,"ruleId",null);
        if (ruleId == null) {
            Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM honor_score_rule WHERE competition_level=? AND award_rank=? AND is_team=? AND param_group IS NULL",
                    Integer.class, strOr(body,"competitionLevel",""), strOr(body,"awardRank",""), boolOr(body,"isTeam",false) ? 1 : 0);
            if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","该组合规则已存在");
            return jdbc.update("INSERT INTO honor_score_rule(competition_level,award_rank,base_score,award_ratio,is_team,team_ratio,enabled,enable_time) VALUES(?,?,?,?,?,?,?,?)",
                    strOr(body,"competitionLevel",""), strOr(body,"awardRank",""),
                    intOr(body,"baseScore",0), doubleOr(body,"awardRatio",1.0),
                    boolOr(body,"isTeam",false) ? 1 : 0, doubleOr(body,"teamRatio",1.0),
                    boolOr(body,"enabled",true) ? 1 : 0, body.get("enableTime") != null ? Timestamp.valueOf(LocalDateTime.now()) : null);
        } else {
            return jdbc.update("UPDATE honor_score_rule SET competition_level=?,award_rank=?,base_score=?,award_ratio=?,is_team=?,team_ratio=?,enabled=? WHERE rule_id=? AND param_group IS NULL",
                    strOr(body,"competitionLevel",""), strOr(body,"awardRank",""),
                    intOr(body,"baseScore",0), doubleOr(body,"awardRatio",1.0),
                    boolOr(body,"isTeam",false) ? 1 : 0, doubleOr(body,"teamRatio",1.0),
                    boolOr(body,"enabled",true) ? 1 : 0, ruleId);
        }
    }

    public int toggleHonorScoreRule(int ruleId, boolean enable){
        requireAdmin();
        if (enable) {
            return jdbc.update("UPDATE honor_score_rule SET enabled=1, enable_time=NOW() WHERE rule_id=?", ruleId);
        } else {
            return jdbc.update("UPDATE honor_score_rule SET enabled=0 WHERE rule_id=?", ruleId);
        }
    }

    /**
     * 重新计算所有学生积分（基于获奖记录，v2.0 五因子规则）
     * 同竞赛同届次取最高（防刷分）：同一学生同一竞赛同一年只计最高分一条
     */
    @Transactional
    public Map<String,Object> recalcAllScores(){
        requireAdmin();
        // 清空明细与积分
        jdbc.update("DELETE FROM student_honor_score_log WHERE score_type IN ('award','recalc')");
        jdbc.update("DELETE FROM student_honor_score");
        // 清空由获奖产生的已验证技能，避免经验值重复累加（自评技能保留）
        jdbc.update("DELETE FROM student_skill WHERE source='award'");
        // 查询所有已通过的获奖申请
        List<Map<String,Object>> applications = jdbc.queryForList(
            "SELECT a.application_id applicationId, a.student_id studentId, a.competition_id competitionId," +
            " a.competition_level competitionLevel, a.award_rank awardRank, a.award_level awardLevel," +
            " a.team_id teamId, a.member_role memberRole, a.breakthrough breakthrough," +
            " a.award_time awardTime, c.competition_name competitionName" +
            " FROM award_application a LEFT JOIN competition c ON c.competition_id=a.competition_id" +
            " WHERE a.application_status='approved' ORDER BY a.award_time");
        // 防刷分：同学生+同竞赛+同届次(年份)取最高分（同届同作品取最高）
        Map<String, Map<String,Object>> bestPerSession = new LinkedHashMap<>();
        for (Map<String,Object> app : applications) {
            int sid = intVal(app.get("studentId"));
            int cid = intVal(app.get("competitionId"));
            String year = str(app.get("awardTime"));
            if (year != null && year.length() >= 4) year = year.substring(0,4); else year = "0";
            String role = str(app.get("memberRole")) != null ? str(app.get("memberRole"))
                    : resolveMemberRole(sid, app.get("teamId"));
            boolean breakthrough = intVal(app.get("breakthrough")) == 1;
            Map<String,Object> calc = scoreEngine.computeAwardScore(
                cid, competitionMaxLevel(cid), str(app.get("competitionLevel")),
                str(app.get("awardRank")), str(app.get("awardLevel")), role,
                scoreEngine.specialFactor(breakthrough));
            double score = doubleVal(calc.get("score"));
            app.put("role", role);
            app.put("calc", calc);
            String key = sid + ":" + cid + ":" + year;
            Map<String,Object> prev = bestPerSession.get(key);
            if (prev == null || doubleVal(((Map<String,Object>)prev.get("calc")).get("score")) < score) {
                bestPerSession.put(key, app);
            }
        }
        Map<Integer,Double> scoreMap = new LinkedHashMap<>();
        Map<Integer,List<Map<String,Object>>> logMap = new LinkedHashMap<>();
        for (Map<String,Object> app : bestPerSession.values()) {
            int sid = intVal(app.get("studentId"));
            int appId = intVal(app.get("applicationId"));
            Map<String,Object> calc = (Map<String,Object>) app.get("calc");
            double score = doubleVal(calc.get("score"));
            String role = str(app.get("role"));
            // 技能经验按两层映射折算
            try { grantSkillExperience(sid, appId, score, role); } catch (Exception ignore) {}
            if (score <= 0) continue;
            scoreMap.merge(sid, score, Double::sum);
            Map<String,Object> log = new LinkedHashMap<>();
            log.put("studentId", sid);
            log.put("applicationId", appId);
            log.put("score", score);
            log.put("scoreType", "recalc");
            log.put("description", "批量重算 - " + app.get("competitionName") + "(" + app.get("competitionLevel") + "/" + app.get("awardLevel") + ")");
            log.put("ruleSnapshot", String.format("v2.0 基础分S=%s × 层次L=%s × 等级G=%s × 角色R=%s(%s) × 特殊K=%s",
                    calc.get("base"), calc.get("levelFactor"), calc.get("gradeFactor"), calc.get("roleFactor"), role,
                    calc.get("specialFactor")));
            logMap.computeIfAbsent(sid,k->new ArrayList<>()).add(log);
        }
        for (Map.Entry<Integer,Double> entry : scoreMap.entrySet()) {
            int sid = entry.getKey();
            jdbc.update("INSERT INTO student_honor_score(student_id,total_score,last_calc_time) VALUES(?,?,NOW())", sid, entry.getValue());
            List<Map<String,Object>> logs = logMap.getOrDefault(sid, List.of());
            for (Map<String,Object> log : logs) {
                jdbc.update("INSERT INTO student_honor_score_log(student_id,application_id,score,score_type,description,rule_snapshot) VALUES(?,?,?,?,?,?)",
                        log.get("studentId"), log.get("applicationId"), log.get("score"),
                        log.get("scoreType"), log.get("description"), log.get("ruleSnapshot"));
            }
        }
        // 检查勋章与荣誉标签
        for (Integer sid : scoreMap.keySet()) {
            try { evaluateBadges(sid); evaluateHonorTags(sid); } catch (Exception ignore) {}
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("recalculatedStudents", scoreMap.size());
        result.put("totalApplications", applications.size());
        result.put("dedupedApplications", bestPerSession.size());
        result.put("ruleVersion", "v2.0");
        return result;
    }

    /**
     * 给单条获奖记录加积分（可在审核通过后调用）
     * v2.0 规则：个人积分 = 基础分S(等次×级别) × 层次L × 等级G × 角色R × 特殊K
     *          技能经验 = 个人积分 × 映射权重 × 放大系数(2.0)，队长附加项目管理经验
     */
    @Transactional
    public void addScoreForApplication(int applicationId){
        Map<String,Object> app = jdbc.queryForMap(
            "SELECT a.application_id applicationId, a.student_id studentId, a.competition_id competitionId," +
            " a.competition_level competitionLevel, a.award_rank awardRank, a.award_level awardLevel," +
            " a.team_id teamId, a.member_role memberRole, a.breakthrough breakthrough, c.competition_name competitionName" +
            " FROM award_application a LEFT JOIN competition c ON c.competition_id=a.competition_id" +
            " WHERE a.application_id=?", applicationId);
        int sid = intVal(app.get("studentId"));
        String competitionName = str(app.get("competitionName"));
        String actualLevel = str(app.get("competitionLevel"));
        // 团队角色：优先使用录入的角色快照（队长/技术骨干/核心成员/普通成员），否则按团队信息推导
        String role = str(app.get("memberRole")) != null ? str(app.get("memberRole"))
                : resolveMemberRole(sid, app.get("teamId"));
        // 特殊系数：历史性突破奖（管理员认定）×1.5，常规 1.0（参数行可调）
        boolean breakthrough = intVal(app.get("breakthrough")) == 1;
        double special = scoreEngine.specialFactor(breakthrough);
        // 五因子计算
        Map<String,Object> calc = scoreEngine.computeAwardScore(
            intVal(app.get("competitionId")), competitionMaxLevel(app.get("competitionId")),
            actualLevel, str(app.get("awardRank")), str(app.get("awardLevel")), role, special);
        double score = doubleVal(calc.get("score"));
        // 防止重复（幂等键：application_id + score_type='award'）
        Integer existed = jdbc.queryForObject("SELECT COUNT(*) FROM student_honor_score_log WHERE application_id=? AND score_type='award'", Integer.class, applicationId);
        if (existed != null && existed > 0) return;
        if (score > 0) {
            Integer current = jdbc.queryForObject("SELECT total_score FROM student_honor_score WHERE student_id=?", Integer.class, sid);
            if (current == null) {
                jdbc.update("INSERT INTO student_honor_score(student_id,total_score,last_calc_time) VALUES(?,?,NOW())", sid, score);
            } else {
                jdbc.update("UPDATE student_honor_score SET total_score=total_score+?, last_calc_time=NOW() WHERE student_id=?", score, sid);
            }
            jdbc.update("INSERT INTO student_honor_score_log(student_id,application_id,score,score_type,description,rule_snapshot) VALUES(?,?,?,?,?,?)",
                    sid, applicationId, score, "award",
                    "获奖积分 - " + competitionName + "(" + actualLevel + "/" + str(app.get("awardLevel")) + ")"
                        + (breakthrough ? "【历史性突破】" : ""),
                    String.format("v2.0 基础分S=%s × 层次L=%s × 等级G=%s × 角色R=%s(%s) × 特殊K=%s%s",
                            calc.get("base"), calc.get("levelFactor"), calc.get("gradeFactor"), calc.get("roleFactor"), role,
                            calc.get("specialFactor"), breakthrough ? "(历史性突破奖)" : ""));
        }
        // 按两层映射把积分折算为技能经验（幂等）
        try { grantSkillExperience(sid, applicationId, score, role); } catch (Exception ignore) {}
        // 触发勋章、荣誉标签评估
        try { evaluateBadges(sid); } catch (Exception ignore){}
        try { evaluateHonorTags(sid); } catch (Exception ignore){}
    }

    /** 解析学生在该获奖中的团队角色 */
    private String resolveMemberRole(int studentId, Object teamIdObj){
        Integer teamId = teamIdObj instanceof Number n ? n.intValue() : null;
        if (teamId == null) return null; // 个人参赛
        try {
            Integer leaderId = jdbc.queryForObject("SELECT leader_id FROM team WHERE team_id=?", Integer.class, teamId);
            if (leaderId != null && leaderId == studentId) return "队长";
        } catch (Exception ignore) {}
        try {
            Integer isLeader = jdbc.queryForObject(
                "SELECT is_leader FROM team_member WHERE team_id=? AND student_id=?", Integer.class, teamId, studentId);
            if (isLeader != null && isLeader == 1) return "队长";
        } catch (Exception ignore) {}
        return "核心成员";
    }

    /** 竞赛目录登记的最高级别（竞赛表 override_level 优先） */
    private String competitionMaxLevel(Object competitionIdObj){
        Integer competitionId = competitionIdObj instanceof Number n ? n.intValue() : null;
        if (competitionId == null) return null;
        try {
            return jdbc.queryForObject(
                "SELECT IFNULL(override_level,'国家级') FROM competition WHERE competition_id=?", String.class, competitionId);
        } catch (Exception ignore) { return "国家级"; }
    }

    /**
     * 两层技能映射：竞赛级覆盖优先，其次类别默认；每个技能经验 = 个人积分 × 权重 × 放大系数。
     * 队长额外获得项目管理经验 = 个人积分 × 0.15 × 放大系数。
     * 幂等键：student_skill(source='award', application_id, skill_id)。
     */
    @Transactional
    public void grantSkillExperience(int studentId, int applicationId, double personalScore, String role){
        if (personalScore <= 0) return;
        Map<String,Object> app = jdbc.queryForMap(
            "SELECT a.competition_id competitionId, c.competition_name competitionName FROM award_application a" +
            " LEFT JOIN competition c ON c.competition_id=a.competition_id WHERE a.application_id=?", applicationId);
        Integer competitionId = intVal(app.get("competitionId"));
        String competitionName = str(app.get("competitionName"));
        Integer categoryId = resolveCategoryId(competitionId, competitionName);
        // 1) 竞赛级覆盖映射
        List<Map<String,Object>> mappings = (competitionId != null)
            ? jdbc.queryForList(
                "SELECT cs.skill_id skillId, st.name skillName, IFNULL(cs.weight,1.0) weight" +
                " FROM competition_skill cs JOIN skill_tag st ON st.skill_id=cs.skill_id" +
                " WHERE cs.competition_id=? AND st.status='enabled'", competitionId)
            : List.of();
        // 2) 类别默认映射兜底
        if (mappings.isEmpty() && categoryId != null) {
            mappings = jdbc.queryForList(
                "SELECT cs.skill_id skillId, st.name skillName, IFNULL(cs.weight,0.3) weight" +
                " FROM competition_skill cs JOIN skill_tag st ON st.skill_id=cs.skill_id" +
                " WHERE cs.category_id=? AND cs.competition_id IS NULL AND st.status='enabled'", categoryId);
        }
        double amplifier = scoreEngine.expAmplifier();
        // 权重归一化（和≠1 时按比例折算，防御脏数据）
        double wsum = mappings.stream().mapToDouble(m -> doubleVal(m.get("weight"))).sum();
        if (wsum <= 0) wsum = 1.0;
        for (Map<String,Object> m : mappings) {
            int skillId = intVal(m.get("skillId"));
            double weight = doubleVal(m.get("weight")) / wsum;
            double exp = Math.round(personalScore * weight * amplifier * 10) / 10.0;
            if (exp <= 0) continue;
            if (alreadyGranted(studentId, skillId, applicationId)) continue;
            upsertSkillExperience(studentId, skillId, exp, applicationId);
        }
        // 队长附加项目管理经验（体现统筹贡献）
        if (role != null && role.contains("队长")) {
            Integer pmSkillId = findSkillIdByName("项目管理");
            if (pmSkillId != null && !alreadyGranted(studentId, pmSkillId, applicationId)) {
                double pmExp = Math.round(personalScore * 0.15 * amplifier * 10) / 10.0;
                if (pmExp > 0) upsertSkillExperience(studentId, pmSkillId, pmExp, applicationId);
            }
        }
    }

    private boolean alreadyGranted(int studentId, int skillId, int applicationId){
        Integer dup = jdbc.queryForObject(
            "SELECT COUNT(*) FROM student_skill WHERE student_id=? AND skill_id=? AND source='award' AND application_id=?",
            Integer.class, studentId, skillId, applicationId);
        return dup != null && dup > 0;
    }

    /** 经验值入账：已存在则累加，否则新增；source='award' 且 verified=1 */
    private void upsertSkillExperience(int studentId, int skillId, double exp, int applicationId){
        List<Map<String,Object>> exists = jdbc.queryForList(
            "SELECT student_id FROM student_skill WHERE student_id=? AND skill_id=?", studentId, skillId);
        if (!exists.isEmpty()) {
            jdbc.update("UPDATE student_skill SET experience = experience + ?, source='award', verified=1, application_id=?" +
                    " WHERE student_id=? AND skill_id=?", exp, applicationId, studentId, skillId);
        } else {
            jdbc.update("INSERT INTO student_skill(student_id,skill_id,self_eval_level,experience,source,verified,application_id) VALUES(?,?,?,?,?,?,?)",
                    studentId, skillId, "beginner", exp, "award", 1, applicationId);
        }
    }

    private Integer findSkillIdByName(String name){
        List<Integer> ids = jdbc.queryForList(
            "SELECT skill_id FROM skill_tag WHERE name=? AND status='enabled' LIMIT 1", Integer.class, name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    // ========================================================
    // 学生积分查询
    // ========================================================
    public Map<String,Object> studentHonorSummary(int studentId){
        ensureStudent(studentId);
        Map<String,Object> result = new LinkedHashMap<>();
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT IFNULL(total_score,0) totalScore, IFNULL(level_score,0) levelScore, last_calc_time lastCalcTime" +
            " FROM student_honor_score WHERE student_id=?", studentId);
        if (!rows.isEmpty()) result.putAll(rows.get(0));
        else { result.put("totalScore", 0); result.put("levelScore", 0); result.put("lastCalcTime", null); }
        Integer totalBadges = jdbc.queryForObject("SELECT COUNT(*) FROM student_badge WHERE student_id=? AND unlocked_at IS NOT NULL", Integer.class, studentId);
        Integer totalTags = jdbc.queryForObject("SELECT COUNT(*) FROM student_honor_tag WHERE student_id=?", Integer.class, studentId);
        Integer totalSkills = jdbc.queryForObject("SELECT COUNT(*) FROM student_skill WHERE student_id=?", Integer.class, studentId);
        Integer verifiedSkills = jdbc.queryForObject("SELECT COUNT(*) FROM student_skill WHERE student_id=? AND verified=1", Integer.class, studentId);
        Integer totalAwards = jdbc.queryForObject("SELECT COUNT(*) FROM award_application WHERE student_id=? AND application_status='approved'", Integer.class, studentId);
        result.put("totalBadges", totalBadges == null ? 0 : totalBadges);
        result.put("totalTags", totalTags == null ? 0 : totalTags);
        result.put("totalSkills", totalSkills == null ? 0 : totalSkills);
        result.put("verifiedSkills", verifiedSkills == null ? 0 : verifiedSkills);
        result.put("totalAwards", totalAwards == null ? 0 : totalAwards);
        return result;
    }

    public List<Map<String,Object>> studentHonorLogs(int studentId, int limit){
        ensureStudent(studentId);
        return jdbc.queryForList(
            "SELECT l.log_id logId, l.student_id studentId, l.application_id applicationId," +
            " l.score, l.score_type scoreType, l.description, l.rule_snapshot ruleSnapshot, l.create_time createTime," +
            " c.competition_name competitionName, aa.competition_level competitionLevel," +
            " aa.award_rank awardRank, aa.award_level awardLevel" +
            " FROM student_honor_score_log l" +
            " LEFT JOIN award_application aa ON aa.application_id=l.application_id" +
            " LEFT JOIN competition c ON c.competition_id=aa.competition_id" +
            " WHERE l.student_id=? ORDER BY l.create_time DESC LIMIT ?", studentId, limit);
    }

    // ========================================================
    // 勋章
    // ========================================================
    public List<Map<String,Object>> studentBadges(int studentId){
        ensureStudent(studentId);
        List<Map<String,Object>> all = jdbc.queryForList(
            "SELECT b.badge_id badgeId, b.badge_code badgeCode, b.badge_name badgeName," +
            " b.description, b.icon, b.unlock_condition unlockCondition, b.unlock_threshold unlockThreshold," +
            " b.sort_order sortOrder, b.status," +
            " IFNULL(sb.progress,0) progress, sb.unlocked_at unlockedAt, IFNULL(sb.is_representative,0) isRepresentative" +
            " FROM badge b LEFT JOIN student_badge sb ON sb.badge_id=b.badge_id AND sb.student_id=?" +
            " WHERE b.status='enabled' ORDER BY b.sort_order, b.badge_id", studentId);
        // 计算每个勋章的进度
        for (Map<String,Object> row : all) {
            Integer bid = intVal(row.get("badgeId"));
            String code = str(row.get("badgeCode"));
            Integer threshold = intVal(row.get("unlockThreshold"));
            Integer currentProgress = intVal(row.get("progress"));
            if (row.get("unlockedAt") != null) {
                row.put("unlocked", true);
                row.put("currentProgress", currentProgress);
            } else {
                int computed = computeBadgeProgress(studentId, code, str(row.get("unlockCondition")), threshold);
                row.put("unlocked", computed >= threshold);
                row.put("currentProgress", Math.min(computed, threshold));
                if (Boolean.TRUE.equals(row.get("unlocked"))) {
                    jdbc.update("INSERT INTO student_badge(student_id,badge_id,progress,unlocked_at) VALUES(?,?,?,NOW()) ON DUPLICATE KEY UPDATE unlocked_at=NOW(),progress=VALUES(progress)",
                            studentId, bid, computed);
                    row.put("unlockedAt", new java.sql.Timestamp(System.currentTimeMillis()));
                }
            }
        }
        return all;
    }

    public int setRepresentativeBadge(int studentId, int badgeId){
        ensureStudent(studentId);
        jdbc.update("UPDATE student_badge SET is_representative=0 WHERE student_id=?", studentId);
        return jdbc.update("UPDATE student_badge SET is_representative=1 WHERE student_id=? AND badge_id=? AND unlocked_at IS NOT NULL",
                studentId, badgeId);
    }

    // ========================================================
    // 勋章管理（管理端）：固定条件类型 + 参数配置，第一版不提供通用规则编辑器
    // ========================================================
    /** 固定条件类型白名单 */
    private static final Set<String> BADGE_CONDITION_TYPES = Set.of(
        "first_award", "award_level", "award_count", "team_award",
        "team_leader_award", "category_count", "team_partner_count", "continuous_semesters");

    public List<Map<String,Object>> allBadgeConfigs(){
        return jdbc.queryForList(
            "SELECT badge_id badgeId, badge_code badgeCode, badge_name badgeName, description, icon," +
            " unlock_condition unlockCondition, unlock_threshold unlockThreshold, sort_order sortOrder, status" +
            " FROM badge ORDER BY sort_order, badge_id");
    }

    /** 新增或修改勋章（badgeId 为空时新增，否则更新） */
    @Transactional
    public int saveBadge(Map<String,Object> body){
        requireAdmin();
        Integer badgeId = intOr(body,"badgeId",null);
        String code = strOr(body,"badgeCode","").trim();
        String name = strOr(body,"badgeName","").trim();
        if (code.isEmpty() || name.isEmpty()) throw new TeamBusinessException("VALIDATION_ERROR","勋章编码与名称不能为空");
        String conditionType = strOr(body,"conditionType","").trim();
        if (!BADGE_CONDITION_TYPES.contains(conditionType)) {
            throw new TeamBusinessException("VALIDATION_ERROR","不支持的条件类型，第一版仅支持固定条件类型：" + String.join("、", BADGE_CONDITION_TYPES));
        }
        String level = str(body,"competitionLevel");
        Integer threshold = intOr(body,"unlockThreshold",1);
        if (threshold == null || threshold < 1) threshold = 1;
        Integer sortOrder = intOr(body,"sortOrder",0);
        String status = strOr(body,"status","enabled");
        if (!"enabled".equals(status) && !"disabled".equals(status)) status = "enabled";
        String condition = buildConditionJson(conditionType, level, threshold);
        if (badgeId == null) {
            Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM badge WHERE badge_code=?", Integer.class, code);
            if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","勋章编码已存在");
            return jdbc.update(
                "INSERT INTO badge(badge_code,badge_name,description,icon,unlock_condition,unlock_threshold,sort_order,status)" +
                " VALUES(?,?,?,?,?,?,?,?)",
                code, name, str(body,"description"), str(body,"icon"), condition, threshold, sortOrder, status);
        }
        return jdbc.update(
            "UPDATE badge SET badge_name=?, description=?, icon=?, unlock_condition=?, unlock_threshold=?, sort_order=?, status=?" +
            " WHERE badge_id=?",
            name, str(body,"description"), str(body,"icon"), condition, threshold, sortOrder, status, badgeId);
    }

    /** 启用/停用勋章 */
    public int toggleBadge(int badgeId, String status){
        requireAdmin();
        if (!"enabled".equals(status) && !"disabled".equals(status)) {
            throw new TeamBusinessException("VALIDATION_ERROR","非法状态：" + status);
        }
        return jdbc.update("UPDATE badge SET status=? WHERE badge_id=?", status, badgeId);
    }

    /** 组装固定条件 JSON（条件类型 + 参数） */
    private static String buildConditionJson(String type, String level, int threshold){
        StringBuilder sb = new StringBuilder("{\"type\":\"").append(type).append("\"");
        if (("award_level".equals(type) || "award_count".equals(type)) && level != null && !level.isBlank()) {
            sb.append(",\"competition_level\":\"").append(level.trim()).append("\"");
        }
        sb.append(",\"min_count\":").append(threshold).append("}");
        return sb.toString();
    }

    public Map<String,Object> recalcBadgesForStudent(int studentId){
        ensureStudent(studentId);
        evaluateBadges(studentId);
        return Map.of("ok", true);
    }

    // ========================================================
    // 荣誉标签
    // ========================================================
    public List<Map<String,Object>> studentHonorTags(int studentId){
        ensureStudent(studentId);
        // 自动评估一次
        evaluateHonorTags(studentId);
        return jdbc.queryForList(
            "SELECT ht.tag_id tagId, ht.tag_name tagName, ht.tag_type tagType, ht.icon, ht.description, ht.sort_order sortOrder," +
            " sht.awarded_at awardedAt" +
            " FROM honor_tag ht JOIN student_honor_tag sht ON sht.tag_id=ht.tag_id" +
            " WHERE ht.status='enabled' AND sht.student_id=?" +
            " ORDER BY ht.sort_order, ht.tag_id", studentId);
    }

    public List<Map<String,Object>> allHonorTags(){
        return jdbc.queryForList(
            "SELECT tag_id tagId,tag_name tagName,tag_type tagType,description,condition_rule conditionRule,icon,status,sort_order sortOrder FROM honor_tag ORDER BY sort_order,tag_id");
    }

    public int saveHonorTag(Map<String,Object> body){
        requireAdmin();
        Integer tagId = intOr(body,"tagId",null);
        String name = strOr(body,"tagName","");
        String type = strOr(body,"tagType","award_level");
        if (tagId == null) {
            Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM honor_tag WHERE tag_name=? AND tag_type=?", Integer.class, name, type);
            if (dup != null && dup > 0) throw new TeamBusinessException("DUPLICATE","标签名称重复");
            return jdbc.update("INSERT INTO honor_tag(tag_name,tag_type,description,condition_rule,icon,status,sort_order) VALUES(?,?,?,?,?,?,?)",
                    name, type, str(body,"description"), strOr(body,"conditionRule","{}"),
                    str(body,"icon"), strOr(body,"status","enabled"), intOr(body,"sortOrder",0));
        } else {
            return jdbc.update("UPDATE honor_tag SET tag_name=?,tag_type=?,description=?,condition_rule=?,icon=?,status=?,sort_order=? WHERE tag_id=?",
                    name, type, str(body,"description"), strOr(body,"conditionRule","{}"),
                    str(body,"icon"), strOr(body,"status","enabled"), intOr(body,"sortOrder",0), tagId);
        }
    }

    public int toggleHonorTag(int tagId, String status){
        requireAdmin();
        return jdbc.update("UPDATE honor_tag SET status=? WHERE tag_id=?", status, tagId);
    }

    // ========================================================
    // 技能树
    // ========================================================
    public Map<String,Object> studentSkillTree(int studentId){
        ensureStudent(studentId);
        // 展示账号保留原有设计：零阈值节点默认点亮；
        // 其他账号所有节点默认未点亮——零阈值节点需学生主动点亮，其余需技能经验/已验证技能
        boolean showcase = isShowcaseStudent(studentId);
        Set<Integer> manualUnlockedNodes = manualUnlockedNodeIds(studentId);
        List<Map<String,Object>> categories = jdbc.queryForList(
            "SELECT skill_category_id skillCategoryId, category_name categoryName, description, sort_order sortOrder FROM skill_category ORDER BY sort_order,skill_category_id");
        List<Map<String,Object>> nodes = jdbc.queryForList(
            "SELECT node_id nodeId, skill_category_id skillCategoryId, parent_id parentId, skill_id skillId," +
            " name, level, description, icon, unlock_threshold unlockThreshold, sort_order sortOrder" +
            " FROM skill_tree_node WHERE status='enabled' ORDER BY parent_id, sort_order, node_id");
        Map<Integer,List<Map<String,Object>>> childrenMap = new LinkedHashMap<>();
        Map<Integer,Map<String,Object>> nodeMap = new LinkedHashMap<>();
        for (Map<String,Object> n : nodes) {
            int nid = intVal(n.get("nodeId"));
            nodeMap.put(nid, n);
            int parent = intVal(n.get("parentId"));
            childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(n);
        }
        // 当前学生的技能经验
        Map<Integer,Integer> skillExp = new LinkedHashMap<>();
        Map<Integer,Boolean> skillVerified = new LinkedHashMap<>();
        for (Map<String,Object> row : jdbc.queryForList(
                "SELECT skill_id skillId, IFNULL(experience,0) experience, verified FROM student_skill WHERE student_id=?", studentId)) {
            skillExp.put(intVal(row.get("skillId")), intVal(row.get("experience")));
            skillVerified.put(intVal(row.get("skillId")), intVal(row.get("verified")) == 1);
        }
        Integer verifiedTotal = jdbc.queryForObject("SELECT COUNT(*) FROM student_skill WHERE student_id=? AND verified=1", Integer.class, studentId);
        int verifiedCount = verifiedTotal == null ? 0 : verifiedTotal;
        // 递归构造节点树
        List<Map<String,Object>> treeCategories = new ArrayList<>();
        for (Map<String,Object> cat : categories) {
            int catId = intVal(cat.get("skillCategoryId"));
            List<Map<String,Object>> roots = childrenMap.getOrDefault(0, new ArrayList<>()).stream()
                    .filter(n -> intVal(n.get("parentId")) == 0 && Objects.equals(intVal(n.get("skillCategoryId")), catId))
                    .collect(Collectors.toList());
            // 同时支持顶级节点用 parent_id 为空或 0 的节点
            if (roots.isEmpty()) {
                roots = childrenMap.getOrDefault(0, new ArrayList<>()).stream()
                        .filter(n -> Objects.equals(intVal(n.get("skillCategoryId")), catId) && intVal(n.get("parentId")) != 0)
                        .collect(Collectors.toList());
            }
            for (Map<String,Object> n : roots) {
                buildTree(n, childrenMap, skillExp, skillVerified, verifiedCount, showcase, manualUnlockedNodes);
            }
            Map<String,Object> catNode = new LinkedHashMap<>(cat);
            catNode.put("nodes", roots);
            treeCategories.add(catNode);
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("categories", treeCategories);
        result.put("verifiedSkillCount", verifiedCount);
        result.put("totalSkillExperience", skillExp.values().stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    @SuppressWarnings("unchecked")
    private void buildTree(Map<String,Object> node, Map<Integer,List<Map<String,Object>>> childrenMap,
                           Map<Integer,Integer> skillExp, Map<Integer,Boolean> skillVerified, int verifiedCount,
                           boolean showcase, Set<Integer> manualUnlockedNodes){
        int nid = intVal(node.get("nodeId"));
        Integer sid = (Integer) node.get("skillId");
        int threshold = intVal(node.get("unlockThreshold"));
        // 展示账号保留原设计（阈值0的节点默认点亮）；普通账号至少需要1点经验/1项已验证技能
        int effectiveThreshold = showcase ? threshold : Math.max(threshold, 1);
        boolean unlocked;
        int currentExp = 0;
        if (sid != null) {
            currentExp = skillExp.getOrDefault(sid, 0);
            unlocked = currentExp >= effectiveThreshold;
        } else {
            // 顶级节点使用已验证技能数量解锁
            unlocked = verifiedCount >= effectiveThreshold;
            currentExp = verifiedCount;
        }
        // 无门槛节点（阈值<=0）：展示账号保留默认点亮效果；普通账号需学生主动点亮后才点亮
        if (threshold <= 0) {
            unlocked = showcase || manualUnlockedNodes.contains(nid);
        }
        node.put("unlocked", unlocked);
        node.put("currentExperience", currentExp);
        node.put("skillExperience", currentExp);
        node.put("verifiedSkillCount", verifiedCount);
        List<Map<String,Object>> children = childrenMap.getOrDefault(nid, new ArrayList<>());
        for (Map<String,Object> child : children) {
            buildTree(child, childrenMap, skillExp, skillVerified, verifiedCount, showcase, manualUnlockedNodes);
        }
        node.put("children", children);
    }

    /** 查询学生已手动点亮的无门槛节点ID集合 */
    private Set<Integer> manualUnlockedNodeIds(int studentId){
        Set<Integer> ids = new java.util.HashSet<>();
        for (Map<String,Object> row : jdbc.queryForList(
                "SELECT node_id FROM skill_node_manual_unlock WHERE student_id=?", studentId)) {
            ids.add(intVal(row.get("node_id")));
        }
        return ids;
    }

    /**
     * 学生手动点亮无门槛技能节点。
     * 仅允许点亮无门槛节点（unlock_threshold <= 0）；有经验门槛的节点需通过获奖积累经验解锁。
     */
    public void manualUnlockNode(int studentId, int nodeId){
        Integer threshold = jdbc.queryForObject(
            "SELECT IFNULL(unlock_threshold,0) FROM skill_tree_node WHERE node_id=? AND status='enabled'",
            Integer.class, nodeId);
        if (threshold == null) throw new TeamBusinessException("NOT_FOUND","技能节点不存在");
        if (threshold > 0) throw new TeamBusinessException("VALIDATION_ERROR","该技能节点有经验门槛，需通过获奖积累经验解锁");
        jdbc.update("INSERT IGNORE INTO skill_node_manual_unlock(student_id, node_id) VALUES(?,?)", studentId, nodeId);
    }

    /**
     * 展示账号：技能树保留演示效果（零阈值节点默认点亮）。
     * 展示账号学号通过环境变量 SAIMS_SHOWCASE_STUDENT 指定（默认为演示账号 202600010001），
     * 该学号不存在时按普通账号处理。
     */
    private static final String SHOWCASE_STUDENT_NUMBER =
            System.getenv().getOrDefault("SAIMS_SHOWCASE_STUDENT", "202600010001");

    private boolean isShowcaseStudent(int studentId){
        try {
            Integer showcaseId = jdbc.queryForObject(
                "SELECT student_id FROM student WHERE student_number='" + SHOWCASE_STUDENT_NUMBER + "'", Integer.class);
            return showcaseId != null && showcaseId == studentId;
        } catch (Exception e) {
            return false;
        }
    }

    // ========================================================
    // 对外：技能/勋章/标签的接口调用
    // ========================================================
    /** 技能等级查询（v2.0：等级由经验值自动计算 L1~L4） */
    public List<Map<String,Object>> queryStudentSkillLevels(int studentId){
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT st.skill_id skillId, st.name skillName, IFNULL(ss.self_eval_level,'beginner') selfEvalLevel," +
            " IFNULL(ss.experience,0) experience, IFNULL(ss.verified,0) verified, IFNULL(ss.source,'none') source" +
            " FROM skill_tag st LEFT JOIN student_skill ss ON ss.skill_id=st.skill_id AND ss.student_id=?" +
            " WHERE st.status='enabled' ORDER BY ss.experience DESC, st.name", studentId);
        for (Map<String,Object> r : rows) {
            double exp = doubleVal(r.get("experience"));
            int lv = scoreEngine.skillLevel(exp);
            r.put("level", lv);
            r.put("levelName", scoreEngine.skillLevelName(lv));
        }
        return rows;
    }

    /** 技能树节点证据：技能档案详情 + 该技能对应的获奖记录 */
    public Map<String,Object> skillEvidence(int studentId, int skillId){
        ensureStudent(studentId);
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT st.skill_id skillId, st.name skillName, st.level skillLevel, st.description," +
            " sc.category_name categoryName, sc.skill_category_id skillCategoryId," +
            " IFNULL(ss.self_eval_level,'beginner') selfEvalLevel, IFNULL(ss.experience,0) experience," +
            " IFNULL(ss.verified,0) verified, IFNULL(ss.source,'none') source, ss.note" +
            " FROM skill_tag st" +
            " LEFT JOIN skill_category sc ON sc.skill_category_id=st.skill_category_id" +
            " LEFT JOIN student_skill ss ON ss.skill_id=st.skill_id AND ss.student_id=?" +
            " WHERE st.skill_id=?", studentId, skillId);
        if (rows.isEmpty()) throw new TeamBusinessException("NOT_FOUND","技能不存在");
        Map<String,Object> result = new LinkedHashMap<>(rows.get(0));
        // 该技能所属竞赛类别（用于说明如何获得）
        List<Map<String,Object>> catRows = jdbc.queryForList(
            "SELECT cc.category_id categoryId, cc.category_name categoryName" +
            " FROM competition_skill cs JOIN competition_category cc ON cc.category_id=cs.category_id" +
            " WHERE cs.skill_id=? GROUP BY cc.category_id, cc.category_name", skillId);
        result.put("competitionCategories", catRows);
        // 该学生所有已通过申请中，贡献了本技能的获奖记录
        List<Map<String,Object>> approved = jdbc.queryForList(
            "SELECT aa.application_id applicationId, aa.application_number applicationNumber," +
            " c.competition_name competitionName, aa.competition_id competitionId, aa.award_rank awardRank, aa.award_level awardLevel," +
            " aa.competition_level competitionLevel, DATE_FORMAT(aa.award_time,'%Y-%m-%d') awardTime" +
            " FROM award_application aa LEFT JOIN competition c ON c.competition_id=aa.competition_id" +
            " WHERE aa.student_id=? AND aa.application_status='approved' ORDER BY aa.award_time DESC", studentId);
        List<Map<String,Object>> matched = new ArrayList<>();
        for (Map<String,Object> a : approved) {
            Integer catId = resolveCategoryId(intVal(a.get("competitionId")), str(a.get("competitionName")));
            if (catId == null) continue;
            Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM competition_skill WHERE category_id=? AND skill_id=?", Integer.class, catId, skillId);
            if (cnt != null && cnt > 0) {
                a.put("categoryId", catId);
                matched.add(a);
            }
        }
        result.put("awards", matched);
        return result;
    }

    /**
     * 竞赛类别关键字映射（第一版固定配置）
     * 顺序敏感：越具体的类别越靠前，避免"电子设计"被"设计"误判为视觉设计
     */
    private static final Map<String,String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put("电子设计", new String[]{"电子设计","嵌入式","集成电路","芯片","物联网","智能汽车","机器人","光电","通信","信息通信"});
        CATEGORY_KEYWORDS.put("数学建模", new String[]{"数学建模","数学模型","统计建模","数学竞赛","密码数学"});
        CATEGORY_KEYWORDS.put("程序设计", new String[]{"程序设计","软件","ACM","ICPC","CCPC","编程","蓝桥","计算机","算法","大数据","数据挖掘","数据要素"});
        CATEGORY_KEYWORDS.put("创新创业", new String[]{"创业","创客","电子商务","创新","商业","营销","金融","会计","市场调查","挑战赛","模拟"});
        CATEGORY_KEYWORDS.put("科研创新", new String[]{"挑战杯","课外学术","学术科技","科研","论文","研究生","实验","科技作品","结构设计","力学"});
        CATEGORY_KEYWORDS.put("视觉设计", new String[]{"设计","美术","视觉","艺术","插画","广告","绘画","工业设计","建筑","数字艺术"});
        CATEGORY_KEYWORDS.put("文体活动", new String[]{"音乐","舞蹈","体育","运动","钢琴","古筝","书法","颂","歌唱","跳绳","球","健美","武术","跳绳","展演","艺术节"});
        CATEGORY_KEYWORDS.put("综合素质", new String[]{"英语","翻译","演讲","作文","词汇","综合","评论","讲解","模拟法庭","外语","阅读","写作"});
    }

    /** 判断竞赛所属类别ID：优先竞赛表 category_id 外键，其次关键字映射兜底 */
    private Integer resolveCategoryId(Integer competitionId, String competitionName){
        if (competitionId != null) {
            try {
                List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT category_id categoryId FROM competition WHERE competition_id=?", competitionId);
                if (!rows.isEmpty()) {
                    Object cid = rows.get(0).get("categoryId");
                    if (cid instanceof Number n) return n.intValue();
                }
            } catch (Exception ignore) {}
        }
        return resolveCategoryId(competitionName);
    }

    /** 判断竞赛所属类别ID：优先关键字映射，其次类别名包含关系 */
    private Integer resolveCategoryId(String competitionName){
        if (competitionName == null) return null;
        List<Map<String,Object>> cats = jdbc.queryForList(
            "SELECT category_id categoryId, category_name categoryName FROM competition_category WHERE status='enabled' ORDER BY sort_order");
        for (Map.Entry<String,String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            boolean hit = false;
            for (String kw : entry.getValue()) {
                if (competitionName.contains(kw)) { hit = true; break; }
            }
            if (!hit) continue;
            for (Map<String,Object> c : cats) {
                if (entry.getKey().equals(str(c.get("categoryName")))) return intVal(c.get("categoryId"));
            }
        }
        for (Map<String,Object> c : cats) {
            String catName = str(c.get("categoryName"));
            if (catName != null && (competitionName.contains(catName) || catName.contains(competitionName))) {
                return intVal(c.get("categoryId"));
            }
        }
        return null;
    }

    /**
     * 学生在获奖审核通过后由获奖记录触发的技能验证/升级逻辑（v2.0 兼容入口）
     * 内部按两层映射 + 权重 × 放大系数计算经验值，与积分联动
     */
    @Transactional
    public void verifySkillsFromApplication(int studentId, int applicationId){
        Map<String,Object> app;
        try {
            app = jdbc.queryForMap(
                "SELECT a.competition_id competitionId, a.award_rank awardRank, a.award_level awardLevel," +
                " a.competition_level competitionLevel, a.team_id teamId, a.member_role memberRole, a.breakthrough breakthrough," +
                " c.competition_name competitionName" +
                " FROM award_application a LEFT JOIN competition c ON c.competition_id=a.competition_id WHERE a.application_id=?",
                applicationId);
        } catch (Exception e) { return; }
        String role = str(app.get("memberRole")) != null ? str(app.get("memberRole"))
                : resolveMemberRole(studentId, app.get("teamId"));
        boolean breakthrough = intVal(app.get("breakthrough")) == 1;
        Map<String,Object> calc = scoreEngine.computeAwardScore(
            intVal(app.get("competitionId")), competitionMaxLevel(app.get("competitionId")),
            str(app.get("competitionLevel")), str(app.get("awardRank")), str(app.get("awardLevel")), role,
            scoreEngine.specialFactor(breakthrough));
        grantSkillExperience(studentId, applicationId, doubleVal(calc.get("score")), role);
    }

    /** 计算勋章当前进度：优先按 unlock_condition 中的固定条件类型评估，其次按预置勋章编码兜底 */
    private int computeBadgeProgress(int studentId, String code, String conditionJson, int threshold){
        String jsonType = jsonStrParam(conditionJson, "type");
        if (jsonType != null && BADGE_CONDITION_TYPES.contains(jsonType)) {
            return computeConditionProgress(studentId, conditionJson);
        }
        switch (code) {
            case "first_award": {
                return countApprovedAwards(studentId, null);
            }
            case "national_honor": {
                return countApprovedAwards(studentId, "国家级");
            }
            case "provincial_expert": {
                // 省级及以上
                return countApprovedAwardsAtOrAbove(studentId, "省级");
            }
            case "team_star": {
                // 以队长（负责人）身份参与的团队获奖
                return countLeaderAwards(studentId);
            }
            case "versatile": {
                // 不同竞赛类别
                return distinctCategoryCount(studentId);
            }
            case "pioneer": {
                return countApprovedAwards(studentId, null);
            }
            case "golden_partner": {
                // 在同一团队获奖的次数（取最多的一个团队）
                return maxAwardsInOneTeam(studentId);
            }
            case "continuous_growth": {
                return countDistinctAwardMonths(studentId);
            }
            default:
                // 兜底：按 unlock_condition 中的固定条件类型评估
                return computeConditionProgress(studentId, conditionJson);
        }
    }

    // ========================================================
    // 勋章进度统计工具
    // ========================================================
    /** 竞赛级别从高到低排序（用于“省级及以上”类条件） */
    private static final List<String> COMPETITION_LEVEL_ORDER = List.of("国家级","省级","市级","校级");

    private int countApprovedAwards(int studentId, String level){
        Integer total = (level == null || level.isBlank())
            ? jdbc.queryForObject("SELECT COUNT(*) FROM award_application WHERE student_id=? AND application_status='approved'", Integer.class, studentId)
            : jdbc.queryForObject("SELECT COUNT(*) FROM award_application WHERE student_id=? AND application_status='approved' AND competition_level=?", Integer.class, studentId, level);
        return total == null ? 0 : total;
    }

    /** 统计某级别及以上的获奖数（例如“省级及以上”= 省级+国家级） */
    private int countApprovedAwardsAtOrAbove(int studentId, String level){
        int idx = COMPETITION_LEVEL_ORDER.indexOf(level);
        if (idx < 0) return countApprovedAwards(studentId, level);
        List<String> levels = COMPETITION_LEVEL_ORDER.subList(0, idx + 1);
        String placeholders = String.join(",", java.util.Collections.nCopies(levels.size(), "?"));
        Object[] args = new Object[levels.size() + 1];
        args[0] = studentId;
        for (int i = 0; i < levels.size(); i++) args[i + 1] = levels.get(i);
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM award_application WHERE student_id=? AND application_status='approved' AND competition_level IN (" + placeholders + ")",
            Integer.class, args);
        return total == null ? 0 : total;
    }

    /** 以队长（team.leader_id 或 team_member.is_leader）身份获得的团队奖项数 */
    private int countLeaderAwards(int studentId){
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM award_application a JOIN team t ON t.team_id=a.team_id" +
            " WHERE a.student_id=? AND a.application_status='approved' AND t.leader_id=a.student_id", Integer.class, studentId);
        if (total == null || total == 0) {
            total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM award_application a JOIN team_member m ON m.team_id=a.team_id AND m.student_id=a.student_id" +
                " WHERE a.student_id=? AND a.application_status='approved' AND m.is_leader=1", Integer.class, studentId);
        }
        return total == null ? 0 : total;
    }

    /** 学生获奖覆盖的不同竞赛类别数 */
    private int distinctCategoryCount(int studentId){
        List<Map<String,Object>> apps = jdbc.queryForList(
            "SELECT c.competition_name competitionName, a.competition_id competitionId FROM award_application a" +
            " LEFT JOIN competition c ON c.competition_id=a.competition_id" +
            " WHERE a.student_id=? AND a.application_status='approved'", studentId);
        Set<Integer> catIds = new HashSet<>();
        for (Map<String,Object> a : apps) {
            Integer catId = resolveCategoryId(intVal(a.get("competitionId")), str(a.get("competitionName")));
            if (catId != null) catIds.add(catId);
        }
        return catIds.size();
    }

    /** 在单个团队内获奖的最大次数 */
    private int maxAwardsInOneTeam(int studentId){
        Integer max = jdbc.queryForObject(
            "SELECT IFNULL(MAX(cnt),0) FROM (SELECT team_id, COUNT(*) cnt FROM award_application" +
            " WHERE student_id=? AND application_status='approved' AND team_id IS NOT NULL GROUP BY team_id) x",
            Integer.class, studentId);
        return max == null ? 0 : max;
    }

    /** 有获奖记录的不同月份数 */
    private int countDistinctAwardMonths(int studentId){
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT DATE_FORMAT(award_time,'%Y-%m')) FROM award_application WHERE student_id=? AND application_status='approved'",
            Integer.class, studentId);
        return total == null ? 0 : total;
    }

    /** 按 unlock_condition 固定条件类型（+参数）评估进度 */
    private int computeConditionProgress(int studentId, String conditionJson){
        String type = jsonStrParam(conditionJson, "type");
        if (type == null || type.isBlank()) return 0;
        String level = jsonStrParam(conditionJson, "competition_level");
        switch (type) {
            case "first_award":
            case "award_count": {
                if (level != null && !level.isBlank()) return countApprovedAwardsAtOrAbove(studentId, level);
                return countApprovedAwards(studentId, null);
            }
            case "award_level": {
                if (level == null || level.isBlank()) return countApprovedAwards(studentId, null);
                return countApprovedAwardsAtOrAbove(studentId, level);
            }
            case "team_award": {
                Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM award_application WHERE student_id=? AND application_status='approved' AND team_id IS NOT NULL", Integer.class, studentId);
                return total == null ? 0 : total;
            }
            case "team_leader_award": {
                return countLeaderAwards(studentId);
            }
            case "category_count": {
                return distinctCategoryCount(studentId);
            }
            case "team_partner_count": {
                return maxAwardsInOneTeam(studentId);
            }
            case "continuous_semesters": {
                return countDistinctAwardMonths(studentId);
            }
            default: return 0;
        }
    }

    /** 从条件 JSON 字符串中提取字符串参数 */
    private static String jsonStrParam(String json, String key){
        if (json == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** 给学生评估所有勋章 */
    public void evaluateBadges(int studentId){
        List<Map<String,Object>> badges = jdbc.queryForList("SELECT badge_id badgeId, badge_code badgeCode, unlock_condition unlockCondition, unlock_threshold unlockThreshold FROM badge WHERE status='enabled'");
        for (Map<String,Object> b : badges) {
            int bid = intVal(b.get("badgeId"));
            String code = str(b.get("badgeCode"));
            int threshold = intVal(b.get("unlockThreshold"));
            int progress = computeBadgeProgress(studentId, code, str(b.get("unlockCondition")), threshold);
            boolean unlocked = progress >= threshold;
            Map<String,Object> exist = null;
            try { exist = jdbc.queryForMap("SELECT 1 FROM student_badge WHERE student_id=? AND badge_id=?", studentId, bid); } catch (Exception ignore){}
            if (exist != null) {
                if (unlocked) {
                    jdbc.update("UPDATE student_badge SET progress=?, unlocked_at=COALESCE(unlocked_at, NOW()) WHERE student_id=? AND badge_id=?",
                            progress, studentId, bid);
                } else {
                    jdbc.update("UPDATE student_badge SET progress=? WHERE student_id=? AND badge_id=? AND unlocked_at IS NULL",
                            progress, studentId, bid);
                }
            } else if (unlocked) {
                jdbc.update("INSERT INTO student_badge(student_id,badge_id,progress,unlocked_at) VALUES(?,?,?,NOW())", studentId, bid, progress);
            }
        }
    }

    /** 给学生评估荣誉标签 */
    public void evaluateHonorTags(int studentId){
        List<Map<String,Object>> tags = jdbc.queryForList("SELECT tag_id tagId, tag_type tagType, condition_rule conditionRule FROM honor_tag WHERE status='enabled'");
        for (Map<String,Object> t : tags) {
            int tid = intVal(t.get("tagId"));
            String type = str(t.get("tagType"));
            boolean match;
            try {
                match = evaluateTagCondition(studentId, type, str(t.get("conditionRule")));
            } catch (Exception ignoreEx) {
                continue; // 单个条件评估失败跳过，不影响其它标签
            }
            if (match) {
                jdbc.update("INSERT IGNORE INTO student_honor_tag(student_id,tag_id) VALUES(?,?)", studentId, tid);
            } else {
                jdbc.update("DELETE FROM student_honor_tag WHERE student_id=? AND tag_id=?", studentId, tid);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateTagCondition(int studentId, String type, String rule){
        // 简化条件匹配
        switch (type) {
            case "award_level": {
                if (rule == null) return false;
                // 等次（A/B/C/D）：优先从 JSON 提取，兼容旧格式
                String rank = jsonStrParam(rule, "award_rank");
                if (rank == null || rank.isBlank()) rank = rule.replaceAll("[^ABCD]", "");
                if (rank == null || rank.isEmpty()) return false;
                // 可选竞赛级别过滤（国家级/省级/市级/校级），为空则不区分级别
                String compLevel = jsonStrParam(rule, "competition_level");
                List<Map<String,Object>> apps = jdbc.queryForList(
                    "SELECT award_rank awardRank, award_level awardLevel, competition_level competitionLevel" +
                    " FROM award_application WHERE student_id=? AND application_status='approved'", studentId);
                for (Map<String,Object> app : apps) {
                    if (compLevel != null && !compLevel.isBlank()
                            && !compLevel.equals(str(app.get("competitionLevel")))) continue;
                    Set<String> ranks = new LinkedHashSet<>();
                    addRankCandidates(ranks, str(app.get("awardRank")));
                    addRankCandidates(ranks, str(app.get("awardLevel")));
                    if (ranks.contains(rank)) return true;
                }
                return false;
            }
            case "competition_direction":
                if (rule == null) return false;
                // 从 rule 中提取 category_name
                int idx = rule.indexOf("category_name");
                if (idx < 0) return false;
                int s = rule.indexOf(":", idx);
                int e = rule.indexOf(",", s);
                if (e < 0) e = rule.indexOf("}", s);
                String cat = s > 0 && e > s ? rule.substring(s + 1, e).replaceAll("[\":}]", "").trim() : "";
                if (cat.isEmpty()) return false;
                Integer dirTotal = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM award_application a JOIN competition c ON c.competition_id=a.competition_id" +
                    " WHERE a.student_id=? AND a.application_status='approved' AND c.competition_name LIKE ?",
                    Integer.class, studentId, "%" + cat + "%");
                return dirTotal != null && dirTotal > 0;
            case "role":
                // 以队长（负责人）身份获得团队奖项
                return countLeaderAwards(studentId) > 0;
            case "ability": {
                Integer verified = jdbc.queryForObject("SELECT COUNT(*) FROM student_skill WHERE student_id=? AND verified=1", Integer.class, studentId);
                return verified != null && verified >= 3;
            }
            case "growth": {
                // 入学第一年获奖（grade 形如 "22级" → 2022-09-01 起一年内）
                try {
                    String grade = jdbc.queryForObject("SELECT grade FROM student WHERE student_id=?", String.class, studentId);
                    if (grade == null) return false;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{2})").matcher(grade);
                    if (!m.find()) return false;
                    int enrollYear = 2000 + Integer.parseInt(m.group(1));
                    LocalDateTime enroll = LocalDateTime.of(enrollYear, 9, 1, 0, 0);
                    Integer firstYear = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM award_application WHERE student_id=? AND application_status='approved'" +
                        " AND award_time >= ? AND award_time < ?",
                        Integer.class, studentId, java.sql.Date.valueOf(enroll.toLocalDate()),
                        java.sql.Date.valueOf(enroll.plusYears(1).toLocalDate()));
                    return firstYear != null && firstYear > 0;
                } catch (Exception growthEx) {
                    return false;
                }
            }
            default: return false;
        }
    }

    // ========================================================
    // 工具方法
    // ========================================================

    /**
     * 按候选等次列表查找积分规则（v1.0 兼容保留：等级归一化为 A/B/C/D 候选列表）
     */
    private List<String> candidateRanks(Object awardRank, Object awardLevel){
        LinkedHashSet<String> set = new LinkedHashSet<>();
        addRankCandidates(set, str(awardRank));
        addRankCandidates(set, str(awardLevel));
        return new ArrayList<>(set);
    }

    private void addRankCandidates(Set<String> set, String value){
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        set.add(v);
        switch (v) {
            case "A": case "1": case "一等奖": case "金奖": case "冠军": case "Grand Prize": case "特等奖":
                set.add("A"); break;
            case "B": case "2": case "二等奖": case "银奖": case "亚军": case "M": case "Meritorious":
                set.add("B"); break;
            case "C": case "3": case "三等奖": case "铜奖": case "季军": case "Honorable": case "优胜奖":
                set.add("C"); break;
            default:
                if (!v.matches("[ABCD]")) set.add("D");
        }
    }

    private int requireStudent(){
        Integer id = AuthContext.require().studentId();
        if (id == null) throw new TeamBusinessException("STUDENT_REQUIRED","仅学生可访问");
        return id;
    }

    private void ensureStudent(int studentId){
        Integer current = AuthContext.require().studentId();
        boolean admin = AuthContext.require().hasRole("admin");
        if (!admin && (current == null || current != studentId)) {
            throw new TeamBusinessException("FORBIDDEN","无权查看其他学生信息");
        }
    }

    private void requireAdmin(){
        if (!AuthContext.require().hasRole("admin")) throw new TeamBusinessException("FORBIDDEN","需要管理员权限");
    }

    private static String str(Map<String,Object> body,String key){
        Object v = body == null ? null : body.get(key);
        return v == null ? null : String.valueOf(v);
    }
    private static String str(Object v){
        return v == null ? null : String.valueOf(v);
    }
    private static String strOr(Map<String,Object> body,String key,String def){
        Object v = body == null ? null : body.get(key);
        return v == null ? def : String.valueOf(v);
    }
    private static Integer intOr(Map<String,Object> body,String key,Integer def){
        Object v = body == null ? null : body.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private static Double doubleOr(Map<String,Object> body,String key,Double def){
        Object v = body == null ? null : body.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private static boolean boolOr(Map<String,Object> body,String key,boolean def){
        Object v = body == null ? null : body.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(String.valueOf(v)) || "1".equals(String.valueOf(v));
    }
    private static int intVal(Object o){ return o instanceof Number n ? n.intValue() : 0; }
    private static double doubleVal(Object o){ return o instanceof Number n ? n.doubleValue() : 0d; }
}