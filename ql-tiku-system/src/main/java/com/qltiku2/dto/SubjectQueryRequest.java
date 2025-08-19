package com.qltiku2.dto;

/**
 * 科目查询请求DTO
 * 
 * @author qltiku2
 */
public class SubjectQueryRequest {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 科目名称（模糊搜索）
     */
    private String name;
    
    /**
     * 关键词（模糊搜索）
     */
    private String keyword;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 排序字段
     */
    private String sortField = "sortOrder";
    
    /**
     * 排序方向：asc-升序，desc-降序
     */
    private String sortOrder = "asc";
    
    // 构造方法
    public SubjectQueryRequest() {}
    
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
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
        return "SubjectQueryRequest{" +
                "current=" + current +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", sortField='" + sortField + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}