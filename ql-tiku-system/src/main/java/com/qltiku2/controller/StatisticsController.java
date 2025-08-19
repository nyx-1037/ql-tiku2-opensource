package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.service.StatisticsService;
import com.qltiku2.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端统计控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {
    
    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 获取个人统计数据
     */
    @GetMapping("/personal")
    public Result<Map<String, Object>> getPersonalStats() {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getPersonalStats(userId);
        } catch (RuntimeException e) {
            // 用户未登录时返回默认统计数据
            if (e.getMessage().contains("用户未登录") || e.getMessage().contains("获取当前用户信息失败")) {
                Map<String, Object> defaultStats = new HashMap<>();
                Map<String, Object> today = new HashMap<>();
                today.put("practiceCount", 0);
                today.put("accuracy", 0.0);
                
                Map<String, Object> total = new HashMap<>();
                total.put("totalCount", 0);
                
                defaultStats.put("today", today);
                defaultStats.put("total", total);
                
                return Result.success(defaultStats);
            }
            return Result.error("获取个人统计数据失败：" + e.getMessage());
        } catch (Exception e) {
            return Result.error("获取个人统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取学习报告
     */
    @GetMapping("/report")
    public Result<Map<String, Object>> getLearningReport(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getLearningReport(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取学习报告失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取概览统计数据
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverviewStats(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getOverviewStats(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取概览统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取学习趋势数据
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrendData(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String trendType,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getTrendData(userId, dateRange, trendType, subjectId);
        } catch (Exception e) {
            return Result.error("获取学习趋势数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取正确率分析数据
     */
    @GetMapping("/accuracy")
    public Result<Map<String, Object>> getAccuracyAnalysis(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getAccuracyAnalysis(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取正确率分析数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取科目分布数据（可按科目筛选）
     */
    @GetMapping("/subject-distribution")
    public Result<Map<String, Object>> getSubjectDistribution(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getSubjectDistribution(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取科目分布数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取难度分析数据
     */
    @GetMapping("/difficulty")
    public Result<Map<String, Object>> getDifficultyAnalysis(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getDifficultyAnalysis(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取难度分析数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取错题分析数据
     */
    @GetMapping("/wrong-analysis")
    public Result<Map<String, Object>> getWrongQuestionAnalysis(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getWrongQuestionAnalysis(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取错题分析数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取学习建议
     */
    @GetMapping("/suggestions")
    public Result<Map<String, Object>> getLearningSuggestions(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long subjectId) {
        try {
            Long userId = UserContext.getCurrentUser().getId();
            return statisticsService.getLearningSuggestions(userId, dateRange, subjectId);
        } catch (Exception e) {
            return Result.error("获取学习建议失败：" + e.getMessage());
        }
    }
}