package com.example.service;

import com.example.mapper.ApplicationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private HonorSystemService honorSystemService;

    /**
     * 获取审核统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计各状态的申请数量
        int totalCount = applicationMapper.countApplicationsByStatus(null);
        int pendingCount = applicationMapper.countApplicationsByStatus("pending");
        int approvedCount = applicationMapper.countApplicationsByStatus("approved");
        int rejectedCount = applicationMapper.countApplicationsByStatus("rejected");
        int returnedCount = applicationMapper.countApplicationsByStatus("returned");
        
        statistics.put("totalCount", totalCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("approvedCount", approvedCount);
        statistics.put("rejectedCount", rejectedCount);
        statistics.put("returnedCount", returnedCount);
        
        return statistics;
    }

    /**
     * 分页查询申请列表
     */
    public Map<String, Object> getApplicationList(
            String status, String competitionName, String studentNumber,
            Integer page, Integer pageSize) {
        
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        // 查询总数
        int total = applicationMapper.countApplicationsForReview(status, competitionName, studentNumber);
        
        // 查询列表
        List<Map<String, Object>> list = applicationMapper.selectApplicationsForReview(
            status, competitionName, studentNumber, offset, pageSize
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        
        return result;
    }

    /**
     * 获取申请详情
     */
    public Map<String, Object> getApplicationDetail(Integer applicationId) {
        return applicationService.getApplicationDetail(applicationId);
    }

    /**
     * 单个审核申请
     */
    @Transactional
    public void reviewApplication(Integer applicationId, String status) {
        // 验证状态值
        if (!isValidStatus(status)) {
            throw new RuntimeException("无效的审核状态");
        }

        // 更新申请状态
        // 改动8：检查更新行数。原代码对不存在的 applicationId 执行 UPDATE 影响 0 行也不报错，
        // 会向前端返回"审核成功"，造成假成功（数据并未变化）。
        int updated = applicationMapper.updateApplicationStatus(applicationId, status);
        if (updated == 0) {
            throw new RuntimeException("申请不存在或已被删除: " + applicationId);
        }

        // 审核通过时触发积分、技能、勋章与荣誉标签评估
        if("approved".equals(status)){
            // 改动9：静默吞异常改为记录日志，评估失败时可在日志中追溯，不影响审核主流程
            try { honorSystemService.addScoreForApplication(applicationId); }
            catch(Exception e){ log.error("申请审核通过后积分评估失败, applicationId={}", applicationId, e); }
        }
    }

    /**
     * 批量审核申请
     */
    @Transactional
    public void batchReviewApplications(List<Integer> applicationIds, String status) {
        // 验证状态值
        if (!isValidStatus(status)) {
            throw new RuntimeException("无效的审核状态");
        }

        // 批量更新状态（改动8：同样检查行数，任一 ID 不存在即抛出，@Transactional 保证整体回滚）
        for (Integer applicationId : applicationIds) {
            int updated = applicationMapper.updateApplicationStatus(applicationId, status);
            if (updated == 0) {
                throw new RuntimeException("申请不存在或已被删除: " + applicationId);
            }
        }
        // 审核通过时批量触发积分评估（改动9：记录失败日志而非静默忽略）
        if("approved".equals(status)){
            for (Integer applicationId : applicationIds) {
                try { honorSystemService.addScoreForApplication(applicationId); }
                catch(Exception e){ log.error("申请审核通过后积分评估失败, applicationId={}", applicationId, e); }
            }
        }
    }

    /**
     * 验证状态值是否有效
     */
    private boolean isValidStatus(String status) {
        return "approved".equals(status) || 
               "rejected".equals(status) || 
               "returned".equals(status) ||
               "pending".equals(status);
    }
}
