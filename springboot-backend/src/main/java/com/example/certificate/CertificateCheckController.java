package com.example.certificate;

import com.example.auth.AuthContext;
import com.example.auth.AuthUser;
import com.example.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 证书查重 / OCR预检 / 风险信息 控制器（审核端，admin/mentor 可访问）
 */
@RestController
@RequestMapping("/api/review")
public class CertificateCheckController {

    private final CertificateCheckService checkService;
    private final RiskRuleService riskRuleService;

    public CertificateCheckController(CertificateCheckService checkService, RiskRuleService riskRuleService) {
        this.checkService = checkService;
        this.riskRuleService = riskRuleService;
    }

    /**
     * 分页查询证书查重记录（含匹配明细、关联申请和相似度）
     */
    @GetMapping("/duplicate/list")
    public Result listCheckRecords(@RequestParam(required = false) Integer applicationId,
                                   @RequestParam(required = false) String studentNumber,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(checkService.listCheckRecords(applicationId, studentNumber, page, pageSize));
    }

    /**
     * 查询单条查重记录的相似匹配明细
     */
    @GetMapping("/duplicate/{checkId}")
    public Result getCheckDetail(@PathVariable int checkId) {
        return Result.success(checkService.listMatchesByCheckId(checkId));
    }

    /**
     * 手动重新预检某个申请（查重 + OCR + 风险计算）
     */
    @PostMapping("/duplicate/recheck/{applicationId}")
    public Result recheck(@PathVariable Integer applicationId) {
        return Result.success("重新预检完成", checkService.recheckApplication(applicationId));
    }

    /**
     * 获取申请的完整风险信息（风险等级/原因、相似证书、OCR对照、关联团队、人工标记）
     */
    @GetMapping("/risk/{applicationId}")
    public Result getRiskInfo(@PathVariable Integer applicationId) {
        return Result.success(riskRuleService.getRiskInfo(applicationId));
    }

    /**
     * 人工标记：正常复用 / 异常重复 / 无法判断（保存人工处理结果，避免反复报警）
     */
    @PostMapping("/risk/mark")
    public Result mark(@RequestBody Map<String, Object> body) {
        AuthUser user = AuthContext.require();
        Integer applicationId = toInt(body.get("applicationId"));
        String markType = String.valueOf(body.get("markType"));
        String remark = body.get("remark") == null ? "" : String.valueOf(body.get("remark"));
        if (applicationId == null) {
            return Result.error("applicationId不能为空");
        }
        riskRuleService.saveManualMark(applicationId, markType, remark, user.username());
        return Result.success("标记成功", riskRuleService.getRiskInfo(applicationId));
    }

    /**
     * 保存审核人员最终意见
     */
    @PostMapping("/risk/opinion")
    public Result saveOpinion(@RequestBody Map<String, Object> body) {
        Integer applicationId = toInt(body.get("applicationId"));
        String finalOpinion = body.get("finalOpinion") == null ? "" : String.valueOf(body.get("finalOpinion"));
        if (applicationId == null) {
            return Result.error("applicationId不能为空");
        }
        riskRuleService.saveFinalOpinion(applicationId, finalOpinion);
        return Result.success("意见已保存");
    }

    /**
     * 批量获取申请的风险等级（用于列表展示）
     */
    @PostMapping("/risk/batch")
    public Result batchRisk(@RequestBody Map<String, Object> body) {
        Object ids = body.get("applicationIds");
        if (!(ids instanceof List<?> list)) {
            return Result.error("applicationIds不能为空");
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object id : list) {
            Integer applicationId = toInt(id);
            if (applicationId != null) {
                result.add(riskRuleService.getRiskInfo(applicationId).get("risk") instanceof Map<?, ?> risk
                        ? (Map<String, Object>) risk : Map.of());
            }
        }
        return Result.success(result);
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
