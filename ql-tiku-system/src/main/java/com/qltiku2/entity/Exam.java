package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 考试实体类
 * 
 * @author qltiku2
 */
@TableName("exam")
public class Exam {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String description;
    
    private Long subjectId;
    
    /**
     * 考试时长（分钟）
     */
    private Integer duration;
    
    /**
     * 总分
     */
    private Integer totalScore;
    
    /**
     * 及格分数
     */
    private Integer passScore;
    
    /**
     * 题目数量
     */
    private Integer questionCount;
    
    /**
     * 考试类型：0-固定试卷，1-模拟考试
     */
    private Integer examType;
    
    /**
     * 考试状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    /**
     * 删除标志：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
    
    public Exam() {}
    
    public Exam(String title, String description, Long subjectId, Integer duration, 
                Integer totalScore, Integer passScore, LocalDateTime startTime, 
                LocalDateTime endTime, Long createBy) {
        this.title = title;
        this.description = description;
        this.subjectId = subjectId;
        this.duration = duration;
        this.totalScore = totalScore;
        this.passScore = passScore;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createBy = createBy;
        this.status = 0; // 默认未开始
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getSubjectId() {
        return subjectId;
    }
    
    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Integer getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
    
    public Integer getPassScore() {
        return passScore;
    }
    
    public void setPassScore(Integer passScore) {
        this.passScore = passScore;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public Integer getExamType() {
        return examType;
    }
    
    public void setExamType(Integer examType) {
        this.examType = examType;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Long getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Integer getDeleted() {
        return deleted;
    }
    
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
    
    @Override
    public String toString() {
        return "Exam{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", subjectId=" + subjectId +
                ", duration=" + duration +
                ", totalScore=" + totalScore +
                ", passScore=" + passScore +
                ", questionCount=" + questionCount +
                ", examType=" + examType +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", createBy=" + createBy +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", deleted=" + deleted +
                '}';
    }
}