package com.example.controller;

import com.example.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    /**
     * 改动1：新增日志记录器。
     * 原代码 catch(Exception) 后只把 e.getMessage() 返回给前端，不落任何日志，
     * 导致上次 application_risk 缺表这类故障在 backend.log 中完全没有堆栈，只能靠手工复现排查。
     */
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    /**
     * 改动2：合法状态集合前置到 Controller 层（与 ReviewService.isValidStatus 保持一致），
     * 非法状态直接返回 400，而不是进入 Service 抛 RuntimeException 后以 500 兜底。
     */
    private static final Set<String> VALID_STATUSES = Set.of("pending", "approved", "returned", "rejected");

    /**
     * 改动3：分页上限，防止调用方传超大 pageSize（如 100000）一次性拖回全表数据。
     */
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ReviewService reviewService;

    /**
     * 获取审核统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = reviewService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("查询审核统计信息失败", e);
            return errorResponse(e, "查询统计信息失败");
        }
    }

    /**
     * 分页查询申请列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getApplicationList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String competitionName,
            @RequestParam(required = false) String studentNumber,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            // 改动4：分页参数规范化。原代码 page=0/负数 时 offset 为负，
            // 生成 LIMIT -20,20 会被 MySQL 拒绝并抛 SQL 语法错误（表现为 500）。
            int safePage = (page == null || page < 1) ? 1 : page;
            int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, MAX_PAGE_SIZE);
            Map<String, Object> result = reviewService.getApplicationList(
                status, competitionName, studentNumber, safePage, safePageSize
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询审核申请列表失败, status={}, competitionName={}, studentNumber={}, page={}, pageSize={}",
                    status, competitionName, studentNumber, page, pageSize, e);
            return errorResponse(e, "查询申请列表失败");
        }
    }

    /**
     * 获取申请详情
     */
    @GetMapping("/detail/{applicationId}")
    public ResponseEntity<Map<String, Object>> getApplicationDetail(@PathVariable Integer applicationId) {
        try {
            Map<String, Object> detail = reviewService.getApplicationDetail(applicationId);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("查询申请详情失败, applicationId={}", applicationId, e);
            return errorResponse(e, "查询申请详情失败");
        }
    }

    /**
     * 单个审核申请
     */
    @PostMapping("/single")
    public ResponseEntity<Map<String, Object>> reviewApplication(@RequestBody Map<String, Object> request) {
        try {
            // 改动5：安全取值替代强制类型转换。原代码 (Integer) request.get("applicationId")
            // 在客户端传字符串 "20" 或浮点数时会抛 ClassCastException（同样是无日志的 500）。
            Integer applicationId = toInteger(request.get("applicationId"));
            String status = request.get("status") == null ? null : String.valueOf(request.get("status"));

            if (applicationId == null || status == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "参数不完整");
                return ResponseEntity.badRequest().body(error);
            }
            if (!VALID_STATUSES.contains(status)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "无效的审核状态: " + status);
                return ResponseEntity.badRequest().body(error);
            }

            reviewService.reviewApplication(applicationId, status);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "审核成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("审核申请失败, request={}", request, e);
            return errorResponse(e, "审核失败");
        }
    }

    /**
     * 批量审核申请
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchReviewApplications(@RequestBody Map<String, Object> request) {
        try {
            // 改动5（同上）：批量 ID 列表元素同样使用安全转换。
            List<Integer> applicationIds = toIntegerList(request.get("applicationIds"));
            String status = request.get("status") == null ? null : String.valueOf(request.get("status"));

            if (applicationIds == null || applicationIds.isEmpty() || status == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "参数不完整");
                return ResponseEntity.badRequest().body(error);
            }
            if (!VALID_STATUSES.contains(status)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "无效的审核状态: " + status);
                return ResponseEntity.badRequest().body(error);
            }

            reviewService.batchReviewApplications(applicationIds, status);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "批量审核成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("批量审核申请失败, request={}", request, e);
            return errorResponse(e, "批量审核失败");
        }
    }

    /**
     * 改动6：统一错误响应构造。e.getMessage() 可能为 null（如 NullPointerException），
     * 原代码会把 {"error": null} 返回给前端导致用户只看到笼统报错，这里提供兜底文案。
     * 注意：响应体结构 {"error": ...} 与 HTTP 状态码保持原样，前端无需任何改动。
     */
    private ResponseEntity<Map<String, Object>> errorResponse(Exception e, String fallbackMsg) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage() != null ? e.getMessage() : fallbackMsg);
        return ResponseEntity.status(500).body(error);
    }

    /**
     * 改动7：容错的整型转换，支持 Number 与数字字符串，转换失败返回 null（由上层按参数缺失处理）。
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 改动7（配套）：批量 ID 列表容错转换，忽略非法元素而不是抛 ClassCastException。
     */
    private List<Integer> toIntegerList(Object value) {
        if (!(value instanceof List<?>)) {
            return null;
        }
        List<Integer> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            Integer id = toInteger(item);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }
}
