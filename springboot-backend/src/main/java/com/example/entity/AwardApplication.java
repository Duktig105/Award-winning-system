package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 获奖申请实体类
 * 对应数据库表：award_application
 */
public class AwardApplication {
    private Integer applicationId;           // 申请ID（主键）
    private Integer competitionId;           // 竞赛ID
    private Integer teamId;                  // 团队ID（个人赛为空）
    private Integer studentId;               // 申请学生ID
    private String projectName;              // 项目名称
    private Integer awardQuantity;           // 获奖数量
    private Integer awardPersonCount;        // 获奖人数
    private String contact;                  // 联系方式
    private String competitionLevel;         // 竞赛级别（国家级、省级等）
    private String awardRank;                // 获奖等次（A、B等）
    private String awardLevel;               // 获奖等级（一等奖、二等奖等）
    private LocalDate awardTime;             // 获奖时间
    private String applicationStatus;        // 申请状态（pending, approved, rejected, returned）
    private LocalDateTime createTime;        // 创建时间
    private String teamName;                 // 团队名称
    private String applicationNumber;        // 申请编号

    // Getter and Setter
    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Integer competitionId) {
        this.competitionId = competitionId;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getAwardQuantity() {
        return awardQuantity;
    }

    public void setAwardQuantity(Integer awardQuantity) {
        this.awardQuantity = awardQuantity;
    }

    public Integer getAwardPersonCount() {
        return awardPersonCount;
    }

    public void setAwardPersonCount(Integer awardPersonCount) {
        this.awardPersonCount = awardPersonCount;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getCompetitionLevel() {
        return competitionLevel;
    }

    public void setCompetitionLevel(String competitionLevel) {
        this.competitionLevel = competitionLevel;
    }

    public String getAwardRank() {
        return awardRank;
    }

    public void setAwardRank(String awardRank) {
        this.awardRank = awardRank;
    }

    public String getAwardLevel() {
        return awardLevel;
    }

    public void setAwardLevel(String awardLevel) {
        this.awardLevel = awardLevel;
    }

    public LocalDate getAwardTime() {
        return awardTime;
    }

    public void setAwardTime(LocalDate awardTime) {
        this.awardTime = awardTime;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }
}
