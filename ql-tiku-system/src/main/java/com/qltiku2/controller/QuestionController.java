package com.qltiku2.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AnswerSubmitRequest;
import com.qltiku2.dto.QuestionQueryRequest;
import com.qltiku2.dto.QuestionSaveRequest;
import com.qltiku2.service.AiQuotaService;
import com.qltiku2.service.PracticeCacheService;
import com.qltiku2.service.QuestionService;
import com.qltiku2.utils.JwtUtils;
import com.qltiku2.vo.AnswerSubmitResponse;
import com.qltiku2.vo.QuestionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/question")
@CrossOrigin(origins = "*")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private PracticeCacheService practiceCacheService;
    
    @Autowired
    private AiQuotaService aiQuotaService;
    
    
    /**
     * 分页查询题目列表
     */
    @PostMapping("/page")
    public Result<IPage<QuestionVO>> getQuestionPage(@RequestBody QuestionQueryRequest request) {
        return questionService.getQuestionPage(request);
    }
    
    /**
     * 根据ID获取题目详情
     */
    @GetMapping("/{id}")
    public Result<QuestionVO> getQuestionById(@PathVariable Long id) {
        return questionService.getQuestionById(id);
    }
    
    /**
     * 创建题目（教师和管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<String> createQuestion(@Valid @RequestBody QuestionSaveRequest request) {
        return questionService.createQuestion(request);
    }
    
    /**
     * 更新题目（教师和管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<String> updateQuestion(@PathVariable Long id, 
                                        @Valid @RequestBody QuestionSaveRequest request) {
        return questionService.updateQuestion(id, request);
    }
    
    /**
     * 删除题目（教师和管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<String> deleteQuestion(@PathVariable Long id) {
        return questionService.deleteQuestion(id);
    }
    
    /**
     * 批量删除题目（管理员）
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDeleteQuestions(@RequestBody List<Long> ids) {
        return questionService.batchDeleteQuestions(ids);
    }
    
    /**
     * 随机获取题目（用于练习）
     */
    @GetMapping("/random")
    public Result<List<QuestionVO>> getRandomQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "10") Integer count) {
        return questionService.getRandomQuestions(subjectId, questionType, difficulty, count);
    }
    
    /**
     * 获取练习题目（支持顺序和随机不重复）
     */
    @GetMapping("/practice")
    public Result<List<QuestionVO>> getPracticeQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam(defaultValue = "random") String mode,
            @RequestParam(required = false) String excludeIds,
            HttpServletRequest request) {
        
        // 获取当前用户ID
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        return questionService.getPracticeQuestions(userId, subjectId, questionType, difficulty, count, mode, excludeIds);
    }
    
    /**
     * 重置练习缓存
     */
    @DeleteMapping("/practice/cache")
    public Result<String> resetPracticeCache(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "random") String mode,
            HttpServletRequest request) {
        
        // 获取当前用户ID
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        // 清除缓存
        practiceCacheService.clearCache(userId, subjectId, questionType, difficulty, mode);
        
        return Result.success("练习缓存已重置");
    }
    
    /**
     * 搜索题目（支持模糊查询和多种筛选条件）
     */
    @GetMapping("/search")
    public Result<IPage<QuestionVO>> searchQuestions(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword) {
        
        // 添加调试日志
        System.out.println("=== 搜索题目参数调试 ===");
        System.out.println("subjectId: " + subjectId);
        System.out.println("questionType: " + questionType);
        System.out.println("difficulty: " + difficulty);
        System.out.println("keyword: " + keyword);
        System.out.println("========================");
        
        QuestionQueryRequest request = new QuestionQueryRequest();
        request.setCurrent(current);
        request.setSize(size);
        request.setSubjectId(subjectId);
        request.setQuestionType(questionType);
        request.setDifficulty(difficulty);
        request.setKeyword(keyword);
        
        return questionService.getQuestionPage(request);
    }
    
    /**
     * 获取题目统计信息
     */
    @GetMapping("/statistics")
    public Result<Object> getQuestionStatistics() {
        return questionService.getQuestionStatistics();
    }
    
    /**
     * 获取科目题目数量统计（公开接口）
     */
    @GetMapping("/subject-stats")
    public Result<Map<String, Object>> getSubjectQuestionStats() {
        return questionService.getSubjectQuestionStats();
    }
    
    /**
     * 获取指定科目的题目数量
     */
    @GetMapping("/count/{subjectId}")
    public Result<Integer> getQuestionCountBySubject(@PathVariable Long subjectId) {
        return questionService.getQuestionCountBySubject(subjectId);
    }
    
    /**
     * 批量导入题目（教师和管理员）
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<String> importQuestions(@RequestBody List<QuestionSaveRequest> questions) {
        return questionService.importQuestions(questions);
    }
    
    /**
     * 导出题目（教师和管理员）
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<List<QuestionVO>> exportQuestions(@RequestBody QuestionQueryRequest request) {
        return questionService.exportQuestions(request);
    }
    
    /**
     * 获取题目类型列表
     */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getQuestionTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        
        Map<String, Object> type1 = new HashMap<>();
        type1.put("value", 0);
        type1.put("label", "单选题");
        types.add(type1);
        
        Map<String, Object> type2 = new HashMap<>();
        type2.put("value", 1);
        type2.put("label", "多选题");
        types.add(type2);
        
        Map<String, Object> type3 = new HashMap<>();
        type3.put("value", 2);
        type3.put("label", "判断题");
        types.add(type3);
        
        Map<String, Object> type4 = new HashMap<>();
        type4.put("value", 3);
        type4.put("label", "简答题");
        types.add(type4);
        
        return Result.success(types);
    }
    
    /**
     * 获取难度等级列表
     */
    @GetMapping("/difficulties")
    public Result<List<Map<String, Object>>> getDifficulties() {
        List<Map<String, Object>> difficulties = new ArrayList<>();
        
        Map<String, Object> diff1 = new HashMap<>();
        diff1.put("value", 1);
        diff1.put("label", "简单");
        difficulties.add(diff1);
        
        Map<String, Object> diff2 = new HashMap<>();
        diff2.put("value", 2);
        diff2.put("label", "中等");
        difficulties.add(diff2);
        
        Map<String, Object> diff3 = new HashMap<>();
        diff3.put("value", 3);
        diff3.put("label", "困难");
        difficulties.add(diff3);
        
        return Result.success(difficulties);
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/submit")
    public Result<AnswerSubmitResponse> submitAnswer(@Valid @RequestBody AnswerSubmitRequest request) {
        return questionService.submitAnswer(request);
    }
    
    /**
     * AI判题
     */
    @PostMapping("/ai-grading")
    public Result<Map<String, Object>> aiGrading(@Valid @RequestBody Map<String, Object> request) {
        return questionService.aiGrading(request);
    }
    
    /**
     * AI判题流式接口
     */
    @PostMapping(value = "/ai-grading-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter aiGradingStream(@RequestBody Map<String, Object> request, 
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse response) {
        // 设置响应头
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        
        // 获取当前用户ID
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        // 创建SSE发射器
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        try {
            // 检查并消耗AI配额
            if (aiQuotaService.hasQuota(userId, "grading")) {
                // 消耗配额 - 在调用大模型之前扣除
                boolean consumed = aiQuotaService.consumeQuota(userId, "grading", 1);
                if (!consumed) {
                    emitter.send(SseEmitter.event()
                        .data("{\"error\":\"配额消耗失败，请稍后重试\"}")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("error"));
                    emitter.send(SseEmitter.event().data("[DONE]").id("done").name("done"));
                    emitter.complete();
                    return emitter;
                }
                
                // 调用服务层处理AI判题流式响应
                questionService.aiGradingStream(request, userId, emitter);
            } else {
                // 配额不足
                emitter.send(SseEmitter.event()
                    .data("{\"error\":\"今日AI判题次数已用完，请明天再试或联系管理员\"}")
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("error"));
                emitter.send(SseEmitter.event().data("[DONE]").id("done").name("done"));
                emitter.complete();
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * 获取AI判题历史记录
     */
    @GetMapping("/ai-grading/history/{questionId}")
    public Result<Map<String, Object>> getAiGradingHistory(@PathVariable Long questionId) {
        return questionService.getAiGradingHistory(questionId);
    }
}