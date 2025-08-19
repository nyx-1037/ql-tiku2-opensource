package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 错题本实体类
 * 
 * @author qltiku2
 */
@TableName("wrong_question")
public class WrongQuestion {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long questionId;
    
    /**
     * 错题类型：PRACTICE-练习题目，EXAM_CUSTOM-模拟考试自定义试卷，EXAM_FIXED-模拟考试固定试卷
     */
    private String wrongType;
    
    /**
     * 错误次数
     */
    private Integer wrongCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastWrongTime;
    
    /**
     * 是否已掌握：0-未掌握，1-已掌握
     */
    private Integer isMastered;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime masterTime;
    
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 非数据库字段
    @TableField(exist = false)
    private String questionTitle;
    
    @TableField(exist = false)
    private String questionContent;
    
    @TableField(exist = false)
    private String options;
    
    @TableField(exist = false)
    private String correctAnswer;
    
    @TableField(exist = false)
    private String analysis;
    
    @TableField(exist = false)
    private Integer questionType;
    
    @TableField(exist = false)
    private Integer difficulty;
    
    @TableField(exist = false)
    private String subjectName;
    
    @TableField(exist = false)
    private String knowledgePoints;
    
    // 构造方法
    public WrongQuestion() {}
    
    public WrongQuestion(Long userId, Long questionId) {
        this.userId = userId;
        this.questionId = questionId;
        this.wrongCount = 1;
        this.isMastered = 0;
        this.lastWrongTime = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public String getWrongType() {
        return wrongType;
    }
    
    public void setWrongType(String wrongType) {
        this.wrongType = wrongType;
    }
    
    public Integer getWrongCount() {
        return wrongCount;
    }
    
    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }
    
    public LocalDateTime getLastWrongTime() {
        return lastWrongTime;
    }
    
    public void setLastWrongTime(LocalDateTime lastWrongTime) {
        this.lastWrongTime = lastWrongTime;
    }
    
    public Integer getIsMastered() {
        return isMastered;
    }
    
    public void setIsMastered(Integer isMastered) {
        this.isMastered = isMastered;
    }
    
    public LocalDateTime getMasterTime() {
        return masterTime;
    }
    
    public void setMasterTime(LocalDateTime masterTime) {
        this.masterTime = masterTime;
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
    
    public String getQuestionTitle() {
        return questionTitle;
    }
    
    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }
    
    public String getQuestionContent() {
        return questionContent;
    }
    
    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
    
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
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
    
    public String getKnowledgePoints() {
        return knowledgePoints;
    }
    
    public void setKnowledgePoints(String knowledgePoints) {
        this.knowledgePoints = knowledgePoints;
    }
    
    @Override
    public String toString() {
        return "WrongQuestion{" +
                "id=" + id +
                ", userId=" + userId +
                ", questionId=" + questionId +
                ", wrongCount=" + wrongCount +
                ", lastWrongTime=" + lastWrongTime +
                ", isMastered=" + isMastered +
                ", createTime=" + createTime +
                '}';
    }
}