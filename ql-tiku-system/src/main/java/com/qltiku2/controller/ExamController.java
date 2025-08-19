package com.qltiku2.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AnswerSubmitRequest;
import com.qltiku2.dto.ExamSaveRequest;
import com.qltiku2.dto.SimulationExamRequest;
import com.qltiku2.service.ExamService;
import com.qltiku2.utils.UserContext;
import com.qltiku2.vo.ExamVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 客户端考试控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/exams")
@CrossOrigin(origins = "*")
public class ExamController {
    
    @Autowired
    private ExamService examService;
    
    /**
     * 获取可用考试列表
     */
    @GetMapping
    public Result<List<ExamVO>> getAvailableExams(
            @RequestParam(required = false) Long subjectId) {
        try {
            List<ExamVO> exams = examService.listAvailableExams(subjectId);
            return Result.success(exams);
        } catch (Exception e) {
            return Result.error("获取考试列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取固定试卷列表（支持分页和模糊查询）
     */
    @GetMapping("/fixed-papers")
    public Result<IPage<ExamVO>> getFixedPapers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String keyword) {
        try {
            IPage<ExamVO> papers = examService.pageFixedPapers(current, size, subjectId, keyword);
            return Result.success(papers);
        } catch (Exception e) {
            return Result.error("获取固定试卷列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取考试详情
     */
    @GetMapping("/{id}")
    public Result<ExamVO> getExam(@PathVariable Long id) {
        try {
            ExamVO exam = examService.getExamById(id);
            if (exam == null) {
                return Result.error("考试不存在");
            }
            return Result.success(exam);
        } catch (Exception e) {
            return Result.error("获取考试详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 开始考试
     */
    @PostMapping("/{id}/start")
    public Result<Map<String, Object>> startExam(@PathVariable Long id) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> examSession = examService.startExam(id, userId);
            return Result.success(examSession);
        } catch (Exception e) {
            return Result.error("开始考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 开始固定试卷考试
     */
    @PostMapping("/fixed-papers/{id}/start")
    public Result<Map<String, Object>> startFixedPaperExam(@PathVariable Long id) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> examSession = examService.startFixedPaperExam(id, userId);
            return Result.success(examSession);
        } catch (Exception e) {
            return Result.error("开始固定试卷考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 提交考试答案
     */
    @PostMapping("/{id}/submit")
    public Result<Map<String, Object>> submitExam(
            @PathVariable Long id,
            @Valid @RequestBody List<AnswerSubmitRequest> answers) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> result = examService.submitExam(id, userId, answers);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("提交考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 提交固定试卷考试答案
     */
    @PostMapping("/fixed-papers/{id}/submit")
    public Result<Map<String, Object>> submitFixedPaperExam(
            @PathVariable Long id,
            @Valid @RequestBody List<AnswerSubmitRequest> answers) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> result = examService.submitFixedPaperExam(id, userId, answers);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("提交固定试卷考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取考试结果
     */
    @GetMapping("/{id}/result")
    public Result<Map<String, Object>> getExamResult(@PathVariable Long id) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> result = examService.getExamResult(id, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取考试结果失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户考试记录（支持搜索和分页）
     */
    @GetMapping("/records")
    public Result<IPage<Map<String, Object>>> getExamRecords(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer status) {
        try {
            Long userId = UserContext.getCurrentUserId();
            IPage<Map<String, Object>> records = examService.getUserExamRecords(userId, current, size, keyword, subjectId, status);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error("获取考试记录失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取考试记录详情
     */
    @GetMapping("/records/{recordId}")
    public Result<Map<String, Object>> getExamRecordDetail(@PathVariable String recordId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> detail;
            
            // 判断是否为模拟考试记录
            if (recordId.startsWith("simulation_")) {
                // 去掉 "simulation_" 前缀，获取实际的模拟考试ID
                String simulationExamId = recordId.substring("simulation_".length());
                detail = examService.getSimulationExamRecordDetail(simulationExamId, userId);
            } else {
                detail = examService.getExamRecordDetail(Long.parseLong(recordId), userId);
            }
            
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error("获取考试记录详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的所有考试记录批次
     */
    @GetMapping("/user-records/{examId}")
    public Result<List<Map<String, Object>>> getUserExamRecords(@PathVariable Long examId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            List<Map<String, Object>> records = examService.getUserExamRecords(examId, userId);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error("获取用户考试记录批次失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建考试
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<Long> createExam(@Valid @RequestBody ExamSaveRequest saveRequest) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            Long examId = examService.createExam(saveRequest, currentUserId);
            return Result.success(examId);
        } catch (Exception e) {
            return Result.error("创建考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建模拟考试
     */
    @PostMapping("/simulation")
    public Result<Map<String, Object>> createSimulationExam(@Valid @RequestBody SimulationExamRequest request) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> result = examService.createSimulationExam(request, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("创建模拟考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 提交模拟考试答案
     */
    @PostMapping("/simulation/{examId}/submit")
    public Result<Map<String, Object>> submitSimulationExam(
            @PathVariable String examId,
            @Valid @RequestBody List<AnswerSubmitRequest> answers) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> result = examService.submitSimulationExam(examId, userId, answers);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("提交模拟考试失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取模拟考试记录详情
     */
    @GetMapping("/simulation/records/{recordId}")
    public Result<Map<String, Object>> getSimulationExamRecordDetail(@PathVariable String recordId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            Map<String, Object> detail = examService.getSimulationExamRecordDetail(recordId, userId);
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error("获取模拟考试记录详情失败：" + e.getMessage());
        }
    }
}