package com.example.entity;

import java.time.LocalDateTime;

/**
 * 学生勋章视图对象
 */
public class StudentBadgeVO {
    private Integer studentId;
    private Integer badgeId;
    private String badgeCode;
    private String badgeName;
    private String description;
    private String icon;
    private Integer unlockThreshold;
    private Integer sortOrder;
    private String status;
    private Integer progress;
    private LocalDateTime unlockedAt;
    private Boolean isRepresentative;
    private Boolean unlocked;
    /** 拥有该勋章的学生数量（用于分享） */
    private Integer ownerCount;

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public Integer getBadgeId() { return badgeId; }
    public void setBadgeId(Integer badgeId) { this.badgeId = badgeId; }
    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }
    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) { this.badgeName = badgeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getUnlockThreshold() { return unlockThreshold; }
    public void setUnlockThreshold(Integer unlockThreshold) { this.unlockThreshold = unlockThreshold; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
    public Boolean getIsRepresentative() { return isRepresentative; }
    public void setIsRepresentative(Boolean isRepresentative) { this.isRepresentative = isRepresentative; }
    public Boolean getUnlocked() { return unlocked; }
    public void setUnlocked(Boolean unlocked) { this.unlocked = unlocked; }
    public Integer getOwnerCount() { return ownerCount; }
    public void setOwnerCount(Integer ownerCount) { this.ownerCount = ownerCount; }
}