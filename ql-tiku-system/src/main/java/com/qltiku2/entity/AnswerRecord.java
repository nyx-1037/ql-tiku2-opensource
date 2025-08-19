package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 答题记录实体类
 * 
 * @author qltiku2
 */
@TableName("answer_record")
public class AnswerRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long questionId;
    
    private Long examId;
    
    private String userAnswer;
    
    /**
     * 是否正确：0-错误，1-正确
     */
    private Integer isCorrect;
    
    private Integer score;
    
    /**
     * 答题用时（秒）
     */
    private Integer answerTime;
    
    /**
     * 练习类型：1-日常练习，2-模拟考试，3-错题重做
     */
    private Integer practiceType;
    
    /**
     * 备注信息（用于存储模拟考试ID等）
     */
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    // 非数据库字段
    @TableField(exist = false)
    private String questionTitle;
    
    @TableField(exist = false)
    private String correctAnswer;
    
    @TableField(exist = false)
    private String subjectName;
    
    @TableField(exist = false)
    private Integer questionType;
    
    // 构造方法
    public AnswerRecord() {}
    
    public AnswerRecord(Long userId, Long questionId, String userAnswer, 
                       Integer isCorrect, Integer score, Integer practiceType) {
        this.userId = userId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
        this.score = score;
        this.practiceType = practiceType;
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
    
    public Long getExamId() {
        return examId;
    }
    
    public void setExamId(Long examId) {
        this.examId = examId;
    }
    
    public String getUserAnswer() {
        return userAnswer;
    }
    
    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }
    
    public Integer getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Integer isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public Integer getAnswerTime() {
        return answerTime;
    }
    
    public void setAnswerTime(Integer answerTime) {
        this.answerTime = answerTime;
    }
    
    public Integer getPracticeType() {
        return practiceType;
    }
    
    public void setPracticeType(Integer practiceType) {
        this.practiceType = practiceType;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public String getQuestionTitle() {
        return questionTitle;
    }
    
    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
    
    public Integer getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(Integer questionType) {
        this.questionType = questionType;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    @Override
    public String toString() {
        return "AnswerRecord{" +
                "id=" + id +
                ", userId=" + userId +
                ", questionId=" + questionId +
                ", examId=" + examId +
                ", userAnswer='" + userAnswer + '\'' +
                ", isCorrect=" + isCorrect +
                ", score=" + score +
                ", practiceType=" + practiceType +
                ", createTime=" + createTime +
                '}';
    }
}