package com.qltiku2.vo;

/**
 * 答案提交响应VO
 * 
 * @author qltiku2
 */
public class AnswerSubmitResponse {
    
    /**
     * 是否正确
     */
    private Boolean correct;
    
    /**
     * 正确答案
     */
    private String correctAnswer;
    
    /**
     * 用户答案
     */
    private String userAnswer;
    
    /**
     * 解析说明
     */
    private String analysis;
    
    public AnswerSubmitResponse() {}
    
    public AnswerSubmitResponse(Boolean correct, String correctAnswer, String userAnswer, String analysis) {
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.userAnswer = userAnswer;
        this.analysis = analysis;
    }
    
    public Boolean getCorrect() {
        return correct;
    }
    
    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getUserAnswer() {
        return userAnswer;
    }
    
    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }
    
    public String getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
    
    @Override
    public String toString() {
        return "AnswerSubmitResponse{" +
                "correct=" + correct +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", userAnswer='" + userAnswer + '\'' +
                ", analysis='" + analysis + '\'' +
                '}';
    }
}