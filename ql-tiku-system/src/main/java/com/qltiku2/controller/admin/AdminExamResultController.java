package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理端考试结果控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminExamResultController {
    
    /**
     * 获取考试结果列表
     */
    @GetMapping("/exam-results")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getExamResults(
            @RequestParam(required = false) Long examId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        // 模拟考试结果数据
        List<Map<String, Object>> results = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (int i = 1; i <= size; i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", (long) i);
            result.put("examId", examId != null ? examId : 1L);
            result.put("userId", (long) i);
            result.put("userName", "用户" + i);
            result.put("score", 80 + (i % 20));
            result.put("totalScore", 100);
            result.put("duration", 60 + (i % 30));
            result.put("submitTime", LocalDateTime.now().minusHours(i).format(formatter));
            result.put("status", i % 3 == 0 ? "completed" : "submitted");
            results.add(result);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("list", results);
        response.put("total", 50L);
        response.put("current", current);
        response.put("size", size);
        
        return Result.success(response);
    }
    
    /**
     * 获取考试记录列表
     */
    @GetMapping("/exam-records")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getExamRecords(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        // 模拟考试记录数据
        List<Map<String, Object>> records = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (int i = 1; i <= size; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", (long) i);
            record.put("examId", examId != null ? examId : 1L);
            record.put("examTitle", "考试" + i);
            record.put("userId", userId != null ? userId : (long) i);
            record.put("userName", "用户" + i);
            record.put("startTime", LocalDateTime.now().minusHours(i + 2).format(formatter));
            record.put("endTime", LocalDateTime.now().minusHours(i).format(formatter));
            record.put("score", 75 + (i % 25));
            record.put("totalScore", 100);
            record.put("duration", 90 + (i % 30));
            record.put("status", i % 4 == 0 ? "timeout" : "completed");
            records.add(record);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("list", records);
        response.put("total", 100L);
        response.put("current", current);
        response.put("size", size);
        
        return Result.success(response);
    }
    
    /**
     * 获取考试记录详情
     */
    @GetMapping("/exam-records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getExamRecord(@PathVariable Long id) {
        
        Map<String, Object> record = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        record.put("id", id);
        record.put("examId", 1L);
        record.put("examTitle", "Java基础考试");
        record.put("userId", 1L);
        record.put("userName", "张三");
        record.put("startTime", LocalDateTime.now().minusHours(2).format(formatter));
        record.put("endTime", LocalDateTime.now().minusHours(1).format(formatter));
        record.put("score", 85);
        record.put("totalScore", 100);
        record.put("duration", 60);
        record.put("status", "completed");
        
        // 答题详情
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", (long) i);
            answer.put("questionTitle", "题目" + i);
            answer.put("questionType", i % 3 == 0 ? "multiple" : (i % 2 == 0 ? "judge" : "single"));
            answer.put("userAnswer", "A");
            answer.put("correctAnswer", i % 5 == 0 ? "B" : "A");
            answer.put("isCorrect", i % 5 != 0);
            answer.put("score", i % 5 != 0 ? 10 : 0);
            answers.add(answer);
        }
        record.put("answers", answers);
        
        return Result.success(record);
    }
    
    /**
     * 删除考试记录
     */
    @DeleteMapping("/exam-records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> deleteExamRecord(@PathVariable Long id) {
        // 模拟删除操作
        return Result.success(true);
    }
    
    /**
     * 获取考试统计
     */
    @GetMapping("/exam-records/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getExamStats(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String dateRange) {
        
        Map<String, Object> stats = new HashMap<>();
        
        // 基础统计
        stats.put("totalRecords", 150);
        stats.put("completedRecords", 135);
        stats.put("averageScore", 82.5);
        stats.put("passRate", 85.2);
        
        // 分数分布
        List<Map<String, Object>> scoreDistribution = new ArrayList<>();
        String[] ranges = {"0-60", "60-70", "70-80", "80-90", "90-100"};
        int[] counts = {15, 25, 35, 45, 30};
        for (int i = 0; i < ranges.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("range", ranges[i]);
            item.put("count", counts[i]);
            scoreDistribution.add(item);
        }
        stats.put("scoreDistribution", scoreDistribution);
        
        // 时间趋势
        List<Map<String, Object>> timeTrend = new ArrayList<>();
        for (int i = 7; i >= 1; i--) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("count", 10 + (i % 5) * 3);
            item.put("averageScore", 75 + (i % 10));
            timeTrend.add(item);
        }
        stats.put("timeTrend", timeTrend);
        
        return Result.success(stats);
    }
}