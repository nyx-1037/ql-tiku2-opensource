package com.qltiku2.dto;

/**
 * 题目查询请求DTO
 * 
 * @author qltiku2
 */
public class QuestionQueryRequest {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 题目类型：0-单选题，1-多选题，2-判断题，3-填空题，4-简答题
     */
    private Integer questionType;
    
    /**
     * 难度：1-简单，2-中等，3-困难
     */
    private Integer difficulty;
    
    /**
     * 关键词搜索
     */
    private String keyword;
    
    /**
     * 知识点
     */
    private String knowledgePoints;
    
    /**
     * 创建用户ID
     */
    private Long createUserId;
    
    /**
     * 排序字段
     */
    private String sortField = "createTime";
    
    /**
     * 排序方向：asc-升序，desc-降序
     */
    private String sortOrder = "desc";
    
    // 构造方法
    public QuestionQueryRequest() {}
    
    // Getter和Setter方法
    public Integer getCurrent() {
        return current;
    }
    
    public void setCurrent(Integer current) {
        this.current = current;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
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
    
    public Integer getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public String getKnowledgePoints() {
        return knowledgePoints;
    }
    
    public void setKnowledgePoints(String knowledgePoints) {
        this.knowledgePoints = knowledgePoints;
    }
    
    public Long getCreateUserId() {
        return createUserId;
    }
    
    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }
    
    public String getSortField() {
        return sortField;
    }
    
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    @Override
    public String toString() {
        return "QuestionQueryRequest{" +
                "current=" + current +
                ", size=" + size +
                ", subjectId=" + subjectId +
                ", questionType=" + questionType +
                ", difficulty=" + difficulty +
                ", keyword='" + keyword + '\'' +
                ", knowledgePoints='" + knowledgePoints + '\'' +
                ", createUserId=" + createUserId +
                ", sortField='" + sortField + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}