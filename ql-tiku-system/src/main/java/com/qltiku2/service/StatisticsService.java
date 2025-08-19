package com.qltiku2.service;

import com.qltiku2.common.Result;

import java.util.Map;

/**
 * 统计服务接口
 * 
 * @author qltiku2
 */
public interface StatisticsService {
    
    /**
     * 获取个人统计数据
     * @param userId 用户ID
     * @return 个人统计数据
     */
    Result<Map<String, Object>> getPersonalStats(Long userId);
    
    /**
     * 获取学习报告
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID
     * @return 学习报告数据
     */
    Result<Map<String, Object>> getLearningReport(Long userId, String dateRange, Long subjectId);
    
    /**
     * 获取概览统计数据
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID
     * @return 概览统计数据
     */
    Result<Map<String, Object>> getOverviewStats(Long userId, String dateRange, Long subjectId);
    
    /**
     * 获取学习趋势数据
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param trendType 趋势类型（daily/weekly/monthly）
     * @param subjectId 科目ID
     * @return 学习趋势数据
     */
    Result<Map<String, Object>> getTrendData(Long userId, String dateRange, String trendType, Long subjectId);
    
    /**
     * 获取正确率分析数据
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID
     * @return 正确率分析数据
     */
    Result<Map<String, Object>> getAccuracyAnalysis(Long userId, String dateRange, Long subjectId);
    
    /**
     * 获取科目分布数据（可按科目筛选）
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID（可选）
     * @return 科目分布数据
     */
    Result<Map<String, Object>> getSubjectDistribution(Long userId, String dateRange, Long subjectId);

    /**
     * 获取难度分析数据
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID
     * @return 难度分析数据
     */
    Result<Map<String, Object>> getDifficultyAnalysis(Long userId, String dateRange, Long subjectId);
    
    /**
     * 获取错题分析数据
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID
     * @return 错题分析数据
     */
    Result<Map<String, Object>> getWrongQuestionAnalysis(Long userId, String dateRange, Long subjectId);
    
    /**
     * 获取学习建议
     * @param userId 用户ID
     * @param dateRange 日期范围
     * @param subjectId 科目ID
     * @return 学习建议
     */
    Result<Map<String, Object>> getLearningSuggestions(Long userId, String dateRange, Long subjectId);
}