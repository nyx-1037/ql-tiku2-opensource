package com.qltiku2.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qltiku2.dto.QuestionSaveRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图对象VO
 * 
 * @author qltiku2
 */
public class QuestionVO {
    
    private Long id;
    
    private Long subjectId;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
    /**
     * 题目类型：0-单选题，1-多选题，2-判断题，3-简答题
     */
    private Integer questionType;
    
    /**
     * 题目类型名称
     */
    private String questionTypeName;
    
    private String title;
    
    private String content;
    
    /**
     * 选项（JSON格式）
     */
    private String options;
    
    /**
     * 选项列表
     */
    private List<QuestionSaveRequest.QuestionOption> optionList;
    
    /**
     * 题目图片URL数组（JSON格式）
     */
    private String images;
    
    /**
     * 图片URL列表
     */
    private List<String> imageList;
    
    private String correctAnswer;
    
    private String analysis;
    
    /**
     * 难度：1-简单，2-中等，3-困难
     */
    private Integer difficulty;
    
    /**
     * 难度名称
     */
    private String difficultyName;
    
    private String knowledgePoints;
    
    /**
     * 知识点列表
     */
    private List<String> knowledgePointList;
    
    private Long createUserId;
    
    /**
     * 创建用户名称
     */
    private String createUserName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    
    /**
     * 用户答案（在答题记录中使用）
     */
    private String userAnswer;
    
    /**
     * 是否答对（在答题记录中使用）
     */
    private Boolean isCorrect;
    
    /**
     * 答题得分（在答题记录中使用）
     */
    private Integer score;
    
    /**
     * 答题次数统计
     */
    private Integer answerCount;
    
    /**
     * 正确率
     */
    private Double accuracy;
    
    // 构造方法
    public QuestionVO() {}
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public Integer getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(Integer questionType) {
        this.questionType = questionType;
        // 设置题目类型名称
        switch (questionType) {
            case 0:
                this.questionTypeName = "单选题";
                break;
            case 1:
                this.questionTypeName = "多选题";
                break;
            case 2:
                this.questionTypeName = "判断题";
                break;
            case 3:
                this.questionTypeName = "简答题";
                break;
            default:
                this.questionTypeName = "未知";
                break;
        }
    }
    
    public String getQuestionTypeName() {
        return questionTypeName;
    }
    
    public void setQuestionTypeName(String questionTypeName) {
        this.questionTypeName = questionTypeName;
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
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public List<QuestionSaveRequest.QuestionOption> getOptionList() {
        return optionList;
    }
    
    public void setOptionList(List<QuestionSaveRequest.QuestionOption> optionList) {
        this.optionList = optionList;
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
        // 设置难度名称
        switch (difficulty) {
            case 1:
                this.difficultyName = "简单";
                break;
            case 2:
                this.difficultyName = "中等";
                break;
            case 3:
                this.difficultyName = "困难";
                break;
            default:
                this.difficultyName = "未知";
                break;
        }
    }
    
    public String getDifficultyName() {
        return difficultyName;
    }
    
    public void setDifficultyName(String difficultyName) {
        this.difficultyName = difficultyName;
    }
    
    public String getKnowledgePoints() {
        return knowledgePoints;
    }
    
    public void setKnowledgePoints(String knowledgePoints) {
        this.knowledgePoints = knowledgePoints;
    }
    
    public List<String> getKnowledgePointList() {
        return knowledgePointList;
    }
    
    public void setKnowledgePointList(List<String> knowledgePointList) {
        this.knowledgePointList = knowledgePointList;
    }
    
    public Long getCreateUserId() {
        return createUserId;
    }
    
    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }
    
    public String getCreateUserName() {
        return createUserName;
    }
    
    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
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
    
    public String getUserAnswer() {
        return userAnswer;
    }
    
    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public Integer getAnswerCount() {
        return answerCount;
    }
    
    public void setAnswerCount(Integer answerCount) {
        this.answerCount = answerCount;
    }
    
    public Double getAccuracy() {
        return accuracy;
    }
    
    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }
    
    public String getImages() {
        return images;
    }
    
    public void setImages(String images) {
        this.images = images;
    }
    
    public List<String> getImageList() {
        return imageList;
    }
    
    public void setImageList(List<String> imageList) {
        this.imageList = imageList;
    }
    
    @Override
    public String toString() {
        return "QuestionVO{" +
                "id=" + id +
                ", subjectId=" + subjectId +
                ", subjectName='" + subjectName + '\'' +
                ", questionType=" + questionType +
                ", questionTypeName='" + questionTypeName + '\'' +
                ", title='" + title + '\'' +
                ", difficulty=" + difficulty +
                ", difficultyName='" + difficultyName + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}