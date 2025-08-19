package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目实体类
 * 
 * @author qltiku2
 */
@TableName("question")
public class Question {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long subjectId;
    
    /**
     * 题目类型：0-单选，1-多选，2-判断，3-简答
     */
    private Integer questionType;
    
    private String title;
    
    private String content;
    
    /**
     * 选项（JSON格式存储）
     */
    private String options;
    
    /**
     * 题目图片URL数组（JSON格式存储，最多3张图片）
     */
    private String images;
    
    private String correctAnswer;
    
    private String analysis;
    
    /**
     * 难度：1-简单，2-中等，3-困难
     */
    private Integer difficulty;
    
    private String knowledgePoints;
    
    @TableField("sort_order")
    private Integer sortOrder;
    
    private Long createUserId;
    
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    // 非数据库字段
    @TableField(exist = false)
    private String subjectName;
    
    @TableField(exist = false)
    private List<String> optionList;
    
    // 构造方法
    public Question() {}
    
    public Question(Long subjectId, Integer questionType, String title, String content, 
                   String options, String correctAnswer, String analysis, Integer difficulty) {
        this.subjectId = subjectId;
        this.questionType = questionType;
        this.title = title;
        this.content = content;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.analysis = analysis;
        this.difficulty = difficulty;
    }
    
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
    
    public String getImages() {
        return images;
    }
    
    public void setImages(String images) {
        this.images = images;
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
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Long getCreateUserId() {
        return createUserId;
    }
    
    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
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
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
    
    public List<String> getOptionList() {
        return optionList;
    }
    
    public void setOptionList(List<String> optionList) {
        this.optionList = optionList;
    }
    
    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", subjectId=" + subjectId +
                ", questionType=" + questionType +
                ", title='" + title + '\'' +
                ", difficulty=" + difficulty +
                ", knowledgePoints='" + knowledgePoints + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}