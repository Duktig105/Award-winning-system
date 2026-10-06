package com.example.certificate;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 智能预检服务
 * 接入现成 OCR 服务（默认百度智能云"通用文字识别（高精度）"），支持配置切换。
 * 识别证书上的：姓名 / 竞赛名称 / 获奖等级 / 获奖时间 / 证书编号，
 * 并与学生申报字段比对，生成 一致 / 不一致 / 无法判断 三种结果。
 * OCR 服务未配置或调用失败时自动转为人工审核。
 */
@Service
public class OcrService {

    private static final String BAIDU_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ocr.provider:none}")
    private String provider;

    /** OCR 接口地址：默认标准版（免费额度内零成本、超量单价最低） */
    @Value("${ocr.baidu.endpoint:https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic}")
    private String baiduOcrUrl;

    @Value("${ocr.baidu.api-key:}")
    private String baiduApiKey;

    @Value("${ocr.baidu.secret-key:}")
    private String baiduSecretKey;

    /** 百度 access_token 缓存 */
    private volatile String cachedToken;
    private volatile long tokenExpireAt = 0;

    // ==================== OCR 服务调用 ====================

    /** 是否已配置可用的 OCR 服务 */
    public boolean isConfigured() {
        if ("none".equalsIgnoreCase(provider) || provider == null) return false;
        if ("baidu".equalsIgnoreCase(provider)) {
            return baiduApiKey != null && !baiduApiKey.isBlank()
                    && baiduSecretKey != null && !baiduSecretKey.isBlank();
        }
        return false;
    }

    public String providerName() {
        return provider == null ? "none" : provider;
    }

    /**
     * 调用 OCR 服务识别图片
     * 返回: {success, error, rawText, words:[{text, probability}]}
     */
    public Map<String, Object> recognize(File imageFile) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (!isConfigured()) {
                result.put("success", false);
                result.put("error", "OCR服务未配置，已自动转人工审核");
                return result;
            }
            byte[] bytes = Files.readAllBytes(imageFile.toPath());
            String token = getBaiduToken();
            if (token == null) {
                result.put("success", false);
                result.put("error", "OCR服务鉴权失败，已自动转人工审核");
                return result;
            }
            String body = "image=" + java.net.URLEncoder.encode(Base64.getEncoder().encodeToString(bytes), "UTF-8");
            try (HttpResponse resp = HttpRequest.post(baiduOcrUrl + "?access_token=" + token)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .timeout(30000)
                    .execute()) {
                JsonNode json = mapper.readTree(resp.body());
                if (json.has("error_code")) {
                    result.put("success", false);
                    result.put("error", "OCR服务返回错误: " + json.path("error_msg").asText());
                    return result;
                }
                List<Map<String, Object>> words = new ArrayList<>();
                StringBuilder raw = new StringBuilder();
                for (JsonNode item : json.path("words_result")) {
                    String text = item.path("words").asText("");
                    double prob = item.path("probability").path("average").asDouble(0.90);
                    Map<String, Object> w = new LinkedHashMap<>();
                    w.put("text", text);
                    w.put("probability", prob);
                    words.add(w);
                    raw.append(text).append('\n');
                }
                result.put("success", true);
                result.put("words", words);
                result.put("rawText", raw.toString());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "OCR调用失败: " + e.getMessage());
        }
        return result;
    }

    private synchronized String getBaiduToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpireAt) {
            return cachedToken;
        }
        try {
            String url = BAIDU_TOKEN_URL + "?grant_type=client_credentials"
                    + "&client_id=" + java.net.URLEncoder.encode(baiduApiKey, "UTF-8")
                    + "&client_secret=" + java.net.URLEncoder.encode(baiduSecretKey, "UTF-8");
            try (HttpResponse resp = HttpRequest.post(url).timeout(15000).execute()) {
                JsonNode json = mapper.readTree(resp.body());
                if (json.has("access_token")) {
                    cachedToken = json.get("access_token").asText();
                    long expiresIn = json.path("expires_in").asLong(2592000L);
                    // 提前 1 小时过期，避免边界失败
                    tokenExpireAt = now + (expiresIn - 3600) * 1000;
                    return cachedToken;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ==================== 字段抽取 ====================

    private static final Pattern NAME_PATTERN =
            Pattern.compile("(?:姓名|获奖人|获奖者|获证人|证书获得者|选手)[:：\\s]*([\\u4e00-\\u9fa5·]{2,15})");
    private static final Pattern AWARD_PATTERN =
            Pattern.compile("(特等奖|一等奖|二等奖|三等奖|金奖|银奖|铜奖|冠军|亚军|季军|优秀奖|优胜奖|单项奖|特等|一等|二等|三等)");
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{4})\\s*[年./\\-]\\s*(\\d{1,2})\\s*[月./\\-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})\\s*年");
    private static final Pattern CERT_NO_PATTERN =
            Pattern.compile("(?:证书编号|证书号码|证书号|编号|Certificate\\s*No|Cert\\s*No|No)[.．:：#\\s]*([A-Za-z0-9\\-]{4,40})");
    private static final Pattern COMPETITION_KEYWORD =
            Pattern.compile("竞赛|大赛|挑战赛|锦标赛|邀请赛|杯赛|选拔赛|设计赛|创新创业|Contest|Competition|Challenge", Pattern.CASE_INSENSITIVE);

    /**
     * 从 OCR 识别文本中抽取结构化字段
     * 返回: {name, competition, awardLevel, awardTime, certificateNo} + 各字段 confidence
     */
    public Map<String, Object> extractFields(List<Map<String, Object>> words) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Map<String, Double> confidence = new LinkedHashMap<>();
        if (words == null || words.isEmpty()) {
            return fields;
        }

        // 姓名标签匹配（标签在同行或下一行）
        for (int i = 0; i < words.size(); i++) {
            String line = clean(words.get(i).get("text"));
            Matcher m = NAME_PATTERN.matcher(line);
            if (m.find()) {
                fields.put("name", m.group(1));
                confidence.put("name", prob(words.get(i)));
                break;
            }
        }

        // 获奖等级
        for (Map<String, Object> w : words) {
            String line = clean(w.get("text"));
            if (line.contains("等级") || line.contains("奖项") || AWARD_PATTERN.matcher(line).find()) {
                Matcher m = AWARD_PATTERN.matcher(line);
                if (m.find()) {
                    String award = m.group(1);
                    if (!award.endsWith("奖") && !award.endsWith("军")) {
                        award = award + "奖";
                    }
                    fields.put("awardLevel", award);
                    confidence.put("awardLevel", prob(w));
                    break;
                }
            }
        }

        // 获奖时间（优先完整日期，其次年份）
        for (Map<String, Object> w : words) {
            String line = clean(w.get("text"));
            Matcher m = DATE_PATTERN.matcher(line);
            if (m.find()) {
                fields.put("awardTime", m.group(1) + "-" + pad(m.group(2)) + "-" + pad(m.group(3)));
                confidence.put("awardTime", prob(w));
                break;
            }
        }
        if (!fields.containsKey("awardTime")) {
            for (Map<String, Object> w : words) {
                Matcher m = YEAR_PATTERN.matcher(clean(w.get("text")));
                if (m.find()) {
                    fields.put("awardTime", m.group(1));
                    confidence.put("awardTime", prob(w));
                    break;
                }
            }
        }

        // 证书编号
        for (Map<String, Object> w : words) {
            String line = clean(w.get("text"));
            Matcher m = CERT_NO_PATTERN.matcher(line);
            if (m.find()) {
                fields.put("certificateNo", m.group(1).toUpperCase());
                confidence.put("certificateNo", prob(w));
                break;
            }
        }

        // 竞赛名称：取包含竞赛关键词的最长一行，并剥离"竞赛名称："等前缀
        String best = null;
        double bestProb = 0;
        for (Map<String, Object> w : words) {
            String line = clean(w.get("text"));
            if (line.length() >= 4 && COMPETITION_KEYWORD.matcher(line).find()) {
                line = line.replaceFirst("^(?:竞赛名称|比赛名称|赛事名称|竞赛|大赛)[:：\\s]*", "");
                if (best == null || line.length() > best.length()) {
                    best = line;
                    bestProb = prob(w);
                }
            }
        }
        if (best != null && best.length() >= 4) {
            fields.put("competition", best);
            confidence.put("competition", bestProb);
        }

        fields.put("confidence", confidence);
        return fields;
    }

    // ==================== 与申报字段比对 ====================

    /**
     * 将识别字段与申报字段比对
     *
     * @param declared      申报字段: {name, competition, awardLevel, awardTime}
     * @param recognized    识别字段（extractFields 结果）
     * @param compareFields 允许参与比对的字段列表（来自风险规则配置）
     * @return {result: consistent/inconsistent/undetermined, detail: [{field, declared, recognized, result}]}
     */
    public Map<String, Object> compareWithDeclared(Map<String, String> declared, Map<String, Object> recognized, List<String> compareFields) {
        List<Map<String, Object>> detail = new ArrayList<>();
        boolean hasInconsistent = false;
        boolean hasUndetermined = false;
        boolean hasConsistent = false;

        String[] fieldKeys = {"name", "competition", "awardLevel", "awardTime"};
        String[] fieldLabels = {"姓名", "竞赛名称", "获奖等级", "获奖时间"};
        for (int i = 0; i < fieldKeys.length; i++) {
            String key = fieldKeys[i];
            String label = fieldLabels[i];
            if (compareFields != null && !compareFields.contains(key)) {
                continue; // 该字段未启用比对
            }
            String declaredVal = declared.get(key);
            String recognizedVal = (String) recognized.get(key);
            String result;
            if (declaredVal == null || declaredVal.isBlank()) {
                result = "undetermined"; // 申报侧无该字段
            } else if (recognizedVal == null || recognizedVal.isBlank()) {
                result = "undetermined"; // 未识别出该字段
            } else {
                result = compareValue(key, declaredVal, recognizedVal);
            }
            if ("inconsistent".equals(result)) hasInconsistent = true;
            else if ("undetermined".equals(result)) hasUndetermined = true;
            else hasConsistent = true;

            Map<String, Object> d = new LinkedHashMap<>();
            d.put("field", key);
            d.put("label", label);
            d.put("declared", declaredVal);
            d.put("recognized", recognizedVal);
            d.put("result", result);
            detail.add(d);
        }

        String overall;
        if (hasInconsistent) overall = "inconsistent";
        else if (hasConsistent) overall = hasUndetermined ? "undetermined" : "consistent";
        else overall = "undetermined";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", overall);
        result.put("detail", detail);
        return result;
    }

    /** 单字段比对，附同义词归一 */
    private String compareValue(String key, String declared, String recognized) {
        String a = normalize(declared);
        String b = normalize(recognized);
        if (a.isEmpty() || b.isEmpty()) return "undetermined";
        switch (key) {
            case "name":
                return a.equals(b) ? "consistent" : "inconsistent";
            case "awardLevel": {
                String na = normalizeAwardLevel(a);
                String nb = normalizeAwardLevel(b);
                return na.equals(nb) ? "consistent" : "inconsistent";
            }
            case "awardTime":
                return compareTime(declared, recognized);
            case "competition":
                // 竞赛名称允许包含关系（OCR可能只识别出部分名称）
                if (a.equals(b) || (a.length() >= 4 && b.contains(a)) || (b.length() >= 4 && a.contains(b))) {
                    return "consistent";
                }
                return "inconsistent";
            default:
                return a.equals(b) ? "consistent" : "inconsistent";
        }
    }

    private String compareTime(String declared, String recognized) {
        try {
            LocalDate d1 = LocalDate.parse(normalizeDateStr(declared), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate d2 = LocalDate.parse(normalizeDateStr(recognized), DateTimeFormatter.ISO_LOCAL_DATE);
            return d1.equals(d2) ? "consistent" : "inconsistent";
        } catch (Exception ignored) {
        }
        // 精度不足时按年/年月比较
        if (recognized.matches("\\d{4}") && declared.contains(recognized)) return "consistent";
        if (recognized.matches("\\d{4}-\\d{2}") && normalizeDateStr(declared).startsWith(recognized)) return "consistent";
        return "undetermined";
    }

    private String normalizeDateStr(String s) {
        String t = s.trim().replace('/', '-').replace('.', '-').replace('年', '-').replace('月', '-').replaceAll("日$", "");
        String[] parts = t.split("-");
        if (parts.length == 3) {
            return parts[0] + "-" + pad(parts[1]) + "-" + pad(parts[2]);
        }
        return t;
    }

    private String normalizeAwardLevel(String s) {
        String t = s.replace("奖项", "").replace("等级", "");
        t = t.replace("特等", "特等奖").replace("一等", "一等奖").replace("二等", "二等奖").replace("三等", "三等奖");
        return t;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "").replace("（", "(").replace("）", ")").toLowerCase();
    }

    private String clean(Object text) {
        if (text == null) return "";
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    private double prob(Map<String, Object> word) {
        Object p = word.get("probability");
        return p instanceof Number ? ((Number) p).doubleValue() : 0.90;
    }

    private String pad(String s) {
        if (s.length() >= 2) return s;
        return "0" + s;
    }

    /** 构建置信度 JSON（保存到 ocr_record.field_confidence） */
    public String buildConfidenceJson(Map<String, Double> confidence) {
        try {
            ObjectNode node = mapper.createObjectNode();
            confidence.forEach((k, v) -> node.put(k, Math.round(v * 100.0) / 100.0));
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    /** 构建比对明细 JSON */
    public String buildDetailJson(List<Map<String, Object>> detail) {
        try {
            ArrayNode arr = mapper.createArrayNode();
            for (Map<String, Object> d : detail) {
                ObjectNode n = mapper.createObjectNode();
                d.forEach((k, v) -> {
                    if (v instanceof String s) n.put(k, s);
                    else if (v != null) n.put(k, String.valueOf(v));
                });
                arr.add(n);
            }
            return mapper.writeValueAsString(arr);
        } catch (Exception e) {
            return null;
        }
    }
}
