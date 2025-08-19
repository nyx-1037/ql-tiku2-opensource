package com.qltiku2.service;

import com.qltiku2.common.Result;

import java.util.Map;

/**
 * 数据分析服务接口
 * 
 * @author qltiku2
 */
public interface AnalyticsService {
    
    /**
     * 获取综合分析数据
     */
    Result<Map<String, Object>> getAnalyticsData(String dateRange, String subjectId);
    
    /**
     * 获取系统概览统计
     */
    Result<Map<String, Object>> getOverviewStats();
    
    /**
     * 获取用户活跃度统计
     */
    Result<Map<String, Object>> getUserActivityStats(String dateRange);
    
    /**
     * 获取题目统计
     */
    Result<Map<String, Object>> getQuestionStats();
    
    /**
     * 获取考试统计
     */
    Result<Map<String, Object>> getExamAnalytics(String dateRange);
    
    /**
     * 获取错题统计
     */
    Result<Map<String, Object>> getWrongQuestionStats(String dateRange);
}