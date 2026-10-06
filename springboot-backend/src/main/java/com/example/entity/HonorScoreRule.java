package com.example.entity;

import java.time.LocalDateTime;

/**
 * 荣誉积分规则
 */
public class HonorScoreRule {
    private Integer ruleId;
    private String competitionLevel;
    private String awardRank;
    private Integer baseScore;
    private Double awardRatio;
    private Boolean isTeam;
    private Double teamRatio;
    private Boolean enabled;
    private LocalDateTime enableTime;
    private LocalDateTime updateTime;

    public Integer getRuleId() { return ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }
    public String getCompetitionLevel() { return competitionLevel; }
    public void setCompetitionLevel(String competitionLevel) { this.competitionLevel = competitionLevel; }
    public String getAwardRank() { return awardRank; }
    public void setAwardRank(String awardRank) { this.awardRank = awardRank; }
    public Integer getBaseScore() { return baseScore; }
    public void setBaseScore(Integer baseScore) { this.baseScore = baseScore; }
    public Double getAwardRatio() { return awardRatio; }
    public void setAwardRatio(Double awardRatio) { this.awardRatio = awardRatio; }
    public Boolean getIsTeam() { return isTeam; }
    public void setIsTeam(Boolean isTeam) { this.isTeam = isTeam; }
    public Double getTeamRatio() { return teamRatio; }
    public void setTeamRatio(Double teamRatio) { this.teamRatio = teamRatio; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getEnableTime() { return enableTime; }
    public void setEnableTime(LocalDateTime enableTime) { this.enableTime = enableTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}