package com.example.entity;

import java.time.LocalDateTime;

/**
 * 学生技能视图对象（含等级、经验、来源等）
 */
public class StudentSkillVO {
    private Integer studentId;
    private Integer skillId;
    private String skillName;
    private Integer skillCategoryId;
    private String categoryName;
    private String level;            // 当前技能等级(beginner/intermediate/advanced)
    private Integer experience;      // 技能经验值
    private String source;           // 来源
    private Integer applicationId;   // 来源获奖ID
    private String applicationNumber;
    private Boolean verified;        // 是否已验证
    private String note;             // 备注
    private LocalDateTime updateTime;

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public Integer getSkillId() { return skillId; }
    public void setSkillId(Integer skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Integer getSkillCategoryId() { return skillCategoryId; }
    public void setSkillCategoryId(Integer skillCategoryId) { this.skillCategoryId = skillCategoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}