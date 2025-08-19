package com.qltiku2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 考试查询请求DTO
 * 
 * @author qltiku2
 */
public class ExamQueryRequest {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 关键词（标题）
     */
    private String keyword;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 考试状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;
    
    /**
     * 开始时间范围 - 起始
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeBegin;
    
    /**
     * 开始时间范围 - 结束
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeEnd;
    
    /**
     * 创建人ID
     */
    private Long createBy;
    
    public ExamQueryRequest() {}
    
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
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Long getSubjectId() {
        return subjectId;
    }
    
    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public LocalDateTime getStartTimeBegin() {
        return startTimeBegin;
    }
    
    public void setStartTimeBegin(LocalDateTime startTimeBegin) {
        this.startTimeBegin = startTimeBegin;
    }
    
    public LocalDateTime getStartTimeEnd() {
        return startTimeEnd;
    }
    
    public void setStartTimeEnd(LocalDateTime startTimeEnd) {
        this.startTimeEnd = startTimeEnd;
    }
    
    public Long getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }
    
    @Override
    public String toString() {
        return "ExamQueryRequest{" +
                "current=" + current +
                ", size=" + size +
                ", keyword='" + keyword + '\'' +
                ", subjectId=" + subjectId +
                ", status=" + status +
                ", startTimeBegin=" + startTimeBegin +
                ", startTimeEnd=" + startTimeEnd +
                ", createBy=" + createBy +
                '}';
    }
}