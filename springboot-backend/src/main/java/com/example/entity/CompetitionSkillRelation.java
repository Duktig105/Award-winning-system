package com.example.entity;

/**
 * 竞赛与技能关联
 */
public class CompetitionSkillRelation {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private Integer skillId;
    private String skillName;
    private Integer contribution;
    private String upgradeRule;
    private String teamRole;
    private Integer teamRoleWeight;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getSkillId() { return skillId; }
    public void setSkillId(Integer skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Integer getContribution() { return contribution; }
    public void setContribution(Integer contribution) { this.contribution = contribution; }
    public String getUpgradeRule() { return upgradeRule; }
    public void setUpgradeRule(String upgradeRule) { this.upgradeRule = upgradeRule; }
    public String getTeamRole() { return teamRole; }
    public void setTeamRole(String teamRole) { this.teamRole = teamRole; }
    public Integer getTeamRoleWeight() { return teamRoleWeight; }
    public void setTeamRoleWeight(Integer teamRoleWeight) { this.teamRoleWeight = teamRoleWeight; }
}