package com.example.entity;

import java.time.LocalDateTime;

/**
 * 学生积分汇总
 */
public class StudentHonorScoreVO {
    private Integer studentId;
    private Integer totalScore;
    private Integer levelScore;
    private LocalDateTime lastCalcTime;
    private LocalDateTime updateTime;

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public Integer getLevelScore() { return levelScore; }
    public void setLevelScore(Integer levelScore) { this.levelScore = levelScore; }
    public LocalDateTime getLastCalcTime() { return lastCalcTime; }
    public void setLastCalcTime(LocalDateTime lastCalcTime) { this.lastCalcTime = lastCalcTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}