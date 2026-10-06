package com.example.entity;

import java.time.LocalDateTime;

/**
 * 技能树节点
 */
public class SkillTreeNode {
    private Integer nodeId;
    private Integer skillCategoryId;
    private Integer parentId;
    private Integer skillId;
    private String name;
    private Integer level;
    private String description;
    private String icon;
    private Integer unlockThreshold;
    private Integer sortOrder;
    private String status;
    private LocalDateTime updateTime;
    // 运行时填充
    private Boolean unlocked;
    private Integer currentExperience;
    private Integer skillExperience;
    private Integer verifiedSkillCount;

    public Integer getNodeId() { return nodeId; }
    public void setNodeId(Integer nodeId) { this.nodeId = nodeId; }
    public Integer getSkillCategoryId() { return skillCategoryId; }
    public void setSkillCategoryId(Integer skillCategoryId) { this.skillCategoryId = skillCategoryId; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public Integer getSkillId() { return skillId; }
    public void setSkillId(Integer skillId) { this.skillId = skillId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
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
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Boolean getUnlocked() { return unlocked; }
    public void setUnlocked(Boolean unlocked) { this.unlocked = unlocked; }
    public Integer getCurrentExperience() { return currentExperience; }
    public void setCurrentExperience(Integer currentExperience) { this.currentExperience = currentExperience; }
    public Integer getSkillExperience() { return skillExperience; }
    public void setSkillExperience(Integer skillExperience) { this.skillExperience = skillExperience; }
    public Integer getVerifiedSkillCount() { return verifiedSkillCount; }
    public void setVerifiedSkillCount(Integer verifiedSkillCount) { this.verifiedSkillCount = verifiedSkillCount; }
}