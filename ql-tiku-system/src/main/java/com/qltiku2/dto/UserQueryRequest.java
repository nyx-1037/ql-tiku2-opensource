package com.qltiku2.dto;

import lombok.Data;

/**
 * 用户查询请求
 * 
 * @author qltiku2
 */
@Data
public class UserQueryRequest {
    
    /**
     * 当前页
     */
    private Integer current = 1;
    
    /**
     * 页大小
     */
    private Integer size = 10;
    
    /**
     * 关键词（用户名、姓名）
     */
    private String keyword;
    
    /**
     * 用户类型（0-学生，1-教师，2-管理员）
     */
    private Integer userType;
    
    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}