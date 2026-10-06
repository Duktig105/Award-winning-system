package com.example.controller;

import com.example.common.Result;
import com.example.dto.ApplicationSubmitDTO;
import com.example.entity.Competition;
import com.example.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import com.example.auth.AuthContext;
import com.example.auth.AuthUser;

/**
 * 申请管理控制器
 * 统一处理申请和竞赛相关的所有HTTP请求
 * 包括竞赛查询、申请提交、申请查询等功能
 */
@RestController
public class ApplicationController {

    @Resource
    ApplicationService applicationService;

    @Resource
    com.example.certificate.CertificateCheckService certificateCheckService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 竞赛相关接口 ====================

    /**
     * 搜索竞赛接口
     * @param keyword 搜索关键字（可选）
     * @return Result 统一响应结果，包含符合条件的竞赛列表
     */
    @GetMapping("/competition/search")
    public Result searchCompetitions(@RequestParam(required = false) String keyword) {
        try {
            List<Competition> competitions = applicationService.searchCompetitions(keyword);
            return Result.success(competitions);
        } catch (Exception e) {
            System.err.println("搜索竞赛失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("搜索竞赛失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有竞赛接口
     * @return Result 统一响应结果，包含所有竞赛列表
     */
    @GetMapping("/competition/all")
    public Result getAllCompetitions() {
        try {
            List<Competition> competitions = applicationService.getAllCompetitions();
            return Result.success(competitions);
        } catch (Exception e) {
            System.err.println("获取竞赛列表失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取竞赛列表失败: " + e.getMessage());
        }
    }

    // ==================== 申请相关接口 ====================

    /**
     * 提交申请接口
     * @param data 申请数据（JSON字符串）
     * @param files 证明材料文件（可选）
     * @param studentId 学生ID，从前端传递
     * @return Result 统一响应结果，成功时返回申请ID
     */
    @PostMapping("/application/submit")
    public Result submitApplication(
            @RequestParam("data") String data,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "studentId", required = false) Integer ignoredStudentId) {
        
        try {
            System.out.println("========== 收到申请提交请求 ==========");
            Integer studentId = AuthContext.require().studentId();
            System.out.println("学生ID: " + studentId);
            System.out.println("数据: " + data);
            System.out.println("文件数量: " + (files != null ? files.length : 0));
            
            // 解析申请数据
            ApplicationSubmitDTO dto = objectMapper.readValue(data, ApplicationSubmitDTO.class);
            
            // 验证数据
            if (dto.getCompetitionId() == null) {
                return Result.error("请选择竞赛");
            }
            
            if (dto.getProjectName() == null || dto.getProjectName().isEmpty()) {
                return Result.error("项目名称不能为空");
            }
            
            if (studentId == null) {
                return Result.error("学生信息不存在，请重新登录");
            }
            
            // 调用Service保存申请
            Integer applicationId = applicationService.submitApplication(dto, files, studentId);

            // 异步触发证书预检（查重 + OCR + 风险计算），不阻塞申请提交
            try {
                certificateCheckService.checkApplicationAsync(applicationId);
            } catch (Exception checkEx) {
                System.err.println("触发证书预检失败（不影响申请提交）: " + checkEx.getMessage());
            }

            System.out.println("申请提交成功，申请ID: " + applicationId);
            System.out.println("========================================");
            
            return Result.success("申请提交成功，等待审核");
            
        } catch (Exception e) {
            System.err.println("申请提交失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("申请提交失败: " + e.getMessage());
        }
    }

    /**
     * 根据学生ID查询申请列表
     * @param studentId 学生ID
     * @return Result 统一响应结果，包含该学生的所有申请
     */
    @GetMapping("/application/list")
    public Result getApplicationsByStudentId(@RequestParam Integer studentId) {
        try {
            requireApplicationOwnerOrReviewer(studentId);
            return Result.success(applicationService.getApplicationsByStudentId(studentId));
        } catch (Exception e) {
            System.err.println("查询申请列表失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询申请列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据申请ID查询申请详情
     * @param applicationId 申请ID
     * @return Result 统一响应结果，包含申请详细信息
     */
    @GetMapping("/application/detail")
    public Result getApplicationDetail(@RequestParam Integer applicationId) {
        try {
            Map<String, Object> detail = applicationService.getApplicationDetail(applicationId);
            requireApplicationOwnerOrReviewer((Integer) detail.get("studentId"));
            return Result.success(detail);
        } catch (Exception e) {
            System.err.println("查询申请详情失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询申请详情失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有申请（管理员/导师使用）
     * @return Result 统一响应结果，包含所有申请列表
     */
    @GetMapping("/application/all")
    public Result getAllApplications() {
        try {
            return Result.success(applicationService.getAllApplications());
        } catch (Exception e) {
            System.err.println("查询所有申请失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询所有申请失败: " + e.getMessage());
        }
    }

    /**
     * 更新申请状态（管理员/导师使用）
     * @param applicationId 申请ID
     * @param status 新状态
     * @return Result 统一响应结果
     */
    @PostMapping("/application/updateStatus")
    public Result updateApplicationStatus(
            @RequestParam Integer applicationId,
            @RequestParam String status) {
        try {
            applicationService.updateApplicationStatus(applicationId, status);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            System.err.println("更新申请状态失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("更新申请状态失败: " + e.getMessage());
        }
    }

    // ==================== 申请记录相关接口 ====================

    /**
     * 获取当前学生的申请记录列表（包含竞赛名称）
     * @param studentId 学生ID
     * @return Result 统一响应结果，包含申请记录列表
     */
    @GetMapping("/application/record/list")
    public Result getApplicationRecordList(@RequestParam Integer studentId) {
        try {
            requireApplicationOwnerOrReviewer(studentId);
            List<java.util.Map<String, Object>> applications = applicationService.getApplicationsByStudentId(studentId);
            return Result.success(applications);
        } catch (Exception e) {
            return Result.error("查询申请记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取申请详情（用于申请记录查看）
     * @param applicationId 申请ID
     * @return Result 统一响应结果，包含申请详细信息
     */
    @GetMapping("/application/record/detail/{applicationId}")
    public Result getApplicationRecordDetail(@PathVariable Integer applicationId) {
        try {
            Map<String, Object> detail = applicationService.getApplicationDetail(applicationId);
            requireApplicationOwnerOrReviewer((Integer) detail.get("studentId"));
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error("查询申请详情失败: " + e.getMessage());
        }
    }

    /**
     * 撤回申请（仅待审核状态可撤回）
     * @param request 包含applicationId和studentId的请求体
     * @return Result 统一响应结果
     */
    @PostMapping("/application/record/withdraw")
    public Result withdrawApplication(@RequestBody java.util.Map<String, Integer> request) {
        try {
            Integer applicationId = request.get("applicationId");
            Integer studentId = AuthContext.require().studentId();
            
            if (applicationId == null || studentId == null) {
                return Result.error("参数不完整");
            }
            
            applicationService.withdrawApplication(applicationId, studentId);
            return Result.success("申请已撤回");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private void requireApplicationOwnerOrReviewer(Integer applicationStudentId) {
        AuthUser user = AuthContext.require();
        if (user.hasRole("admin") || user.hasRole("mentor")) return;
        if (applicationStudentId == null || !applicationStudentId.equals(user.studentId())) {
            throw new SecurityException("无权访问其他学生的申请");
        }
    }
}
