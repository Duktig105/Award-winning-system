package com.example.entity;

/**
 * 技能方向实体
 */
public class SkillCategory {
    private Integer skillCategoryId;
    private String categoryName;
    private Integer parentId;
    private String description;
    private Integer sortOrder;
    private String status;

    public Integer getSkillCategoryId() { return skillCategoryId; }
    public void setSkillCategoryId(Integer skillCategoryId) { this.skillCategoryId = skillCategoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}