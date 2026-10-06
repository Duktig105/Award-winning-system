package com.example.controller;

import com.example.auth.AuthContext;
import com.example.common.Result;
import com.example.service.HonorSystemService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 荣誉系统Controller
 * 提供给学生端使用的基础API，以及管理员/部分技能共用的API
 */
@RestController
public class HonorController {
    @Resource
    HonorSystemService honorSystemService;

    // ============== 学生端 ==============

    /** 学生自评技能 */
    @PostMapping("/api/honor/student/skill/self")
    public Result addSelfSkill(@RequestBody Map<String,Object> body){
        honorSystemService.addSelfEvalSkill(body);
        return Result.success("已添加自评技能");
    }

    /** 学生更新自评技能 */
    @PutMapping("/api/honor/student/skill/self")
    public Result updateSelfSkill(@RequestBody Map<String,Object> body){
        honorSystemService.updateSelfEvalSkill(body);
        return Result.success("已更新");
    }

    /** 学生移除自评技能 */
    @DeleteMapping("/api/honor/student/skill/{skillId}")
    public Result removeSelfSkill(@PathVariable Integer skillId){
        honorSystemService.removeStudentSkill(skillId);
        return Result.success("已移除");
    }

    /** 学生技能档案 */
    @GetMapping("/api/honor/student/skill/list")
    public Result studentSkills(){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentSkills(sid));
    }

    /** 学生技能证据（技能详情 + 对应获奖记录） */
    @GetMapping("/api/honor/student/skill/{skillId}/evidence")
    public Result skillEvidence(@PathVariable Integer skillId){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.skillEvidence(sid, skillId));
    }

    /** 学生积分总览 */
    @GetMapping("/api/honor/student/score/summary")
    public Result studentHonorSummary(){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentHonorSummary(sid));
    }

    /** 学生积分明细 */
    @GetMapping("/api/honor/student/score/logs")
    public Result studentScoreLogs(@RequestParam(defaultValue = "200") Integer limit){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentHonorLogs(sid, limit));
    }

    /** 学生勋章列表 */
    @GetMapping("/api/honor/student/badges")
    public Result studentBadges(){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentBadges(sid));
    }

    /** 学生设置代表勋章 */
    @PostMapping("/api/honor/student/badge/representative")
    public Result setRepresentativeBadge(@RequestBody Map<String,Object> body){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        Integer bid = (Integer) body.get("badgeId");
        if (bid == null) return Result.error("badgeId不能为空");
        honorSystemService.setRepresentativeBadge(sid, bid);
        return Result.success("已更新代表勋章");
    }

    /** 手动重算我的勋章 */
    @PostMapping("/api/honor/student/badge/recheck")
    public Result recheckMyBadges(){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        honorSystemService.evaluateBadges(sid);
        return Result.success("已重算");
    }

    /** 学生荣誉标签 */
    @GetMapping("/api/honor/student/tags")
    public Result studentHonorTags(){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentHonorTags(sid));
    }

    /** 学生技能树 */
    @GetMapping("/api/honor/student/skill-tree")
    public Result studentSkillTree(){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentSkillTree(sid));
    }

    /** 学生手动点亮无门槛技能节点（阈值<=0的节点） */
    @PostMapping("/api/honor/student/skill-tree/unlock")
    public Result manualUnlockNode(@RequestBody Map<String,Object> body){
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        Integer nodeId = null;
        try {
            nodeId = Integer.parseInt(String.valueOf(body.get("nodeId")));
        } catch (Exception ignored) {
        }
        if (nodeId == null) return Result.error("节点ID不能为空");
        honorSystemService.manualUnlockNode(sid, nodeId);
        return Result.success("已点亮");
    }

    // ============== 通用查询（学生端与管理端共用） ==============

    /** 获取已启用的竞赛类别 */
    @GetMapping("/api/honor/category/list")
    public Result listCategories(@RequestParam(required = false) String status){
        return Result.success(honorSystemService.listCategories(status == null ? "enabled" : status));
    }

    /** 全部竞赛类别（管理员用） */
    @GetMapping("/api/honor/category/all")
    public Result listAllCategories(){
        return Result.success(honorSystemService.listCategories(null));
    }

    /** 技能方向列表 */
    @GetMapping("/api/honor/skill-category/list")
    public Result listSkillCategories(){
        return Result.success(honorSystemService.listSkillCategories());
    }

    /** 技能标签列表 */
    @GetMapping("/api/honor/skill/list")
    public Result listSkills(@RequestParam(required = false) Integer categoryId,
                             @RequestParam(required = false) String status){
        return Result.success(honorSystemService.listSkillTags(categoryId, status == null ? "enabled" : status));
    }

    /** 全部技能（管理端） */
    @GetMapping("/api/honor/skill/all")
    public Result listAllSkills(){
        return Result.success(honorSystemService.listSkillTags(null, null));
    }

    /** 竞赛-技能关联（mapType=default 类别默认 / override 竞赛级覆盖 / 空=全部） */
    @GetMapping("/api/honor/competition-skill/list")
    public Result listCompetitionSkillRelations(@RequestParam(required = false) Integer categoryId,
                                                @RequestParam(required = false) String mapType){
        return Result.success(honorSystemService.listCompetitionSkillRelations(categoryId, mapType));
    }

    /** 积分规则 */
    @GetMapping("/api/honor/score/rules")
    public Result listHonorScoreRules(){
        return Result.success(honorSystemService.listHonorScoreRules());
    }

    /** 勋章配置列表（管理员返回勋章配置表，学生返回自己的勋章墙） */
    @GetMapping("/api/honor/badge/list")
    public Result listBadges(){
        if (AuthContext.require().hasRole("admin")) {
            return Result.success(honorSystemService.allBadgeConfigs());
        }
        Integer sid = AuthContext.require().studentId();
        if (sid == null) return Result.error("请用学生账号登录");
        return Result.success(honorSystemService.studentBadges(sid));
    }

    /** 荣誉标签配置列表 */
    @GetMapping("/api/honor/tag/list")
    public Result listHonorTags(){
        return Result.success(honorSystemService.allHonorTags());
    }

    /** 技能等级查询 */
    @GetMapping("/api/honor/skill-level/{studentId}")
    public Result skillLevel(@PathVariable Integer studentId){
        return Result.success(honorSystemService.queryStudentSkillLevels(studentId));
    }
}