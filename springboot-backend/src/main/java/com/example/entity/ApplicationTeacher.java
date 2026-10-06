package com.example.entity;

/**
 * 申请指导老师关联实体类
 * 对应数据库表：application_teacher
 */
public class ApplicationTeacher {
    private Integer id;                  // ID（主键）
    private String teacherName;          // 指导老师姓名
    private String teacherDepartment;    // 指导老师所在学院
    private String teacherNo;            // 指导老师工号
    private Integer applicationId;       // 申请ID

    // Getter and Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }
}
