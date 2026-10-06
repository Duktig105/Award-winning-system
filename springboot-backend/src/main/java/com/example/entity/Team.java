package com.example.entity;

import java.time.LocalDateTime;

/**
 * 团队实体类
 * 对应数据库表：team
 */
public class Team {
    private Integer teamId;              // 团队ID（主键）
    private String name;                 // 团队名称
    private Integer leaderId;            // 队长学生ID
    private Integer competitionId;       // 关联竞赛ID
    private Integer hasSort;             // 是否有排序（1=是，0=否）
    private LocalDateTime createTime;    // 创建时间

    // Getter and Setter
    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Integer leaderId) {
        this.leaderId = leaderId;
    }

    public Integer getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Integer competitionId) {
        this.competitionId = competitionId;
    }

    public Integer getHasSort() {
        return hasSort;
    }

    public void setHasSort(Integer hasSort) {
        this.hasSort = hasSort;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
