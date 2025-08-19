package com.qltiku2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 题目保存请求DTO
 * 
 * @author qltiku2
 */
public class QuestionSaveRequest {
    
    @NotNull(message = "科目ID不能为空")
    private Long subjectId;
    
    @NotNull(message = "题目类型不能为空")
    @Min(value = 0, message = "题目类型值不正确")
    @Max(value = 4, message = "题目类型值不正确")
    private Integer questionType;
    
    @NotBlank(message = "题目标题不能为空")
    private String title;
    
    @NotBlank(message = "题目内容不能为空")
    private String content;
    
    /**
     * 选项列表
     */
    private List<QuestionOption> options;
    
    @NotBlank(message = "正确答案不能为空")
    private String correctAnswer;
    
    /**
     * 题目解析
     */
    private String analysis;
    
    @NotNull(message = "难度等级不能为空")
    @Min(value = 1, message = "难度等级最小为1")
    @Max(value = 3, message = "难度等级最大为3")
    private Integer difficulty;
    
    /**
     * 知识点（多个用逗号分隔）
     */
    private String knowledgePoints;
    
    /**
     * 题目图片URL列表
     */
    private List<String> images;
    
    // 构造方法
    public QuestionSaveRequest() {}
    
    // Getter和Setter方法
    public Long getSubjectId() {
        return subjectId;
    }
    
    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }
    
    public Integer getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(Integer questionType) {
        this.questionType = questionType;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public List<QuestionOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<QuestionOption> options) {
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
    
    public Integer getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getKnowledgePoints() {
        return knowledgePoints;
    }
    
    public void setKnowledgePoints(String knowledgePoints) {
        this.knowledgePoints = knowledgePoints;
    }
    
    public List<String> getImages() {
        return images;
    }
    
    public void setImages(List<String> images) {
        this.images = images;
    }
    
    @Override
    public String toString() {
        return "QuestionSaveRequest{" +
                "subjectId=" + subjectId +
                ", questionType=" + questionType +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", difficulty=" + difficulty +
                ", knowledgePoints='" + knowledgePoints + '\'' +
                '}';
    }
    
    /**
     * 题目选项内部类
     */
    public static class QuestionOption {
        /**
         * 选项标识（A、B、C、D等）
         */
        private String key;
        
        /**
         * 选项内容
         */
        private String value;
        
        /**
         * 是否为正确答案
         */
        private Boolean isCorrect = false;
        
        // 构造方法
        public QuestionOption() {}
        
        public QuestionOption(String key, String value) {
            this.key = key;
            this.value = value;
        }
        
        public QuestionOption(String key, String value, Boolean isCorrect) {
            this.key = key;
            this.value = value;
            this.isCorrect = isCorrect;
        }
        
        // Getter和Setter方法
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public Boolean getIsCorrect() {
            return isCorrect;
        }
        
        public void setIsCorrect(Boolean isCorrect) {
            this.isCorrect = isCorrect;
        }
        
        @Override
        public String toString() {
            return "QuestionOption{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    ", isCorrect=" + isCorrect +
                    '}';
        }
    }
}