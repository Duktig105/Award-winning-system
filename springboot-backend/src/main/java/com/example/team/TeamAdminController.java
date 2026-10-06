package com.example.team;

import com.example.common.Result;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/teams")
public class TeamAdminController {
    private final TeamService service;
    private final TeamRecommendationService recommendations;
    public TeamAdminController(TeamService service,TeamRecommendationService recommendations){this.service=service;this.recommendations=recommendations;}
    @GetMapping public Result list(@RequestParam(required=false)String keyword,@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return Result.success(service.adminTeams(keyword,status,page,pageSize));}
    @PostMapping("/{teamId}/{action}") public Result action(@PathVariable int teamId,@PathVariable String action,@RequestBody(required=false)Map<String,Object>body){service.adminAction(teamId,action,body==null?null:String.valueOf(body.getOrDefault("reason","")));return Result.success();}
    @GetMapping("/reports") public Result reports(){return Result.success(service.adminReports());}
    @PostMapping("/reports/{reportId}") public Result resolve(@PathVariable int reportId,@RequestBody Map<String,Object>body){service.resolveReport(reportId,String.valueOf(body.get("status")),String.valueOf(body.getOrDefault("resolution","")));return Result.success();}
    @GetMapping("/{teamId}/audit") public Result audit(@PathVariable int teamId){return Result.success(service.auditLogs(teamId));}
    @GetMapping("/recommendation-weights") public Result weights(){return Result.success(recommendations.getWeights());}
    @PutMapping("/recommendation-weights") public Result updateWeights(@RequestBody Map<String,Object>body){return Result.success(recommendations.updateWeights(body));}
    @GetMapping("/recommendation-weights/history") public Result weightHistory(){return Result.success(recommendations.weightHistory());}
}
