package com.qltiku2.dto;

/**
 * 反馈查询请求DTO
 * 
 * @author qltiku2
 */
public class FeedbackQueryRequest {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 用户ID（客户端查询自己的反馈时使用）
     */
    private Long userId;
    
    /**
     * 反馈类型：1-bug反馈，2-功能建议，3-其他反馈
     */
    private Integer feedbackType;
    
    /**
     * 状态：0-待处理，1-已受理，2-已处理，3-已修复，4-已采纳，5-已失效，6-已撤销
     */
    private Integer status;
    
    /**
     * 关键词搜索（标题、内容）
     */
    private String keyword;
    
    /**
     * 用户名搜索（管理端使用）
     */
    private String username;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
    /**
     * 排序字段
     */
    private String sortField = "createTime";
    
    /**
     * 排序方向：asc-升序，desc-降序
     */
    private String sortOrder = "desc";
    
    // 构造方法
    public FeedbackQueryRequest() {}
    
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getFeedbackType() {
        return feedbackType;
    }
    
    public void setFeedbackType(Integer feedbackType) {
        this.feedbackType = feedbackType;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
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
        return "FeedbackQueryRequest{" +
                "current=" + current +
                ", size=" + size +
                ", userId=" + userId +
                ", feedbackType=" + feedbackType +
                ", status=" + status +
                ", keyword='" + keyword + '\'' +
                ", username='" + username + '\'' +
                ", sortField='" + sortField + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}