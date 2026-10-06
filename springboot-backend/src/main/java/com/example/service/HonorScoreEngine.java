package com.example.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 荣誉积分规则引擎 v2.0（依据《竞赛技能积分系统规则设计文档》）
 *
 * 个人荣誉积分 = 竞赛基础分S(等次基础分×级别系数) × 获奖层次系数L × 获奖等级系数G × 团队角色系数R × 特殊系数K(默认1.0)
 * 技能经验值 = 个人荣誉积分 × 技能映射权重 × 经验放大系数(默认2.0)
 * 技能等级：L1≥20 / L2≥100 / L3≥300 / L4≥800（经验值自动计算，只升不降）
 *
 * 参数均从 honor_score_rule 的 v2.0 参数行读取，可在线调整；
 * 表中无参数行时按文档默认值兜底。
 */
@Component
public class HonorScoreEngine {

    private final JdbcTemplate jdbc;

    /** 等次基础分默认值 A/B/C/D */
    private static final Map<String,Double> DEFAULT_GRADE_BASE = Map.of("A",100.0,"B",80.0,"C",60.0,"D",40.0);
    /** 竞赛级别系数默认值 */
    private static final Map<String,Double> DEFAULT_LEVEL_FACTOR = Map.of("国家级",1.0,"省级",0.7,"市级",0.4,"校级",0.2,"院级",0.1);
    /** 竞赛级别顺序（用于"低于最高级别一级"判断） */
    private static final List<String> LEVEL_ORDER = List.of("国家级","省级","市级","校级","院级");
    /** 团队角色系数默认值 */
    private static final Map<String,Double> DEFAULT_ROLE_FACTOR = new HashMap<>();
    static {
        DEFAULT_ROLE_FACTOR.put("队长",1.0); DEFAULT_ROLE_FACTOR.put("leader",1.0);
        DEFAULT_ROLE_FACTOR.put("技术骨干",1.0); DEFAULT_ROLE_FACTOR.put("核心成员",0.8);
        DEFAULT_ROLE_FACTOR.put("普通成员",0.6);
    }

