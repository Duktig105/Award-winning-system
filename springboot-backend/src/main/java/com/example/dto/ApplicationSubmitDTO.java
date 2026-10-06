package com.example.dto;

import java.util.List;

/**
 * 申请提交DTO
 */
public class ApplicationSubmitDTO {
    private Integer competitionId;       // 竞赛ID
    private Integer teamId;              // 已锁定的赛前组队ID（可选）
    private String projectName;          // 项目名称
    private String teamName;             // 团队名称（个人赛为空）
    private String competitionLevel;     // 竞赛级别
    private String awardLevel;           // 获奖等次（A/B/C/D）
    private String awardRank;            // 获奖等级（金奖/一等奖等）
    private String awardTime;            // 获奖时间
    private String contact;              // 联系方式
    private List<TeacherDTO> teachers;     // 指导老师列表
    private List<TeamMemberDTO> teamMembers;  // 团队成员列表
    private Boolean confirmMemberOrder;  // 确认成员排序
    private Boolean isTeam;              // 是否团体赛

    // Getter and Setter
    public Integer getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Integer competitionId) {
        this.competitionId = competitionId;
    }

    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getCompetitionLevel() {
        return competitionLevel;
    }

    public void setCompetitionLevel(String competitionLevel) {
        this.competitionLevel = competitionLevel;
    }

    public String getAwardLevel() {
        return awardLevel;
    }

    public void setAwardLevel(String awardLevel) {
        this.awardLevel = awardLevel;
    }

    public String getAwardRank() {
        return awardRank;
    }

    public void setAwardRank(String awardRank) {
        this.awardRank = awardRank;
    }

    public String getAwardTime() {
        return awardTime;
    }

    public void setAwardTime(String awardTime) {
        this.awardTime = awardTime;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<TeacherDTO> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<TeacherDTO> teachers) {
        this.teachers = teachers;
    }

    public List<TeamMemberDTO> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<TeamMemberDTO> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public Boolean getConfirmMemberOrder() {
        return confirmMemberOrder;
    }

    public void setConfirmMemberOrder(Boolean confirmMemberOrder) {
        this.confirmMemberOrder = confirmMemberOrder;
    }

    public Boolean getIsTeam() {
        return isTeam;
    }

    public void setIsTeam(Boolean isTeam) {
        this.isTeam = isTeam;
    }

    /**
     * 指导老师DTO
     */
    public static class TeacherDTO {
        private String teacherName;
        private String teacherDepartment;
        private String teacherNo;

        public String getTeacherName() {
            return teacherName;
        }

        public void setTeacherName(String teacherName) {
            this.teacherName = teacherName;
        }

        public String getTeacherDepartment() {
            return teacherDepartment;
        }

        public void setTeacherDepartment(String teacherDepartment) {
            this.teacherDepartment = teacherDepartment;
        }

        public String getTeacherNo() {
            return teacherNo;
        }

        public void setTeacherNo(String teacherNo) {
            this.teacherNo = teacherNo;
        }
    }

    /**
     * 团队成员DTO
     */
    public static class TeamMemberDTO {
        private Boolean isExternal;
        private String studentName;
        private String studentDepartment;
        private String studentNumber;
        private String externalName;
        private String externalSchool;
        private String externalNumber;
        private Boolean isLeader;
        private Integer sortOrder;

        public Boolean getIsExternal() {
            return isExternal;
        }

        public void setIsExternal(Boolean isExternal) {
            this.isExternal = isExternal;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getStudentDepartment() {
            return studentDepartment;
        }

        public void setStudentDepartment(String studentDepartment) {
            this.studentDepartment = studentDepartment;
        }

        public String getStudentNumber() {
            return studentNumber;
        }

        public void setStudentNumber(String studentNumber) {
            this.studentNumber = studentNumber;
        }

        public String getExternalName() {
            return externalName;
        }

        public void setExternalName(String externalName) {
            this.externalName = externalName;
        }

        public String getExternalSchool() {
            return externalSchool;
        }

        public void setExternalSchool(String externalSchool) {
            this.externalSchool = externalSchool;
        }

        public String getExternalNumber() {
            return externalNumber;
        }

        public void setExternalNumber(String externalNumber) {
            this.externalNumber = externalNumber;
        }

        public Boolean getIsLeader() {
            return isLeader;
        }

        public void setIsLeader(Boolean isLeader) {
            this.isLeader = isLeader;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
