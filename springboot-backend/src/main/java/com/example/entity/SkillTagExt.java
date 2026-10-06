package com.example.entity;

/**
 * 技能标签扩展实体（包含新字段）
 */
public class SkillTagExt {
    private Integer skillId;
    private String name;
    private Integer skillCategoryId;
    private String categoryName;
    private String description;
    private String level;
    private String icon;
    private Boolean allowSelfEval;
    private Boolean awardOnly;
    private Integer sortOrder;
    private String status;

    public Integer getSkillId() { return skillId; }
    public void setSkillId(Integer skillId) { this.skillId = skillId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSkillCategoryId() { return skillCategoryId; }
    public void setSkillCategoryId(Integer skillCategoryId) { this.skillCategoryId = skillCategoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Boolean getAllowSelfEval() { return allowSelfEval; }
    public void setAllowSelfEval(Boolean allowSelfEval) { this.allowSelfEval = allowSelfEval; }
    public Boolean getAwardOnly() { return awardOnly; }
    public void setAwardOnly(Boolean awardOnly) { this.awardOnly = awardOnly; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}