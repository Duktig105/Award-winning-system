package com.example.entity;

/**
 * 导师实体类
 * 对应数据库表：mentor
 */
public class Mentor {
    private Integer mentorId;        // 导师ID（主键）
    private String mentorName;       // 导师姓名
    private String department;       // 所在学院
    private String mentorNo;         // 导师工号（唯一）

    // Getter and Setter
    public Integer getMentorId() {
        return mentorId;
    }

    public void setMentorId(Integer mentorId) {
        this.mentorId = mentorId;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getMentorNo() {
        return mentorNo;
    }

    public void setMentorNo(String mentorNo) {
        this.mentorNo = mentorNo;
    }
}
