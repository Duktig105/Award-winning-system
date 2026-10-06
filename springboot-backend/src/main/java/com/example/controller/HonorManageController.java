package com.example.controller;

import com.example.common.Result;
import com.example.service.HonorSystemService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 荣誉系统管理端Controller
 * 提供给管理员/导师管理类别、技能、积分规则、勋章、荣誉标签
 */
@RestController
@RequestMapping("/api/honor-manage")
public class HonorManageController {

    @Resource
    HonorSystemService honorSystemService;

    // 类别
    @PostMapping("/category")
    public Result createCategory(@RequestBody Map<String,Object> body){
        honorSystemService.createCategory(body);
        return Result.success("新增成功");
    }

    @PutMapping("/category")
    public Result updateCategory(@RequestBody Map<String,Object> body){
        honorSystemService.updateCategory(body);
        return Result.success("更新成功");
    }

    @DeleteMapping("/category/{id}")
    public Result disableCategory(@PathVariable Integer id){
        honorSystemService.disableCategory(id);
        return Result.success("已停用");
    }

    // 技能标签
    @PostMapping("/skill")
    public Result createSkill(@RequestBody Map<String,Object> body){
        honorSystemService.createSkillTag(body);
        return Result.success("新增成功");
    }

    @PutMapping("/skill")
    public Result updateSkill(@RequestBody Map<String,Object> body){
        honorSystemService.updateSkillTag(body);
        return Result.success("更新成功");
    }

    @DeleteMapping("/skill/{id}")
    public Result disableSkill(@PathVariable Integer id){
        honorSystemService.disableSkillTag(id);
        return Result.success("已停用");
    }

    // 竞赛-技能关联
    @PostMapping("/competition-skill")
    public Result saveCompetitionSkill(@RequestBody Map<String,Object> body){
        honorSystemService.saveCompetitionSkillRelation(body);
        return Result.success("保存成功");
    }

    @DeleteMapping("/competition-skill/{id}")
    public Result deleteCompetitionSkill(@PathVariable Integer id){
        honorSystemService.deleteCompetitionSkillRelation(id);
        return Result.success("删除成功");
    }

    // 竞赛目录（类别归属/目录等次/计分覆盖/基础分）
    @GetMapping("/competition/options")
    public Result competitionOptions(@RequestParam(required = false) String keyword){
        return Result.success(honorSystemService.competitionOptions(keyword));
    }

    @PostMapping("/competition")
    public Result saveCompetitionCatalog(@RequestBody Map<String,Object> body){
        honorSystemService.saveCompetitionCatalog(body);
        return Result.success("保存成功");
    }

    // 获奖认定（角色快照 + 历史性突破特殊系数）
    @GetMapping("/application/list")
    public Result listApplicationsForMarking(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword){
        return Result.success(honorSystemService.listApplicationsForMarking(status, keyword));
    }

    @PostMapping("/application/mark")
    public Result markApplication(@RequestBody Map<String,Object> body){
        honorSystemService.markApplication(body);
        return Result.success("认定成功");
    }

    // 积分规则
    @PostMapping("/score-rule")
    public Result saveScoreRule(@RequestBody Map<String,Object> body){
        honorSystemService.saveHonorScoreRule(body);
        return Result.success("保存成功");
    }

    @PostMapping("/score-rule/{ruleId}/toggle")
    public Result toggleScoreRule(@PathVariable Integer ruleId, @RequestParam Boolean enable){
        honorSystemService.toggleHonorScoreRule(ruleId, enable);
        return Result.success(enable ? "已启用" : "已停用");
    }

    @PostMapping("/score/recalc")
    public Result recalcAllScores(){
        return Result.success(honorSystemService.recalcAllScores());
    }

    // 勋章（固定条件类型 + 参数配置）
    @PostMapping("/badge")
    public Result saveBadge(@RequestBody Map<String,Object> body){
        honorSystemService.saveBadge(body);
        return Result.success("保存成功");
    }

    @PostMapping("/badge/{badgeId}/toggle")
    public Result toggleBadge(@PathVariable Integer badgeId, @RequestParam String status){
        honorSystemService.toggleBadge(badgeId, status);
        return Result.success("enabled".equals(status) ? "已启用" : "已停用");
    }

    @PostMapping("/badge/recheck/{studentId}")
    public Result recheckBadgesForStudent(@PathVariable Integer studentId){
        honorSystemService.evaluateBadges(studentId);
        return Result.success("已重算勋章");
    }

    // 荣誉标签
    @PostMapping("/tag")
    public Result saveTag(@RequestBody Map<String,Object> body){
        honorSystemService.saveHonorTag(body);
        return Result.success("保存成功");
    }

    @PostMapping("/tag/{tagId}/toggle")
    public Result toggleTag(@PathVariable Integer tagId, @RequestParam String status){
        honorSystemService.toggleHonorTag(tagId, status);
        return Result.success("已更新");
    }
}