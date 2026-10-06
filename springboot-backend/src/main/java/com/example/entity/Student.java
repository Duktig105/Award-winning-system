package com.example.entity;

/**
 * 学生实体类
 * 对应数据库表：student
 */
public class Student {
    private Integer studentId;        // 学生ID（主键）
    private String studentNumber;     // 学号
    private String studentName;       // 学生姓名
    private String grade;             // 年级
    private String major;             // 专业
    private String className;         // 班级
    private String college;           // 所在学院

    // Getter and Setter
    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
}
