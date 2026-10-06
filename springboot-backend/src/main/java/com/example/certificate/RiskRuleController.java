package com.example.certificate;

import com.example.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 风险规则配置控制器（仅管理员可访问 /api/admin/**）
 * 对应管理页面：pHash阈值配置、OCR比对字段配置、风险规则启用停用、风险等级配置
 */
@RestController
@RequestMapping("/api/admin/risk")
public class RiskRuleController {

    private final RiskRuleService riskRuleService;
    private final OcrService ocrService;

    public RiskRuleController(RiskRuleService riskRuleService, OcrService ocrService) {
        this.riskRuleService = riskRuleService;
        this.ocrService = ocrService;
    }

    /** 风险规则列表（含OCR服务配置状态） */
    @GetMapping("/rules")
    public Result listRules() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("rules", riskRuleService.listRules());
        data.put("ocrConfigured", ocrService.isConfigured());
        data.put("ocrProvider", ocrService.providerName());
        return Result.success(data);
    }

    /** 更新风险规则配置 */
    @PutMapping("/rules")
    @SuppressWarnings("unchecked")
    public Result updateRules(@RequestBody Map<String, Object> body) {
        Object rules = body.get("rules");
        if (!(rules instanceof List<?> list)) {
            return Result.error("rules不能为空");
        }
        riskRuleService.updateRules((List<Map<String, Object>>) list);
        return Result.success("风险规则配置已保存", riskRuleService.listRules());
    }
}
