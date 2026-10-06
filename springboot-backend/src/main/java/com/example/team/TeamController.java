package com.example.team;

import com.example.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TeamController {
    private final TeamService service;
    public TeamController(TeamService service,TeamRecommendationService recommendations) { this.service = service; this.recommendations=recommendations; }

    @GetMapping("/api/team-profiles/me") public Result myProfile(){return Result.success(service.getMyProfile());}
    @PutMapping("/api/team-profiles/me") public Result saveProfile(@RequestBody Map<String,Object> body){return Result.success(service.saveProfile(body));}
    @GetMapping("/api/team-profiles/{studentId}") public Result profile(@PathVariable int studentId){return Result.success(service.getProfile(studentId));}

    @GetMapping("/api/teams") public Result teams(@RequestParam Map<String,String> filters,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int pageSize){return Result.success(service.marketplace(filters,page,pageSize));}
    @GetMapping("/api/teams/mine") public Result mine(){return Result.success(service.mine());}
    @GetMapping("/api/teams/locked") public Result locked(){return Result.success(service.lockedTeams());}
    @GetMapping("/api/teams/recommendations") public Result recommendations(){return Result.success(recommendations.teamsForMe());}
    @GetMapping("/api/teams/{teamId}/candidates") public Result candidates(@PathVariable int teamId){return Result.success(recommendations.candidates(teamId));}
    @GetMapping("/api/teams/{teamId}") public Result detail(@PathVariable int teamId){return Result.success(service.detail(teamId));}
    @PostMapping("/api/teams") public Result create(@RequestBody Map<String,Object> body){return Result.success(Map.of("teamId",service.create(body)));}
    @PutMapping("/api/teams/{teamId}") public Result update(@PathVariable int teamId,@RequestBody Map<String,Object> body){service.update(teamId,body);return Result.success();}
    @PostMapping("/api/teams/{teamId}/positions") public Result position(@PathVariable int teamId,@RequestBody Map<String,Object> body){return Result.success(Map.of("positionId",service.addPosition(teamId,body)));}
    @PutMapping("/api/teams/{teamId}/positions/{positionId}") public Result updatePosition(@PathVariable int teamId,@PathVariable int positionId,@RequestBody Map<String,Object>body){service.updatePosition(teamId,positionId,body);return Result.success();}
    @PostMapping("/api/teams/{teamId}/positions/{positionId}/close") public Result closePosition(@PathVariable int teamId,@PathVariable int positionId){service.closePosition(teamId,positionId);return Result.success();}
    @DeleteMapping("/api/teams/{teamId}/positions/{positionId}") public Result deletePosition(@PathVariable int teamId,@PathVariable int positionId){service.deletePosition(teamId,positionId);return Result.success();}
    @PostMapping("/api/teams/{teamId}/apply") public Result apply(@PathVariable int teamId,@RequestBody Map<String,Object> body){return Result.success(Map.of("requestId",service.apply(teamId,body)));}
    @PostMapping("/api/teams/{teamId}/invite") public Result invite(@PathVariable int teamId,@RequestBody Map<String,Object> body){return Result.success(Map.of("requestId",service.invite(teamId,body)));}
    @DeleteMapping("/api/teams/{teamId}/members/{studentId}") public Result remove(@PathVariable int teamId,@PathVariable int studentId){service.removeMember(teamId,studentId);return Result.success();}
    @PostMapping("/api/teams/{teamId}/leave") public Result leave(@PathVariable int teamId){service.leave(teamId);return Result.success();}
    @PutMapping("/api/teams/{teamId}/members/{studentId}/role") public Result memberRole(@PathVariable int teamId,@PathVariable int studentId,@RequestBody Map<String,Object>body){service.changeMemberRole(teamId,studentId,body);return Result.success();}
    @PostMapping("/api/teams/{teamId}/transfer") public Result transfer(@PathVariable int teamId,@RequestBody Map<String,Object> body){service.transfer(teamId,((Number)body.get("studentId")).intValue());return Result.success();}
    @PostMapping("/api/teams/{teamId}/recruiting") public Result recruiting(@PathVariable int teamId,@RequestBody Map<String,Object> body){service.setRecruiting(teamId,Boolean.TRUE.equals(body.get("recruiting")));return Result.success();}
    @PostMapping("/api/teams/{teamId}/lock") public Result lock(@PathVariable int teamId){service.lock(teamId);return Result.success();}
    @PostMapping("/api/teams/{teamId}/dissolve") public Result dissolve(@PathVariable int teamId){service.dissolve(teamId);return Result.success();}
    @PostMapping("/api/teams/{teamId}/reports") public Result report(@PathVariable int teamId,@RequestBody Map<String,Object> body){service.report(teamId,String.valueOf(body.getOrDefault("reason","")));return Result.success();}

    @GetMapping("/api/team-requests") public Result requests(@RequestParam(defaultValue="mine") String scope){return Result.success(service.requests(scope));}
    @PostMapping("/api/team-requests/{requestId}/{action}") public Result requestAction(@PathVariable int requestId,@PathVariable String action){service.decideRequest(requestId,action);return Result.success();}

    private final TeamRecommendationService recommendations;
}