    public HonorScoreEngine(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    // ========================================================
    // 参数读取（v2.0 参数行，缺省回退默认值）
    // ========================================================
    private double param(String group, String key, double def){
        try {
            String v = jdbc.queryForObject(
                "SELECT param_value FROM honor_score_rule WHERE param_group=? AND param_key=? AND enabled=1 ORDER BY rule_id DESC LIMIT 1",
                String.class, group, key);
            if (v != null && !v.isBlank()) return Double.parseDouble(v.trim());
        } catch (Exception ignore) {}
        return def;
    }

    /** 竞赛等次基础分 */
    public double gradeBase(String grade){
        if (grade == null) return DEFAULT_GRADE_BASE.get("D");
        return param("竞赛等次基础分", grade + "等", DEFAULT_GRADE_BASE.getOrDefault(grade, 40.0));
    }

    /** 竞赛级别系数 */
    public double levelFactor(String level){
        if (level == null) return 0.7;
        return param("竞赛级别系数", level, DEFAULT_LEVEL_FACTOR.getOrDefault(level, 0.5));
    }

    /** 技能经验放大系数 */
    public double expAmplifier(){
        return param("技能经验放大系数", "全局", 2.0);
    }

    /**
     * 特殊系数 K：历史性突破奖（学校/学院首次获得某竞赛最高奖，管理员手动认定）取 1.5，
     * 常规情况取 1.0。取值可在线调整（honor_score_rule 参数行「特殊系数」组）。
     */
    public double specialFactor(boolean breakthrough){
        return breakthrough ? param("特殊系数", "历史性突破奖", 1.5)
                            : param("特殊系数", "常规情况", 1.0);
    }

    /** 技能等级阈值：[L1,L2,L3,L4] */
    public double[] skillLevelThresholds(){
        return new double[]{
            param("技能等级阈值","L1 了解",20),
            param("技能等级阈值","L2 熟练",100),
            param("技能等级阈值","L3 精通",300),
            param("技能等级阈值","L4 专家",800)
        };
    }

    // ========================================================
    // 五因子积分计算
    // ========================================================

    /**
     * 获奖层次系数 L：
     * 国际级 1.2；与竞赛目录最高级别一致 1.0；低于最高级别一级 0.6；校级 0.2（默认不计分）
     */
    public double awardLevelFactor(String actualLevel, String competitionMaxLevel){
        if (actualLevel == null || actualLevel.isBlank()) return 1.0;
        if (actualLevel.contains("国际") || actualLevel.equalsIgnoreCase("international")) {
            return param("获奖层次系数","国际级获奖",1.2);
        }
        if ("校级".equals(actualLevel)) return param("获奖层次系数","校级",0.2);
        if (competitionMaxLevel == null || competitionMaxLevel.isBlank()) {
            return param("获奖层次系数","与竞赛最高级别一致",1.0);
        }
        if (actualLevel.equals(competitionMaxLevel)) {
            return param("获奖层次系数","与竞赛最高级别一致",1.0);
        }
        // 低于最高级别一级（如国家级竞赛体系的省级获奖）
        int ai = LEVEL_ORDER.indexOf(actualLevel);
        int mi = LEVEL_ORDER.indexOf(competitionMaxLevel);
        if (ai < 0 || mi < 0 || ai > mi) return param("获奖层次系数","低于竞赛最高级别",0.6);
        return param("获奖层次系数","低于竞赛最高级别",0.6);
    }

    /**
     * 获奖等级系数 G（同义词自动归档）：
     * 特等奖/冠军 1.2；一等/金奖/亚军 1.0；二等/银奖/季军 0.8；三等/铜奖 0.6；优胜/入围 0.3
     */
    public double awardGradeFactor(String awardRank, String awardLevel){
        String raw = (awardLevel != null && !awardLevel.isBlank()) ? awardLevel : awardRank;
        if (raw == null || raw.isBlank()) return 1.0;
        String s = raw.trim();
        // 优先按等级文字归档
        if (s.contains("特等") || s.contains("冠军") || s.contains("Grand") || s.contains("特等")) {
            return param("获奖等级系数","特等奖/冠军",1.2);
        }
        if (s.contains("一等") || s.contains("金奖") || s.contains("亚军") || s.contains("Winner")
                || s.equals("1") || s.equals("A") || s.contains("Outstanding") || s.contains("特")) {
            // A 档：一等奖/金奖/亚军
            if (s.equals("A") || s.contains("一等") || s.contains("金奖") || s.contains("亚军")) {
                return param("获奖等级系数","一等奖/金奖/亚军",1.0);
            }
            if (s.contains("Outstanding") || s.contains("Winner")) return param("获奖等级系数","一等奖/金奖/亚军",1.0);
        }
        if (s.contains("二等") || s.contains("银奖") || s.contains("季军") || s.equals("2") || s.equals("B")
                || s.equals("M") || s.contains("Meritorious")) {
            return param("获奖等级系数","二等奖/银奖/季军",0.8);
        }
        if (s.contains("三等") || s.contains("铜奖") || s.equals("3") || s.equals("C")) {
            return param("获奖等级系数","三等奖/铜奖",0.6);
        }
        if (s.contains("优胜") || s.contains("优秀奖") || s.contains("入围") || s.contains("决赛") || s.contains("Honorable")
                || s.equals("D") || s.equals("4")) {
            return param("获奖等级系数","优秀奖/优胜奖/入围奖",0.3);
        }
        if (s.contains("参与") || s.contains("成功参赛")) {
            return param("获奖等级系数","参与未获奖",0.05);
        }
        // A/B/C/D 直接档位
        if (s.equals("A")) return param("获奖等级系数","一等奖/金奖/亚军",1.0);
        if (s.equals("B")) return param("获奖等级系数","二等奖/银奖/季军",0.8);
        if (s.equals("C")) return param("获奖等级系数","三等奖/铜奖",0.6);
        if (s.equals("D")) return param("获奖等级系数","优秀奖/优胜奖/入围奖",0.3);
        return 1.0;
    }

    /** 团队角色系数 R */
    public double roleFactor(String role){
        if (role == null || role.isBlank()) return 1.0; // 个人参赛
        String s = role.trim();
        if (s.contains("队长") || s.contains("负责人") || s.contains("第一完成") || s.equalsIgnoreCase("leader")) {
            return param("团队角色系数","队长/第一完成人",1.0);
        }
        if (s.contains("骨干")) return param("团队角色系数","技术骨干/第2-3完成人",1.0);
        if (s.contains("核心")) return param("团队角色系数","核心成员",0.8);
        if (s.contains("普通")) return param("团队角色系数","普通成员",0.6);
        return param("团队角色系数","个人参赛（无团队）",1.0);
    }

    // ========================================================
    // 技能等级计算（经验值自动计算）
    // ========================================================
    public int skillLevel(double exp){
        double[] t = skillLevelThresholds();
        if (exp >= t[3]) return 4;
        if (exp >= t[2]) return 3;
        if (exp >= t[1]) return 2;
        if (exp >= t[0]) return 1;
        return 0;
    }

    public String skillLevelName(int level){
        return switch (level) {
            case 4 -> "L4 专家";
            case 3 -> "L3 精通";
            case 2 -> "L2 熟练";
            case 1 -> "L1 了解";
            default -> "未定级";
        };
    }

    /** 计算下一等级差（用于进度条），无下一级返回 -1 */
    public double nextLevelGap(double exp){
        double[] t = skillLevelThresholds();
        int lv = skillLevel(exp);
        if (lv >= 4) return -1;
        return t[lv] - exp;
    }

    /** 竞赛基础分 S：优先取 competition.base_score 冗余值，否则按等次×级别计算 */
    public double competitionBaseScore(Integer competitionId, String fallbackGrade, String fallbackLevel){
        if (competitionId != null) {
            try {
                Map<String,Object> row = jdbc.queryForMap(
                    "SELECT IFNULL(base_score,0) baseScore, grade, override_grade, override_level, award_rank FROM competition WHERE competition_id=?", competitionId);
                Double base = toDouble(row.get("baseScore"));
                String grade = str(row.get("override_grade")) != null ? str(row.get("override_grade"))
                        : (str(row.get("grade")) != null ? str(row.get("grade")) : str(row.get("award_rank")));
                String level = str(row.get("override_level")) != null ? str(row.get("override_level")) : fallbackLevel;
                if (base != null && base > 0) return base;
                if (grade != null && level != null) return gradeBase(grade) * levelFactor(level);
            } catch (Exception ignore) {}
        }
        if (fallbackGrade == null || fallbackLevel == null) return 60.0 * 0.7;
        return gradeBase(fallbackGrade) * levelFactor(fallbackLevel);
    }

    /**
     * 计算单条获奖的个人荣誉积分。
     * 返回 map：score（积分）、base、levelFactor、gradeFactor、roleFactor、specialFactor（计算快照）
     */
    public Map<String,Object> computeAwardScore(Integer competitionId, String competitionMaxLevel,
                                                 String actualLevel, String awardRank, String awardLevel,
                                                 String role, double specialFactor){
        double base = competitionBaseScore(competitionId,
                normalizeGrade(awardRank), actualLevel != null ? actualLevel : competitionMaxLevel);
        double lf = awardLevelFactor(actualLevel, competitionMaxLevel);
        double gf = awardGradeFactor(awardRank, awardLevel);
        double rf = roleFactor(role);
        double score = base * lf * gf * rf * specialFactor;
        // 校级默认不计分（系数0.2但默认关闭）——仅记录
        if ("校级".equals(actualLevel) && !schoolScoreEnabled()) score = 0;

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("score", Math.round(score * 10) / 10.0);
        result.put("base", base);
        result.put("levelFactor", lf);
        result.put("gradeFactor", gf);
        result.put("roleFactor", rf);
        result.put("specialFactor", specialFactor);
        return result;
    }

    /** 校级计分开关（默认关闭） */
    private boolean schoolScoreEnabled(){
        try {
            Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM honor_score_rule WHERE param_group='反刷分规则' AND param_key='校级计分开关' AND param_value='开启' AND enabled=1", Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception ignore) { return false; }
    }

    private static String normalizeGrade(String awardRank){
        if (awardRank == null) return "D";
        String s = awardRank.trim().toUpperCase();
        if (s.startsWith("A") || s.contains("一等") || s.contains("特等")) return "A";
        if (s.startsWith("B") || s.contains("二等")) return "B";
        if (s.startsWith("C") || s.contains("三等")) return "C";
        return "D";
    }

    private static String str(Object o){ return o == null ? null : String.valueOf(o); }
    private static Double toDouble(Object o){ return o instanceof Number n ? n.doubleValue() : null; }
}
