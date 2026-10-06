package com.example.entity;

import java.time.LocalDateTime;

/**
 * 积分明细
 */
public class HonorScoreLogVO {
    private Long logId;
    private Integer studentId;
    private Integer applicationId;
    private Integer score;
    private String scoreType;
    private String description;
    private String ruleSnapshot;
    private LocalDateTime createTime;
    // 展示用
    private String competitionName;
    private String awardLevel;
    private String awardRank;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleSnapshot() { return ruleSnapshot; }
    public void setRuleSnapshot(String ruleSnapshot) { this.ruleSnapshot = ruleSnapshot; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getCompetitionName() { return competitionName; }
    public void setCompetitionName(String competitionName) { this.competitionName = competitionName; }
    public String getAwardLevel() { return awardLevel; }
    public void setAwardLevel(String awardLevel) { this.awardLevel = awardLevel; }
    public String getAwardRank() { return awardRank; }
    public void setAwardRank(String awardRank) { this.awardRank = awardRank; }
}