package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端数据分析控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/analytics")
@CrossOrigin(origins = "*")
public class AdminAnalyticsController {
    
    @Autowired
    private AnalyticsService analyticsService;
    
    /**
     * 获取分析数据（综合数据）
     */
    @GetMapping("/data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAnalyticsData(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String subjectId) {
        return analyticsService.getAnalyticsData(dateRange, subjectId);
    }
    
    /**
     * 获取系统概览统计
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getOverviewStats() {
        return analyticsService.getOverviewStats();
    }
    
    /**
     * 获取用户活跃度统计
     */
    @GetMapping("/user-activity")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getUserActivityStats(@RequestParam(required = false) String dateRange) {
        return analyticsService.getUserActivityStats(dateRange);
    }
    
    /**
     * 获取题目统计
     */
    @GetMapping("/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getQuestionStats() {
        return analyticsService.getQuestionStats();
    }
    
    /**
     * 获取考试统计
     */
    @GetMapping("/exams")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getExamAnalytics(@RequestParam(required = false) String dateRange) {
        return analyticsService.getExamAnalytics(dateRange);
    }
    
    /**
     * 获取错题统计
     */
    @GetMapping("/wrong-questions")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getWrongQuestionStats(@RequestParam(required = false) String dateRange) {
        return analyticsService.getWrongQuestionStats(dateRange);
    }
}