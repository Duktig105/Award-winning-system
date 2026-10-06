package com.example.entity;

/**
 * 团队成员实体类
 * 对应数据库表：team_member
 */
public class TeamMember {
    private Integer teamMemberId;      // 团队成员ID（主键）
    private Integer teamId;            // 团队ID
    private Integer studentId;         // 学生ID（本校学生）
    private Integer isLeader;          // 是否为队长（1=是，0=否）
    private Integer sortOrder;         // 排序
    private String externalName;       // 外校成员姓名
    private String externalNumber;     // 外校成员学号
    private String externalSchool;     // 外校成员学校

    // Getter and Setter
    public Integer getTeamMemberId() {
        return teamMemberId;
    }

    public void setTeamMemberId(Integer teamMemberId) {
        this.teamMemberId = teamMemberId;
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

    public Integer getIsLeader() {
        return isLeader;
    }

    public void setIsLeader(Integer isLeader) {
        this.isLeader = isLeader;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    public String getExternalNumber() {
        return externalNumber;
    }

    public void setExternalNumber(String externalNumber) {
        this.externalNumber = externalNumber;
    }

    public String getExternalSchool() {
        return externalSchool;
    }

    public void setExternalSchool(String externalSchool) {
        this.externalSchool = externalSchool;
    }
}
