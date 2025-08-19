package com.qltiku2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 答案提交请求DTO
 * 
 * @author qltiku2
 */
public class AnswerSubmitRequest {
    
    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Long questionId;
    
    /**
     * 用户答案
     */
    @NotBlank(message = "用户答案不能为空")
    private String userAnswer;
    
    /**
     * 答题时间（秒）
     */
    private Integer answerTime;
    
    public AnswerSubmitRequest() {}
    
    public AnswerSubmitRequest(Long questionId, String userAnswer) {
        this.questionId = questionId;
        this.userAnswer = userAnswer;
    }
    
    public AnswerSubmitRequest(Long questionId, String userAnswer, Integer answerTime) {
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.answerTime = answerTime;
    }
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public String getUserAnswer() {
        return userAnswer;
    }
    
    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }
    
    public Integer getAnswerTime() {
        return answerTime;
    }
    
    public void setAnswerTime(Integer answerTime) {
        this.answerTime = answerTime;
    }
    
    @Override
    public String toString() {
        return "AnswerSubmitRequest{" +
                "questionId=" + questionId +
                ", userAnswer='" + userAnswer + '\'' +
                ", answerTime=" + answerTime +
                '}';
    }
}