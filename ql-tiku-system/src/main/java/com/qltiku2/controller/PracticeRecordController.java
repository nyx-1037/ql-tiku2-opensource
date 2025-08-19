package com.qltiku2.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.entity.PracticeRecord;
import com.qltiku2.service.PracticeRecordService;
import com.qltiku2.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 练习记录控制器
 */
@RestController
@RequestMapping("/practice-record")
public class PracticeRecordController {
    
    @Autowired
    private PracticeRecordService practiceRecordService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 获取练习记录列表
     */
    @GetMapping("/list")
    public Result<Page<PracticeRecord>> getPracticeRecordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            HttpServletRequest request) {
        
        System.out.println("=== PracticeRecordController.getPracticeRecordList 开始 ===");
        System.out.println("请求URL: " + request.getRequestURL());
        System.out.println("请求URI: " + request.getRequestURI());
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("参数 - page: " + page + ", size: " + size + ", subjectId: " + subjectId + ", questionType: " + questionType + ", difficulty: " + difficulty);
        
        String token = request.getHeader("Authorization");
        System.out.println("Authorization头: " + token);
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        System.out.println("解析的用户ID: " + userId);
        
        try {
            Page<PracticeRecord> result = practiceRecordService.getPracticeRecordPage(
                    page, size, userId, subjectId, questionType, difficulty);
            System.out.println("查询结果: " + result.getRecords().size() + " 条记录");
            System.out.println("=== PracticeRecordController.getPracticeRecordList 成功结束 ===");
            return Result.success(result);
        } catch (Exception e) {
            System.err.println("=== PracticeRecordController.getPracticeRecordList 异常 ===");
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 获取练习记录详情
     */
    @GetMapping("/details/{recordId}")
    public Result<List<Map<String, Object>>> getPracticeRecordDetails(
            @PathVariable Long recordId,
            HttpServletRequest request) {
        
        System.out.println("=== PracticeRecordController.getPracticeRecordDetails 开始 ===");
        System.out.println("请求URL: " + request.getRequestURL());
        System.out.println("请求URI: " + request.getRequestURI());
        System.out.println("练习记录ID: " + recordId);
        
        String token = request.getHeader("Authorization");
        System.out.println("Authorization头: " + token);
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        System.out.println("解析的用户ID: " + userId);
        
        try {
            List<Map<String, Object>> result = practiceRecordService.getPracticeRecordDetailsByRecordId(userId, recordId);
            System.out.println("查询结果: " + (result != null ? result.size() : 0) + " 条记录");
            System.out.println("=== PracticeRecordController.getPracticeRecordDetails 成功结束 ===");
            return Result.success(result);
        } catch (Exception e) {
            System.err.println("=== PracticeRecordController.getPracticeRecordDetails 异常 ===");
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 创建练习记录
     */
    @PostMapping("/create")
    public Result<Long> createPracticeRecord(
            @RequestParam Long subjectId,
            @RequestParam String subjectName,
            @RequestParam Integer questionType,
            @RequestParam Integer difficulty,
            HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        Long practiceRecordId = practiceRecordService.createPracticeRecord(
                userId, subjectId, subjectName, questionType, difficulty);
        
        return Result.success(practiceRecordId);
    }
    
    /**
     * 更新练习记录
     */
    @PutMapping("/update")
    public Result<Void> updatePracticeRecord(
            @RequestParam Long practiceRecordId,
            @RequestParam Integer totalQuestions,
            @RequestParam Integer correctCount,
            @RequestParam Integer wrongCount,
            @RequestParam Integer totalTime,
            @RequestParam Integer status) {
        
        practiceRecordService.updatePracticeRecord(
                practiceRecordId, totalQuestions, correctCount, wrongCount, totalTime, status);
        
        return Result.success();
    }
    
    /**
     * 获取练习记录统计信息
     */
    @GetMapping("/stats/{recordId}")
    public Result<Map<String, Object>> getPracticeRecordStats(
            @PathVariable Long recordId,
            HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        try {
            Map<String, Object> stats = practiceRecordService.getPracticeRecordStats(userId, recordId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取练习统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 添加答题记录
     */
    @PostMapping("/add-answer")
    public Result<Void> addAnswerRecord(
            @RequestParam Long questionId,
            @RequestParam String userAnswer,
            @RequestParam Boolean isCorrect,
            @RequestParam Integer timeSpent,
            @RequestParam(defaultValue = "1") Integer practiceType,
            HttpServletRequest request) {
        
        System.out.println("=== PracticeRecordController.addAnswerRecord 开始 ===");
        System.out.println("请求URL: " + request.getRequestURL());
        System.out.println("请求URI: " + request.getRequestURI());
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("参数 - questionId: " + questionId + ", userAnswer: " + userAnswer + ", isCorrect: " + isCorrect + ", timeSpent: " + timeSpent + ", practiceType: " + practiceType);
        
        String token = request.getHeader("Authorization");
        System.out.println("Authorization头: " + token);
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        System.out.println("解析的用户ID: " + userId);
        
        try {
            practiceRecordService.addAnswerRecord(
                    userId, questionId, userAnswer, isCorrect, timeSpent, practiceType);
            System.out.println("✅ 答题记录添加成功");
            System.out.println("=== PracticeRecordController.addAnswerRecord 成功结束 ===");
            return Result.success();
        } catch (Exception e) {
            System.err.println("=== PracticeRecordController.addAnswerRecord 异常 ===");
            e.printStackTrace();
            return Result.error("添加答题记录失败：" + e.getMessage());
        }
    }
}