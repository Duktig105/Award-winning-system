package com.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：user
 */
public class User {
    private Integer userId;              // 用户ID（主键）
    private String username;             // 登录账号（唯一）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;             // 登录凭据，不参与响应序列化
    private String role;                 // 用户角色（student, mentor, admin）
    private Integer studentId;           // 关联学生表ID
    private Integer mentorId;            // 关联导师表ID
    private LocalDateTime createTime;    // 创建时间
    private String status;               // 账号状态（enabled, disabled）
    private String name;                 // 真实姓名（不存储在数据库，登录时动态查询）

    // Getter and Setter
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getMentorId() {
        return mentorId;
    }

    public void setMentorId(Integer mentorId) {
        this.mentorId = mentorId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
