package com.example.certificate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险规则服务
 * 负责风险规则的加载、配置更新，以及基于规则的申请风险等级计算：
 * - 完全相同文件 / pHash相似度阈值 / 证书编号重复
 * - 姓名不一致 / 奖项不一致 / 时间不一致
 * - OCR识别置信度阈值（OCR失败同样触发，转人工审核）
 * 同时负责：风险等级计算、是否需要人工复核判定、人工标记（正常复用/异常重复/无法判断）的保存。
 */
@Service
public class RiskRuleService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public RiskRuleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================== 规则配置 ====================

    /** 加载全部风险规则 */
    public List<Map<String, Object>> listRules() {
        return jdbc.queryForList(
                "SELECT rule_id AS ruleId, rule_key AS ruleKey, rule_name AS ruleName, description, " +
                        "rule_type AS ruleType, threshold_value AS thresholdValue, threshold_unit AS thresholdUnit, " +
                        "config_json AS configJson, risk_level AS riskLevel, need_manual_review AS needManualReview, " +
                        "enabled, update_time AS updateTime FROM risk_rule ORDER BY rule_id");
    }

    /** 更新规则配置（启用/停用、阈值、风险等级、是否人工复核、比对字段） */
    public void updateRules(List<Map<String, Object>> rules) {
        for (Map<String, Object> r : rules) {
            String ruleKey = (String) r.get("ruleKey");
            if (ruleKey == null || ruleKey.isBlank()) continue;
            StringBuilder sql = new StringBuilder("UPDATE risk_rule SET update_time = NOW()");
            List<Object> args = new ArrayList<>();
            if (r.containsKey("enabled")) {
                sql.append(", enabled = ?");
                args.add(toInt(r.get("enabled")));
            }
            if (r.containsKey("thresholdValue") && r.get("thresholdValue") != null) {
                sql.append(", threshold_value = ?");
                args.add(Double.parseDouble(String.valueOf(r.get("thresholdValue"))));
            }
            if (r.containsKey("riskLevel") && r.get("riskLevel") != null) {
                sql.append(", risk_level = ?");
                args.add(String.valueOf(r.get("riskLevel")));
            }
            if (r.containsKey("needManualReview")) {
                sql.append(", need_manual_review = ?");
                args.add(toInt(r.get("needManualReview")));
            }
            if (r.containsKey("configJson") && r.get("configJson") != null) {
                sql.append(", config_json = ?");
                args.add(String.valueOf(r.get("configJson")));
            }
            sql.append(" WHERE rule_key = ?");
            args.add(ruleKey);
            jdbc.update(sql.toString(), args.toArray());
        }
    }

    /** 读取单条规则（无记录时返回 null） */
    public Map<String, Object> getRule(String ruleKey) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM risk_rule WHERE rule_key = ?", ruleKey);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean isRuleEnabled(String ruleKey) {
        Map<String, Object> rule = getRule(ruleKey);
        return rule != null && toInt(rule.get("enabled")) == 1;
    }

    public double getThreshold(String ruleKey, double defaultValue) {
        Map<String, Object> rule = getRule(ruleKey);
        if (rule == null) return defaultValue;
        Object v = rule.get("threshold_value");
        if (v == null) return defaultValue;
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** OCR 参与比对的字段列表（来自 OCR_COMPARE_FIELDS 规则的 configJson） */
    @SuppressWarnings("unchecked")
    public List<String> getOcrCompareFields() {
        Map<String, Object> rule = getRule("OCR_COMPARE_FIELDS");
        if (rule == null || !isRuleEnabled("OCR_COMPARE_FIELDS")) {
            return List.of("name", "competition", "awardLevel", "awardTime");
        }
        Object cfg = rule.get("config_json");
        if (cfg == null) return List.of("name", "competition", "awardLevel", "awardTime");
        try {
            JsonNode arr = mapper.readTree(String.valueOf(cfg));
            List<String> fields = new ArrayList<>();
            arr.forEach(n -> fields.add(n.asText()));
            return fields.isEmpty() ? List.of("name", "competition", "awardLevel", "awardTime") : fields;
        } catch (Exception e) {
            return List.of("name", "competition", "awardLevel", "awardTime");
        }
    }

    // ==================== 风险等级计算 ====================

    /**
     * 计算并保存申请的综合风险等级
     * 规则触发时按规则配置的风险等级取最高值，任一触发规则要求人工复核则 need_manual_review = 1。
     * 已人工标记(normal_reuse)的申请不再产生人工复核报警（避免反复报警）。
     */
    public Map<String, Object> computeAndSaveRisk(Integer applicationId) {
        List<String> reasons = new ArrayList<>();
        String level = "none";
        boolean needManual = false;

        // ---- 0. 文件类型/大小校验失败 ----
        List<Map<String, Object>> failedChecks = jdbc.queryForList(
                "SELECT file_id FROM duplicate_check_record WHERE application_id = ? AND check_status = 'failed'",
                applicationId);
        if (!failedChecks.isEmpty()) {
            reasons.add("有 " + failedChecks.size() + " 个证书文件未通过校验（类型或大小不符）");
            level = maxLevel(level, "medium");
            needManual = true;
        }

        // ---- 1. 完全相同文件 / pHash 相似 ----
        List<Map<String, Object>> matches = jdbc.queryForList(
                "SELECT m.*, aa.application_number AS matchedApplicationNumber FROM duplicate_match m " +
                        "LEFT JOIN award_application aa ON m.matched_application_id = aa.application_id " +
                        "WHERE m.application_id = ? AND m.matched_application_id <> ?", applicationId, applicationId);
        double phashThreshold = getThreshold("PHASH_SIMILAR", 0.90);
        boolean exactEnabled = isRuleEnabled("EXACT_DUPLICATE");
        boolean phashEnabled = isRuleEnabled("PHASH_SIMILAR");

        for (Map<String, Object> m : matches) {
            boolean teamRelated = toInt(m.get("team_related")) == 1;
            boolean handled = toInt(m.get("handled")) == 1;
            if (teamRelated) {
                continue; // 同一团队成员允许上传相同证书
            }
            if (handled) {
                continue; // 已人工处理（正常复用），不再报警
            }
            String type = String.valueOf(m.get("match_type"));
            double sim = m.get("similarity") == null ? 0 : Double.parseDouble(String.valueOf(m.get("similarity")));
            String matchedNo = String.valueOf(m.get("matchedApplicationNumber"));
            if ("exact".equals(type) && exactEnabled) {
                reasons.add("证书文件与申请[" + matchedNo + "]完全相同（SHA-256一致）");
                level = maxLevel(level, ruleLevel("EXACT_DUPLICATE"));
                needManual = needManual || ruleNeedManual("EXACT_DUPLICATE");
            } else if ("phash".equals(type) && phashEnabled && sim >= phashThreshold) {
                reasons.add("证书图片与申请[" + matchedNo + "]高度相似（相似度 " + Math.round(sim * 100) + "%）");
                level = maxLevel(level, ruleLevel("PHASH_SIMILAR"));
                needManual = needManual || ruleNeedManual("PHASH_SIMILAR");
            }
        }

        // ---- 2. OCR 比对结果 ----
        List<Map<String, Object>> ocrRecords = jdbc.queryForList(
                "SELECT * FROM ocr_record WHERE application_id = ? ORDER BY ocr_id DESC", applicationId);
        Map<String, Map<String, String>> ocrFieldResult = new HashMap<>();
        for (Map<String, Object> ocr : ocrRecords) {
            String detailJson = (String) ocr.get("compare_detail");
            if (detailJson == null) continue;
            try {
                JsonNode arr = mapper.readTree(detailJson);
                for (JsonNode d : arr) {
                    ocrFieldResult.put(d.path("field").asText(),
                            Map.of("result", d.path("result").asText(),
                                    "recognized", d.path("recognized").asText(""),
                                    "declared", d.path("declared").asText("")));
                }
            } catch (Exception ignored) {
            }
        }

        checkFieldMismatch(ocrFieldResult, "name", "NAME_MISMATCH", "姓名", reasons);
        level = maxLevel(level, levelFromField(ocrFieldResult, "name", "NAME_MISMATCH"));
        needManual = needManual || manualFromField(ocrFieldResult, "name", "NAME_MISMATCH");

        checkFieldMismatch(ocrFieldResult, "awardLevel", "AWARD_MISMATCH", "获奖等级", reasons);
        level = maxLevel(level, levelFromField(ocrFieldResult, "awardLevel", "AWARD_MISMATCH"));
        needManual = needManual || manualFromField(ocrFieldResult, "awardLevel", "AWARD_MISMATCH");

        checkFieldMismatch(ocrFieldResult, "awardTime", "TIME_MISMATCH", "获奖时间", reasons);
        level = maxLevel(level, levelFromField(ocrFieldResult, "awardTime", "TIME_MISMATCH"));
        needManual = needManual || manualFromField(ocrFieldResult, "awardTime", "TIME_MISMATCH");

        // ---- 3. 证书编号重复 ----
        if (isRuleEnabled("CERT_NO_DUPLICATE")) {
            List<Map<String, Object>> dupNos = jdbc.queryForList(
                    "SELECT DISTINCT aa.application_number FROM ocr_record o1 " +
                            "JOIN ocr_record o2 ON o1.recognized_certificate_no = o2.recognized_certificate_no " +
                            "LEFT JOIN award_application aa ON o2.application_id = aa.application_id " +
                            "WHERE o1.application_id = ? AND o2.application_id <> ? " +
                            "AND o1.recognized_certificate_no IS NOT NULL AND o1.recognized_certificate_no <> ''",
                    applicationId, applicationId);
            for (Map<String, Object> dup : dupNos) {
                reasons.add("证书编号[" + String.valueOf(dup.get("application_number")) + "]与其他申请重复");
                level = maxLevel(level, ruleLevel("CERT_NO_DUPLICATE"));
                needManual = needManual || ruleNeedManual("CERT_NO_DUPLICATE");
            }
        }

        // ---- 4. OCR 置信度 / 失败 ----
        if (isRuleEnabled("OCR_LOW_CONFIDENCE")) {
            double confThreshold = getThreshold("OCR_LOW_CONFIDENCE", 0.80);
            for (Map<String, Object> ocr : ocrRecords) {
                String status = String.valueOf(ocr.get("ocr_status"));
                if ("failed".equals(status)) {
                    reasons.add("OCR识别失败（" + String.valueOf(ocr.get("error_message")) + "），需人工审核");
                    level = maxLevel(level, ruleLevel("OCR_LOW_CONFIDENCE"));
                    needManual = needManual || ruleNeedManual("OCR_LOW_CONFIDENCE");
                } else if ("success".equals(status) && ocr.get("overall_confidence") != null) {
                    double conf = Double.parseDouble(String.valueOf(ocr.get("overall_confidence")));
                    if (conf < confThreshold) {
                        reasons.add("OCR识别置信度偏低（" + Math.round(conf * 100) + "%，低于阈值 " + Math.round(confThreshold * 100) + "%）");
                        level = maxLevel(level, ruleLevel("OCR_LOW_CONFIDENCE"));
                        needManual = needManual || ruleNeedManual("OCR_LOW_CONFIDENCE");
                    }
                }
            }
        }

        // ---- 5. 人工标记处理 ----
        Map<String, Object> existing = getRiskRow(applicationId);
        String manualMark = existing != null ? (String) existing.get("manual_mark") : null;
        if ("normal_reuse".equals(manualMark)) {
            // 审核人员已标记正常复用：不再触发人工复核报警
            needManual = false;
        } else if ("abnormal_duplicate".equals(manualMark)) {
            level = "high";
            needManual = true;
            reasons.add("审核人员已标记为异常重复");
        } else if ("undetermined".equals(manualMark)) {
            needManual = true;
        }

        // 保存/更新 application_risk
        String reasonsJson;
        try {
            reasonsJson = mapper.writeValueAsString(reasons);
        } catch (Exception e) {
            reasonsJson = "[]";
        }
        jdbc.update("INSERT INTO application_risk (application_id, risk_level, need_manual_review, risk_reasons, update_time) " +
                        "VALUES (?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE risk_level = VALUES(risk_level), need_manual_review = VALUES(need_manual_review), " +
                        "risk_reasons = VALUES(risk_reasons), update_time = NOW()",
                applicationId, level, needManual ? 1 : 0, reasonsJson);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationId", applicationId);
        result.put("riskLevel", level);
        result.put("needManualReview", needManual);
        result.put("riskReasons", reasons);
        return result;
    }

    private void checkFieldMismatch(Map<String, Map<String, String>> ocrFieldResult, String field,
                                    String ruleKey, String label, List<String> reasons) {
        if (!isRuleEnabled(ruleKey)) return;
        Map<String, String> r = ocrFieldResult.get(field);
        if (r != null && "inconsistent".equals(r.get("result"))) {
            reasons.add("OCR识别" + label + "与申报不一致（申报: " + r.get("declared") + " / 识别: " + r.get("recognized") + "）");
        }
    }

    private String levelFromField(Map<String, Map<String, String>> ocrFieldResult, String field, String ruleKey) {
        Map<String, String> r = ocrFieldResult.get(field);
        if (r != null && "inconsistent".equals(r.get("result")) && isRuleEnabled(ruleKey)) {
            return ruleLevel(ruleKey);
        }
        return "none";
    }

    private boolean manualFromField(Map<String, Map<String, String>> ocrFieldResult, String field, String ruleKey) {
        Map<String, String> r = ocrFieldResult.get(field);
        return r != null && "inconsistent".equals(r.get("result")) && isRuleEnabled(ruleKey) && ruleNeedManual(ruleKey);
    }

    // ==================== 风险信息查询 ====================

    private Map<String, Object> getRiskRow(Integer applicationId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM application_risk WHERE application_id = ?", applicationId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取申请的完整风险信息（供审核页面展示）：
     * 风险等级 / 原因 / 人工标记 + 相似证书明细 + OCR识别结果 + 关联申请与团队
     */
    public Map<String, Object> getRiskInfo(Integer applicationId) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> risk = getRiskRow(applicationId);
        Map<String, Object> riskInfo = new LinkedHashMap<>();
        riskInfo.put("applicationId", applicationId);
        riskInfo.put("riskLevel", risk != null ? risk.get("risk_level") : "none");
        riskInfo.put("needManualReview", risk != null ? risk.get("need_manual_review") : 0);
        riskInfo.put("riskReasons", parseJsonArray(risk != null ? (String) risk.get("risk_reasons") : null));
        riskInfo.put("manualMark", risk != null ? risk.get("manual_mark") : null);
        riskInfo.put("manualRemark", risk != null ? risk.get("manual_remark") : null);
        riskInfo.put("manualReviewer", risk != null ? risk.get("manual_reviewer") : null);
        riskInfo.put("manualTime", risk != null ? risk.get("manual_time") : null);
        riskInfo.put("finalOpinion", risk != null ? risk.get("final_opinion") : null);
        riskInfo.put("handled", risk != null && risk.get("manual_mark") != null);
        result.put("risk", riskInfo);

        // 相似证书明细（含关联申请学生、团队信息）
        List<Map<String, Object>> matches = jdbc.queryForList(
                "SELECT m.match_id AS matchId, m.match_type AS matchType, m.similarity, m.team_related AS teamRelated, " +
                        "m.handled, m.matched_application_id AS matchedApplicationId, " +
                        "aa.application_number AS matchedApplicationNumber, aa.project_name AS matchedProjectName, " +
                        "aa.application_status AS matchedStatus, aa.award_level AS matchedAwardLevel, aa.award_time AS matchedAwardTime, " +
                        "s.student_number AS matchedStudentNumber, s.student_name AS matchedStudentName, " +
                        "c.competition_name AS matchedCompetitionName, " +
                        "aa.team_id AS matchedTeamId, t.name AS matchedTeamName, " +
                        "af.file_name AS matchedFileName, af.file_path AS matchedFilePath " +
                        "FROM duplicate_match m " +
                        "LEFT JOIN award_application aa ON m.matched_application_id = aa.application_id " +
                        "LEFT JOIN student s ON aa.student_id = s.student_id " +
                        "LEFT JOIN competition c ON aa.competition_id = c.competition_id " +
                        "LEFT JOIN team t ON aa.team_id = t.team_id " +
                        "LEFT JOIN application_file af ON m.matched_file_id = af.file_id " +
                        "WHERE m.application_id = ? AND m.matched_application_id <> ? " +
                        "ORDER BY m.similarity DESC", applicationId, applicationId);

        // 本申请的团队信息
        List<Map<String, Object>> myTeams = jdbc.queryForList(
                "SELECT aa.team_id AS teamId, t.name AS teamName, t.leader_id AS leaderId, " +
                        "(SELECT student_name FROM student WHERE student_id = t.leader_id) AS leaderName " +
                        "FROM award_application aa LEFT JOIN team t ON aa.team_id = t.team_id " +
                        "WHERE aa.application_id = ?", applicationId);
        result.put("myTeam", myTeams.isEmpty() || myTeams.get(0).get("teamId") == null ? null : myTeams.get(0));

        // 关联团队与成员（对每个匹配申请，展示其团队成员）
        for (Map<String, Object> m : matches) {
            Object matchedTeamId = m.get("matchedTeamId");
            if (matchedTeamId != null) {
                List<Map<String, Object>> members = jdbc.queryForList(
                        "SELECT tm.external_name AS memberName, tm.external_number AS memberNumber, " +
                                "tm.is_leader AS isLeader, s.student_name AS studentName " +
                                "FROM team_member tm LEFT JOIN student s ON tm.student_id = s.student_id " +
                                "WHERE tm.team_id = ? ORDER BY tm.sort_order", matchedTeamId);
                m.put("matchedTeamMembers", members);
            } else {
                m.put("matchedTeamMembers", new ArrayList<>());
            }
        }
        result.put("duplicateMatches", matches);

        // OCR 识别结果
        List<Map<String, Object>> ocrList = new ArrayList<>();
        List<Map<String, Object>> ocrRows = jdbc.queryForList(
                "SELECT o.*, af.file_name AS fileName, af.file_path AS filePath " +
                        "FROM ocr_record o LEFT JOIN application_file af ON o.file_id = af.file_id " +
                        "WHERE o.application_id = ? ORDER BY o.ocr_id DESC", applicationId);
        for (Map<String, Object> o : ocrRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ocrId", o.get("ocr_id"));
            item.put("fileName", o.get("fileName"));
            item.put("filePath", o.get("filePath"));
            item.put("ocrStatus", o.get("ocr_status"));
            item.put("rawText", o.get("raw_text"));
            item.put("recognizedName", o.get("recognized_name"));
            item.put("recognizedCompetition", o.get("recognized_competition"));
            item.put("recognizedAwardLevel", o.get("recognized_award_level"));
            item.put("recognizedAwardTime", o.get("recognized_award_time"));
            item.put("recognizedCertificateNo", o.get("recognized_certificate_no"));
            item.put("overallConfidence", o.get("overall_confidence"));
            item.put("compareResult", o.get("compare_result"));
            item.put("errorMessage", o.get("error_message"));
            item.put("compareDetail", parseDetailJson((String) o.get("compare_detail")));
            ocrList.add(item);
        }
        result.put("ocrRecords", ocrList);

        return result;
    }

    /** 保存人工标记（normal_reuse-正常复用 / abnormal_duplicate-异常重复 / undetermined-无法判断） */
    public void saveManualMark(Integer applicationId, String markType, String remark, String reviewer) {
        if (!"normal_reuse".equals(markType) && !"abnormal_duplicate".equals(markType) && !"undetermined".equals(markType)) {
            throw new IllegalArgumentException("无效的标记类型");
        }
        jdbc.update("INSERT INTO application_risk (application_id, manual_mark, manual_remark, manual_reviewer, manual_time, update_time) " +
                        "VALUES (?, ?, ?, ?, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE manual_mark = VALUES(manual_mark), manual_remark = VALUES(manual_remark), " +
                        "manual_reviewer = VALUES(manual_reviewer), manual_time = NOW(), update_time = NOW()",
                applicationId, markType, remark, reviewer);
        // 标记正常复用时，把该申请相关的相似匹配置为已处理，避免反复报警
        if ("normal_reuse".equals(markType)) {
            jdbc.update("UPDATE duplicate_match SET handled = 1 WHERE application_id = ?", applicationId);
        }
        // 重新计算风险（人工标记会影响风险等级和复核标记）
        computeAndSaveRisk(applicationId);
    }

    /** 保存审核人员最终意见 */
    public void saveFinalOpinion(Integer applicationId, String finalOpinion) {
        jdbc.update("INSERT INTO application_risk (application_id, final_opinion, update_time) VALUES (?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE final_opinion = VALUES(final_opinion), update_time = NOW()",
                applicationId, finalOpinion);
    }

    // ==================== 工具方法 ====================

    private String ruleLevel(String ruleKey) {
        Map<String, Object> rule = getRule(ruleKey);
        return rule != null && rule.get("risk_level") != null ? String.valueOf(rule.get("risk_level")) : "medium";
    }

    private boolean ruleNeedManual(String ruleKey) {
        Map<String, Object> rule = getRule(ruleKey);
        return rule == null || toInt(rule.get("need_manual_review")) == 1;
    }

    private String maxLevel(String a, String b) {
        int ra = levelRank(a);
        int rb = levelRank(b);
        return ra >= rb ? a : b;
    }

    private int levelRank(String level) {
        return switch (level == null ? "none" : level) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> parseJsonArray(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;
        try {
            JsonNode arr = mapper.readTree(json);
            arr.forEach(n -> list.add(n.asText()));
        } catch (Exception ignored) {
        }
        return list;
    }

    private List<Map<String, Object>> parseDetailJson(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;
        try {
            JsonNode arr = mapper.readTree(json);
            arr.forEach(n -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("field", n.path("field").asText());
                m.put("label", n.path("label").asText());
                m.put("declared", n.path("declared").asText());
                m.put("recognized", n.path("recognized").asText());
                m.put("result", n.path("result").asText());
                list.add(m);
            });
        } catch (Exception ignored) {
        }
        return list;
    }
}
