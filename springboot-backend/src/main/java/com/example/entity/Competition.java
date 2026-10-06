package com.example.entity;

/**
 * 竞赛实体类
 * 对应数据库表：competition
 */
public class Competition {
    private Integer competitionId;     // 竞赛ID（主键）
    private String competitionName;    // 竞赛名称
    private String awardRank;          // 获奖等次（A/B/C/D）
    private String competitionType;    // 竞赛类型（个人赛/团体赛）
    private Integer minTeamSize;
    private Integer maxTeamSize;
    private java.time.LocalDateTime teamOpenTime;
    private java.time.LocalDateTime teamCloseTime;
    private Integer teamEnabled;

    // Getter and Setter
    public Integer getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Integer competitionId) {
        this.competitionId = competitionId;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    public void setCompetitionName(String competitionName) {
        this.competitionName = competitionName;
    }

    public String getAwardRank() {
        return awardRank;
    }

    public void setAwardRank(String awardRank) {
        this.awardRank = awardRank;
    }

    public String getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(String competitionType) {
        this.competitionType = competitionType;
    }

    public Integer getMinTeamSize() { return minTeamSize; }
    public void setMinTeamSize(Integer minTeamSize) { this.minTeamSize = minTeamSize; }
    public Integer getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(Integer maxTeamSize) { this.maxTeamSize = maxTeamSize; }
    public java.time.LocalDateTime getTeamOpenTime() { return teamOpenTime; }
    public void setTeamOpenTime(java.time.LocalDateTime teamOpenTime) { this.teamOpenTime = teamOpenTime; }
    public java.time.LocalDateTime getTeamCloseTime() { return teamCloseTime; }
    public void setTeamCloseTime(java.time.LocalDateTime teamCloseTime) { this.teamCloseTime = teamCloseTime; }
    public Integer getTeamEnabled() { return teamEnabled; }
    public void setTeamEnabled(Integer teamEnabled) { this.teamEnabled = teamEnabled; }
}
