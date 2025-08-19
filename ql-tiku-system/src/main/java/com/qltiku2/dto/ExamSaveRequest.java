package com.qltiku2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * 考试保存请求DTO
 * 
 * @author qltiku2
 */
public class ExamSaveRequest {
    
    /**
     * 考试ID（更新时需要）
     */
    private Long id;
    
    /**
     * 考试标题
     */
    @NotBlank(message = "考试标题不能为空")
    private String title;
    
    /**
     * 考试描述
     */
    private String description;
    
    /**
     * 科目ID
     */
    @NotNull(message = "科目ID不能为空")
    private Long subjectId;
    
    /**
     * 考试时长（分钟）
     */
    @NotNull(message = "考试时长不能为空")
    @Positive(message = "考试时长必须大于0")
    private Integer duration;
    
    /**
     * 总分
     */
    @NotNull(message = "总分不能为空")
    @Positive(message = "总分必须大于0")
    private Integer totalScore;
    
    /**
     * 及格分数
     */
    @NotNull(message = "及格分数不能为空")
    @Positive(message = "及格分数必须大于0")
    private Integer passScore;
    
    /**
     * 考试状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 考试类型：0-随机题目，1-自选题目
     */
    private Integer examType = 0;
    
    /**
     * 题目数量（随机模式使用）
     */
    private Integer questionCount;
    
    /**
     * 选中的题目ID列表（自选模式使用）
     */
    private java.util.List<Long> selectedQuestionIds;
    
    /**
     * 题目配置（随机模式使用）
     */
    private java.util.List<QuestionConfig> questionConfigs;
    
    /**
     * 题目配置内部类
     */
    public static class QuestionConfig {
        /**
         * 题目类型：0-单选题，1-多选题，2-判断题，3-简答题
         */
        private Integer questionType;
        
        /**
         * 难度：1-简单，2-中等，3-困难
         */
        private Integer difficulty;
        
        /**
         * 题目数量
         */
        private Integer count;
        
        /**
         * 每题分值
         */
        private Integer score;
        
        public QuestionConfig() {}
        
        public Integer getQuestionType() {
            return questionType;
        }
        
        public void setQuestionType(Integer questionType) {
            this.questionType = questionType;
        }
        
        public Integer getDifficulty() {
            return difficulty;
        }
        
        public void setDifficulty(Integer difficulty) {
            this.difficulty = difficulty;
        }
        
        public Integer getCount() {
            return count;
        }
        
        public void setCount(Integer count) {
            this.count = count;
        }
        
        public Integer getScore() {
            return score;
        }
        
        public void setScore(Integer score) {
            this.score = score;
        }
    }
    
    public ExamSaveRequest() {}
    
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
    
    public Integer getStatus() {
        return status;
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
    
    public Integer getExamType() {
        return examType;
    }
    
    public void setExamType(Integer examType) {
        this.examType = examType;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public java.util.List<Long> getSelectedQuestionIds() {
        return selectedQuestionIds;
    }
    
    public void setSelectedQuestionIds(java.util.List<Long> selectedQuestionIds) {
        this.selectedQuestionIds = selectedQuestionIds;
    }
    
    public java.util.List<QuestionConfig> getQuestionConfigs() {
        return questionConfigs;
    }
    
    public void setQuestionConfigs(java.util.List<QuestionConfig> questionConfigs) {
        this.questionConfigs = questionConfigs;
    }
    
    @Override
    public String toString() {
        return "ExamSaveRequest{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", subjectId=" + subjectId +
                ", duration=" + duration +
                ", totalScore=" + totalScore +
                ", passScore=" + passScore +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", examType=" + examType +
                ", questionCount=" + questionCount +
                ", selectedQuestionIds=" + selectedQuestionIds +
                ", questionConfigs=" + questionConfigs +
                '}';
    }
}