package com.example.certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 证书查重服务
 * 流程：上传证书文件 -> 文件类型/大小校验 -> 计算SHA-256（完全重复判断）
 *       -> 图片格式转换与标准化 -> 计算pHash -> 查询历史相似证书（按相似度阈值）
 *       -> 判断团队关系（同一团队成员允许上传相同证书）
 *       -> 保存每次查重结果（含关联申请和相似度）
 *       -> OCR 智能预检 -> 风险等级计算
 */
@Service
public class CertificateCheckService {

    private final JdbcTemplate jdbc;
    private final OcrService ocrService;
    private final RiskRuleService riskRuleService;

    @Value("${file.upload.path}")
    private String uploadPath;

    /** 查重后台执行器（单线程串行，避免并发查重互相干扰） */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "certificate-check");
        t.setDaemon(true);
        return t;
    });

    /** 相似度记录下限：低于此值的历史证书不记录（风险阈值由规则配置控制，通常更高） */
    private static final double MATCH_FLOOR = 0.75;

    public CertificateCheckService(JdbcTemplate jdbc, OcrService ocrService, RiskRuleService riskRuleService) {
        this.jdbc = jdbc;
        this.ocrService = ocrService;
        this.riskRuleService = riskRuleService;
    }

    // ==================== 入口 ====================

    /** 异步执行证书预检（申请提交后调用，不阻塞提交） */
    public void checkApplicationAsync(Integer applicationId) {
        executor.submit(() -> {
            try {
                checkApplication(applicationId);
            } catch (Exception e) {
                System.err.println("证书预检失败 applicationId=" + applicationId + ": " + e.getMessage());
            }
        });
    }

    /** 同步重新预检（审核端手动触发） */
    public Map<String, Object> recheckApplication(Integer applicationId) {
        try {
            checkApplication(applicationId);
        } catch (Exception e) {
            throw new RuntimeException("证书预检失败: " + e.getMessage(), e);
        }
        return riskRuleService.getRiskInfo(applicationId);
    }

    /**
     * 对申请的全部证书文件执行预检（查重 + OCR + 风险计算）
     */
    public void checkApplication(Integer applicationId) throws Exception {
        // 清理该申请旧的预检结果（重新查重时覆盖）
        jdbc.update("DELETE FROM duplicate_match WHERE application_id = ?", applicationId);
        jdbc.update("DELETE FROM duplicate_check_record WHERE application_id = ?", applicationId);
        jdbc.update("DELETE FROM ocr_record WHERE application_id = ?", applicationId);
        jdbc.update("DELETE FROM certificate_fingerprint WHERE application_id = ?", applicationId);

        // 申报字段（用于OCR比对）
        Map<String, String> declared = loadDeclaredFields(applicationId);
        List<String> memberNames = loadTeamMemberNames(applicationId);
        List<String> compareFields = riskRuleService.getOcrCompareFields();

        List<Map<String, Object>> files = jdbc.queryForList(
                "SELECT file_id, file_name, file_path, file_type, file_size FROM application_file WHERE application_id = ?",
                applicationId);

        for (Map<String, Object> file : files) {
            checkOneFile(applicationId, file, declared, memberNames, compareFields);
        }

        // 计算综合风险等级
        riskRuleService.computeAndSaveRisk(applicationId);
    }

    // ==================== 单文件查重 ====================

    private void checkOneFile(Integer applicationId, Map<String, Object> file,
                              Map<String, String> declared, List<String> memberNames, List<String> compareFields) {
        Integer fileId = ((Number) file.get("file_id")).intValue();
        String fileName = String.valueOf(file.get("file_name"));
        String filePath = String.valueOf(file.get("file_path"));
        String fileType = String.valueOf(file.get("file_type"));
        int fileSize = file.get("file_size") == null ? 0 : ((Number) file.get("file_size")).intValue();

        File diskFile = resolveDiskFile(filePath);
        if (diskFile == null || !diskFile.exists()) {
            insertCheckRecord(applicationId, fileId, null, null, false, 0, null, "failed");
            return;
        }

        // 文件类型、大小校验
        if (!CertificateFingerprintUtil.isAllowed(fileType)) {
            insertCheckRecord(applicationId, fileId, null, null, false, 0, null, "failed");
            return;
        }
        if (diskFile.length() > CertificateFingerprintUtil.MAX_FILE_SIZE) {
            insertCheckRecord(applicationId, fileId, null, null, false, 0, null, "failed");
            return;
        }

        try {
            // SHA-256
            String sha256 = CertificateFingerprintUtil.sha256Hex(diskFile);

            // 图片标准化 + pHash
            String phash = null;
            String standardPath = null;
            BufferedImageHolder holder = null;
            if (CertificateFingerprintUtil.isImage(fileType)) {
                try {
                    java.awt.image.BufferedImage std = CertificateFingerprintUtil.standardize(diskFile);
                    if (std != null) {
                        holder = new BufferedImageHolder(std);
                        phash = CertificateFingerprintUtil.phash(std);
                        standardPath = saveStandardImage(applicationId, std);
                    }
                } catch (Exception e) {
                    System.err.println("图片标准化失败 fileId=" + fileId + ": " + e.getMessage());
                }
            }

            // 保存指纹
            jdbc.update("INSERT INTO certificate_fingerprint (application_id, file_id, sha256, phash, standard_path, create_time) " +
                            "VALUES (?, ?, ?, ?, ?, NOW())",
                    applicationId, fileId, sha256, phash, standardPath);

            // ---- 查询历史相似证书 ----
            // 1) 完全重复（SHA-256 相同，排除本申请）
            List<Map<String, Object>> exactHits = jdbc.queryForList(
                    "SELECT cf.file_id AS matchedFileId, cf.application_id AS matchedApplicationId, aa.student_id AS matchedStudentId " +
                            "FROM certificate_fingerprint cf " +
                            "LEFT JOIN award_application aa ON cf.application_id = aa.application_id " +
                            "WHERE cf.sha256 = ? AND cf.application_id <> ? AND cf.file_id <> ?",
                    sha256, applicationId, fileId);

            // 2) pHash 相似（在历史指纹中筛选，排除本申请）
            List<Map<String, Object>> phashCandidates = new ArrayList<>();
            if (phash != null) {
                List<Map<String, Object>> allPhash = jdbc.queryForList(
                        "SELECT cf.file_id AS matchedFileId, cf.application_id AS matchedApplicationId, cf.phash, aa.student_id AS matchedStudentId " +
                                "FROM certificate_fingerprint cf " +
                                "LEFT JOIN award_application aa ON cf.application_id = aa.application_id " +
                                "WHERE cf.phash IS NOT NULL AND cf.application_id <> ? AND cf.file_id <> ?", applicationId, fileId);
                Set<Integer> exactFileIds = new HashSet<>();
                for (Map<String, Object> h : exactHits) {
                    exactFileIds.add(((Number) h.get("matchedFileId")).intValue());
                }
                for (Map<String, Object> c : allPhash) {
                    double sim = CertificateFingerprintUtil.similarity(phash, String.valueOf(c.get("phash")));
                    if (sim >= MATCH_FLOOR && !exactFileIds.contains(((Number) c.get("matchedFileId")).intValue())) {
                        c.put("similarity", sim);
                        phashCandidates.add(c);
                    }
                }
                phashCandidates.sort((a, b) -> Double.compare((Double) b.get("similarity"), (Double) a.get("similarity")));
            }

            // ---- 团队关系判断 & 人工处理标记判断 ----
            List<Map<String, Object>> allMatches = new ArrayList<>();
            for (Map<String, Object> h : exactHits) {
                allMatches.add(buildMatch(applicationId, fileId, h, "exact", 1.0));
            }
            for (Map<String, Object> c : phashCandidates) {
                allMatches.add(buildMatch(applicationId, fileId, c, "phash", (Double) c.get("similarity")));
            }

            boolean exactDuplicate = !exactHits.isEmpty();
            int similarCount = allMatches.size();
            double maxSim = 0;
            for (Map<String, Object> m : allMatches) {
                maxSim = Math.max(maxSim, (Double) m.get("similarity"));
            }

            // 保存查重记录
            Integer checkId = insertCheckRecord(applicationId, fileId, sha256, phash,
                    exactDuplicate, similarCount, similarCount > 0 ? maxSim : null, "done");

            // 保存匹配明细
            for (Map<String, Object> m : allMatches) {
                jdbc.update("INSERT INTO duplicate_match (check_id, application_id, file_id, matched_application_id, " +
                                "matched_file_id, matched_student_id, match_type, similarity, team_related, handled, create_time) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                        checkId, applicationId, fileId,
                        m.get("matchedApplicationId"), m.get("matchedFileId"), m.get("matchedStudentId"),
                        m.get("matchType"), Math.round((Double) m.get("similarity") * 10000) / 10000.0,
                        (Boolean) m.get("teamRelated") ? 1 : 0, (Boolean) m.get("handled") ? 1 : 0);
            }

            // ---- OCR 智能预检 ----
            runOcrCheck(applicationId, fileId, fileName, holder, diskFile, declared, memberNames, compareFields);

        } catch (Exception e) {
            insertCheckRecord(applicationId, fileId, null, null, false, 0, null, "failed");
            System.err.println("证书查重失败 fileId=" + fileId + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildMatch(Integer applicationId, Integer fileId, Map<String, Object> hit,
                                           String matchType, double similarity) {
        Integer matchedAppId = ((Number) hit.get("matchedApplicationId")).intValue();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("matchedApplicationId", matchedAppId);
        m.put("matchedFileId", ((Number) hit.get("matchedFileId")).intValue());
        m.put("matchedStudentId", hit.get("matchedStudentId"));
        m.put("matchType", matchType);
        m.put("similarity", similarity);
        m.put("teamRelated", isTeamRelated(applicationId, matchedAppId));
        m.put("handled", isHandled(applicationId) || isHandled(matchedAppId));
        return m;
    }

    // ==================== OCR 预检 ====================

    @SuppressWarnings("unchecked")
    private void runOcrCheck(Integer applicationId, Integer fileId, String fileName, BufferedImageHolder std,
                             File diskFile, Map<String, String> declared, List<String> memberNames,
                             List<String> compareFields) {
        // PDF 暂不支持OCR，转人工
        String ext = fileName.lastIndexOf('.') >= 0 ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!CertificateFingerprintUtil.isImage(ext)) {
            jdbc.update("INSERT INTO ocr_record (application_id, file_id, ocr_status, error_message, compare_result, create_time) " +
                    "VALUES (?, ?, 'skipped', '非图片文件，跳过OCR识别', 'undetermined', NOW())", applicationId, fileId);
            return;
        }

        // 优先使用标准化后的图片（体积小、识别快），失败时回退原图
        Map<String, Object> ocrResult;
        File tmp = null;
        try {
            if (std != null) {
                tmp = File.createTempFile("ocr_std_", ".jpg");
                ImageIO.write(std.image, "jpg", tmp);
                ocrResult = ocrService.recognize(tmp);
                if (!Boolean.TRUE.equals(ocrResult.get("success"))) {
                    ocrResult = ocrService.recognize(diskFile); // 回退原图重试
                }
            } else {
                ocrResult = ocrService.recognize(diskFile);
            }
        } catch (Exception e) {
            ocrResult = new LinkedHashMap<>();
            ocrResult.put("success", false);
            ocrResult.put("error", "OCR调用异常: " + e.getMessage());
        } finally {
            if (tmp != null) tmp.delete();
        }

        if (!Boolean.TRUE.equals(ocrResult.get("success"))) {
            // OCR 失败后自动转人工审核
            jdbc.update("INSERT INTO ocr_record (application_id, file_id, ocr_status, error_message, compare_result, create_time) " +
                            "VALUES (?, ?, 'failed', ?, 'undetermined', NOW())",
                    applicationId, fileId, ocrResult.get("error"));
            return;
        }

        List<Map<String, Object>> words = (List<Map<String, Object>>) ocrResult.get("words");
        String rawText = String.valueOf(ocrResult.get("rawText"));
        Map<String, Object> fields = ocrService.extractFields(words);

        // 置信度：所有识别词的平均概率
        double overallConf = 0;
        for (Map<String, Object> w : words) {
            Object p = w.get("probability");
            overallConf += p instanceof Number ? ((Number) p).doubleValue() : 0.90;
        }
        overallConf = words.isEmpty() ? 0 : overallConf / words.size();

        // 与申报字段比对
        Map<String, Object> compare = ocrService.compareWithDeclared(declared, fields, compareFields);
        List<Map<String, Object>> detail = (List<Map<String, Object>>) compare.get("detail");

        // 团队申请：证书上的姓名可能是任一团队成员，任一匹配即视为一致
        if (memberNames != null && !memberNames.isEmpty()) {
            for (Map<String, Object> d : detail) {
                if ("name".equals(d.get("field")) && "inconsistent".equals(d.get("result"))) {
                    String recognized = String.valueOf(d.get("recognized"));
                    if (recognized != null && memberNames.contains(recognized)) {
                        d.put("result", "consistent");
                    }
                }
            }
            boolean inconsistent = detail.stream().anyMatch(d -> "inconsistent".equals(d.get("result")));
            boolean consistent = detail.stream().anyMatch(d -> "consistent".equals(d.get("result")));
            boolean undetermined = detail.stream().anyMatch(d -> "undetermined".equals(d.get("result")));
            compare.put("result", inconsistent ? "inconsistent"
                    : (consistent && !undetermined ? "consistent" : "undetermined"));
        }

        Map<String, Double> confidence = (Map<String, Double>) fields.get("confidence");
        jdbc.update("INSERT INTO ocr_record (application_id, file_id, ocr_status, raw_text, recognized_name, " +
                        "recognized_competition, recognized_award_level, recognized_award_time, recognized_certificate_no, " +
                        "field_confidence, overall_confidence, compare_result, compare_detail, create_time) " +
                        "VALUES (?, ?, 'success', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                applicationId, fileId, rawText,
                str(fields.get("name")), str(fields.get("competition")), str(fields.get("awardLevel")),
                str(fields.get("awardTime")), str(fields.get("certificateNo")),
                ocrService.buildConfidenceJson(confidence), Math.round(overallConf * 100) / 100.0,
                compare.get("result"), ocrService.buildDetailJson(detail));
    }

    // ==================== 团队关系判断 ====================

    /**
     * 判断两个申请是否属于同一团队（或存在团队成员关系）。
     * 同一团队成员允许上传相同证书；无团队关系的相同证书标记高风险（在风险计算中处理）。
     */
    private boolean isTeamRelated(Integer appId1, Integer appId2) {
        if (appId1.equals(appId2)) return true;
        List<Map<String, Object>> apps = jdbc.queryForList(
                "SELECT application_id, student_id, team_id FROM award_application WHERE application_id IN (?, ?)",
                appId1, appId2);
        Map<Integer, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> a : apps) {
            byId.put(((Number) a.get("application_id")).intValue(), a);
        }
        Map<String, Object> a1 = byId.get(appId1);
        Map<String, Object> a2 = byId.get(appId2);
        if (a1 == null || a2 == null) return false;

        Integer team1 = a1.get("team_id") == null ? null : ((Number) a1.get("team_id")).intValue();
        Integer team2 = a2.get("team_id") == null ? null : ((Number) a2.get("team_id")).intValue();
        Integer student1 = a1.get("student_id") == null ? null : ((Number) a1.get("student_id")).intValue();
        Integer student2 = a2.get("student_id") == null ? null : ((Number) a2.get("student_id")).intValue();

        // 同一团队
        if (team1 != null && team1.equals(team2)) return true;

        Set<Integer> members1 = teamMemberIds(team1);
        Set<Integer> members2 = teamMemberIds(team2);
        members1.add(student1);
        members2.add(student2);

        // 成员有交集（含申请人互为对方团队成员）
        for (Integer m : members1) {
            if (m != null && members2.contains(m)) return true;
        }
        return false;
    }

    /** 团队全部成员学生ID集合（含队长） */
    private Set<Integer> teamMemberIds(Integer teamId) {
        Set<Integer> ids = new HashSet<>();
        if (teamId == null) return ids;
        jdbc.queryForList("SELECT student_id FROM team_member WHERE team_id = ? AND student_id IS NOT NULL", teamId)
                .forEach(r -> ids.add(((Number) r.get("student_id")).intValue()));
        jdbc.queryForList("SELECT leader_id FROM team WHERE team_id = ? AND leader_id IS NOT NULL", teamId)
                .forEach(r -> ids.add(((Number) r.get("leader_id")).intValue()));
        return ids;
    }

    /** 申请是否已被人工标记为正常复用 */
    private boolean isHandled(Integer applicationId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT manual_mark FROM application_risk WHERE application_id = ? AND manual_mark = 'normal_reuse'",
                applicationId);
        return !rows.isEmpty();
    }

    // ==================== 数据装载 ====================

    /** 申报字段：姓名 / 竞赛名称 / 获奖等级 / 获奖时间 */
    private Map<String, String> loadDeclaredFields(Integer applicationId) {
        Map<String, String> declared = new HashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT aa.award_level, aa.award_time, s.student_name, c.competition_name " +
                        "FROM award_application aa " +
                        "LEFT JOIN student s ON aa.student_id = s.student_id " +
                        "LEFT JOIN competition c ON aa.competition_id = c.competition_id " +
                        "WHERE aa.application_id = ?", applicationId);
        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            declared.put("name", str(row.get("student_name")));
            declared.put("competition", str(row.get("competition_name")));
            declared.put("awardLevel", str(row.get("award_level")));
            declared.put("awardTime", row.get("award_time") == null ? "" : String.valueOf(row.get("award_time")));
        }
        return declared;
    }

    /** 本申请团队成员姓名列表（团队申请时用于OCR姓名比对的候选） */
    private List<String> loadTeamMemberNames(Integer applicationId) {
        return jdbc.queryForList(
                "SELECT COALESCE(s.student_name, tm.external_name) AS name FROM award_application aa " +
                        "JOIN team_member tm ON aa.team_id = tm.team_id " +
                        "LEFT JOIN student s ON tm.student_id = s.student_id " +
                        "WHERE aa.application_id = ?", String.class, applicationId);
    }

    // ==================== 工具方法 ====================

    private Integer insertCheckRecord(Integer applicationId, Integer fileId, String sha256, String phash,
                                      boolean exactDuplicate, int similarCount, Double maxSimilarity, String status) {
        jdbc.update("INSERT INTO duplicate_check_record (application_id, file_id, sha256, phash, exact_duplicate, " +
                        "similar_count, max_similarity, check_status, check_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                applicationId, fileId, sha256, phash, exactDuplicate ? 1 : 0, similarCount, maxSimilarity, status);
        return jdbc.queryForObject("SELECT MAX(check_id) FROM duplicate_check_record WHERE application_id = ? AND file_id = ?",
                Integer.class, applicationId, fileId);
    }

    /** 将网络相对路径 /uploads/applications/... 解析为磁盘文件 */
    private File resolveDiskFile(String networkPath) {
        if (networkPath == null) return null;
        String prefix = "/uploads/applications/";
        if (networkPath.startsWith(prefix)) {
            return new File(uploadPath, networkPath.substring(prefix.length()));
        }
        if (networkPath.startsWith("/uploads/")) {
            return new File(new File(uploadPath).getParentFile(), networkPath.substring("/uploads/".length()));
        }
        return new File(networkPath);
    }

    /** 保存标准化图片，返回网络相对路径 */
    private String saveStandardImage(Integer applicationId, java.awt.image.BufferedImage stdImage) {
        try {
            File dir = new File(uploadPath, "certstd/" + applicationId);
            if (!dir.exists()) dir.mkdirs();
            String name = UUID.randomUUID() + ".jpg";
            File out = new File(dir, name);
            ImageIO.write(stdImage, "jpg", out);
            return "/uploads/applications/certstd/" + applicationId + "/" + name;
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** 简单图片持有器（避免重复解码） */
    private record BufferedImageHolder(java.awt.image.BufferedImage image) {
    }

    // ==================== 查询接口 ====================

    /**
     * 分页查询查重记录（含申请信息与匹配概要）
     */
    public Map<String, Object> listCheckRecords(Integer applicationId, String studentNumber,
                                                int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (applicationId != null) {
            where.append(" AND r.application_id = ? ");
            args.add(applicationId);
        }
        if (studentNumber != null && !studentNumber.isBlank()) {
            where.append(" AND s.student_number LIKE ? ");
            args.add("%" + studentNumber.trim() + "%");
        }

        String baseSql = " FROM duplicate_check_record r " +
                "LEFT JOIN award_application aa ON r.application_id = aa.application_id " +
                "LEFT JOIN student s ON aa.student_id = s.student_id " +
                "LEFT JOIN competition c ON aa.competition_id = c.competition_id " +
                "LEFT JOIN application_file af ON r.file_id = af.file_id " +
                "LEFT JOIN application_risk ar ON r.application_id = ar.application_id " + where;

        int total = jdbc.queryForObject("SELECT COUNT(*)" + baseSql, Integer.class, args.toArray());

        String listSql = "SELECT r.check_id AS checkId, r.application_id AS applicationId, r.sha256, r.phash, " +
                "r.exact_duplicate AS exactDuplicate, r.similar_count AS similarCount, r.max_similarity AS maxSimilarity, " +
                "r.check_status AS checkStatus, r.check_time AS checkTime, " +
                "aa.application_number AS applicationNumber, aa.application_status AS applicationStatus, " +
                "s.student_number AS studentNumber, s.student_name AS studentName, " +
                "c.competition_name AS competitionName, af.file_name AS fileName, af.file_path AS filePath, " +
                "ar.risk_level AS riskLevel, ar.need_manual_review AS needManualReview, ar.manual_mark AS manualMark " +
                baseSql + " ORDER BY r.check_id DESC LIMIT ? OFFSET ?";
        args.add(pageSize);
        args.add((page - 1) * pageSize);
        List<Map<String, Object>> list = jdbc.queryForList(listSql, args.toArray());

        // 附带每个记录的匹配明细（数量有限，直接批量查询）
        for (Map<String, Object> row : list) {
            Object checkId = row.get("checkId");
            if (checkId != null) {
                List<Map<String, Object>> matches = listMatchesByCheckId(((Number) checkId).intValue());
                row.put("matches", matches);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    /** 查询单条查重记录的匹配明细 */
    public List<Map<String, Object>> listMatchesByCheckId(int checkId) {
        return jdbc.queryForList(
                "SELECT m.match_id AS matchId, m.match_type AS matchType, m.similarity, m.team_related AS teamRelated, m.handled, " +
                        "m.matched_application_id AS matchedApplicationId, " +
                        "aa.application_number AS matchedApplicationNumber, aa.application_status AS matchedStatus, " +
                        "s.student_number AS matchedStudentNumber, s.student_name AS matchedStudentName, " +
                        "c.competition_name AS matchedCompetitionName, t.name AS matchedTeamName, " +
                        "af.file_name AS matchedFileName, af.file_path AS matchedFilePath " +
                        "FROM duplicate_match m " +
                        "LEFT JOIN award_application aa ON m.matched_application_id = aa.application_id " +
                        "LEFT JOIN student s ON aa.student_id = s.student_id " +
                        "LEFT JOIN competition c ON aa.competition_id = c.competition_id " +
                        "LEFT JOIN team t ON aa.team_id = t.team_id " +
                        "LEFT JOIN application_file af ON m.matched_file_id = af.file_id " +
                        "WHERE m.check_id = ? ORDER BY m.similarity DESC", checkId);
    }
}
