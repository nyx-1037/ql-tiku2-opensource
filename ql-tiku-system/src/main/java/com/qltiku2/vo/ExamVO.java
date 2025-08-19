
package com.qltiku2.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 考试视图对象
 * 
 * @author qltiku2
 */
public class ExamVO {
    
    /**
     * 考试ID
     */
    private Long id;
    
    /**
     * 考试标题
     */
    private String title;
    
    /**
     * 考试描述
     */
    private String description;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
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
     * 考试状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusText;
    
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
    private Long createBy;
    
    /**
     * 创建人姓名
     */
    private String createByName;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    /**
     * 参与人数
     */
    private Integer participantCount;
    
    /**
     * 平均分
     */
    private Double averageScore;
    
    /**
     * 题目数量
     */
    private Integer questionCount;
    
    /**
     * 考试类型：0-模拟试卷，1-真题试卷
     */
    private Integer examType;
    
    /**
     * 考试类型描述
     */
    private String examTypeText;
    
    public ExamVO() {}
    
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
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
        // 设置状态描述
        if (status != null) {
            switch (status) {
                case 0:
                    this.statusText = "未开始";
                    break;
                case 1:
                    this.statusText = "进行中";
                    break;
                case 2:
                    this.statusText = "已结束";
                    break;
                default:
                    this.statusText = "未知";
                    break;
            }
        }
    }
    
    public String getStatusText() {
        return statusText;
    }
    
    public void setStatusText(String statusText) {
        this.statusText = statusText;
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
    
    public String getCreateByName() {
        return createByName;
    }
    
    public void setCreateByName(String createByName) {
        this.createByName = createByName;
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
    
    public Integer getParticipantCount() {
        return participantCount;
    }
    
    public void setParticipantCount(Integer participantCount) {
        this.participantCount = participantCount;
    }
    
    public Double getAverageScore() {
        return averageScore;
    }
    
    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public Integer getExamType() {
        return examType;
    }
    
    public void setExamType(Integer examType) {
        this.examType = examType;
        // 设置考试类型描述
        if (examType != null) {
            switch (examType) {
                case 0:
                    this.examTypeText = "模拟试卷";
                    break;
                case 1:
                    this.examTypeText = "真题试卷";
                    break;
                default:
                    this.examTypeText = "未知类型";
                    break;
            }
        }
    }
    
    public String getExamTypeText() {
        return examTypeText;
    }
    
    public void setExamTypeText(String examTypeText) {
        this.examTypeText = examTypeText;
    }
    
    @Override
    public String toString() {
        return "ExamVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", subjectId=" + subjectId +
                ", subjectName='" + subjectName + '\'' +
                ", duration=" + duration +
                ", totalScore=" + totalScore +
                ", passScore=" + passScore +
                ", status=" + status +
                ", statusText='" + statusText + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", createBy=" + createBy +
                ", createByName='" + createByName + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", participantCount=" + participantCount +
                ", averageScore=" + averageScore +
                '}';
    }
}