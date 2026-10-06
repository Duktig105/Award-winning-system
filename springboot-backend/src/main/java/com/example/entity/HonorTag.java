package com.example.entity;

import java.time.LocalDateTime;

/**
 * 荣誉标签
 */
public class HonorTag {
    private Integer tagId;
    private String tagName;
    private String tagType;
    private String description;
    private String conditionRule;
    private String icon;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createTime;

    public Integer getTagId() { return tagId; }
    public void setTagId(Integer tagId) { this.tagId = tagId; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getConditionRule() { return conditionRule; }
    public void setConditionRule(String conditionRule) { this.conditionRule = conditionRule; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}