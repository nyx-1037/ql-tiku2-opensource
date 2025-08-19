package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.ExamQueryRequest;
import com.qltiku2.dto.ExamSaveRequest;
import com.qltiku2.dto.QuestionQueryRequest;
import com.qltiku2.service.ExamService;
import com.qltiku2.service.QuestionService;
import com.qltiku2.utils.UserContext;
import com.qltiku2.vo.ExamVO;
import com.qltiku2.vo.QuestionVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 管理端考试控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/exams")
public class AdminExamController {
    
    @Resource
    private ExamService examService;
    
    @Resource
    private QuestionService questionService;
    
    /**
     * 分页查询考试
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<ExamVO>> pageExams(ExamQueryRequest queryRequest) {
        IPage<ExamVO> page = examService.pageExams(queryRequest);
        return Result.success(page);
    }
    
    /**
     * 获取考试详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ExamVO> getExam(@PathVariable Long id) {
        ExamVO exam = examService.getExamById(id);
        if (exam == null) {
            return Result.error("考试不存在");
        }
        return Result.success(exam);
    }
    
    /**
     * 创建考试
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> createExam(@Valid @RequestBody ExamSaveRequest saveRequest) {
        Long currentUserId = UserContext.getCurrentUserId();
        Long examId = examService.createExam(saveRequest, currentUserId);
        return Result.success(examId);
    }
    
    /**
     * 更新考试
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> updateExam(@PathVariable Long id, @Valid @RequestBody ExamSaveRequest saveRequest) {
        saveRequest.setId(id);
        boolean success = examService.updateExam(saveRequest);
        return success ? Result.success(true) : Result.error("更新失败");
    }
    
    /**
     * 删除考试
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> deleteExam(@PathVariable Long id) {
        boolean success = examService.deleteExam(id);
        return success ? Result.success(true) : Result.error("删除失败");
    }
    
    /**
     * 批量删除考试
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> batchDeleteExams(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的考试");
        }
        boolean success = examService.batchDeleteExams(ids);
        return success ? Result.success(true) : Result.error("批量删除失败");
    }
    
    /**
     * 更新考试状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> updateExamStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            return Result.error("状态参数无效");
        }
        boolean success = examService.updateExamStatus(id, status);
        return success ? Result.success(true) : Result.error("状态更新失败");
    }
    
    /**
     * 发布考试
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> publishExam(@PathVariable Long id) {
        boolean success = examService.updateExamStatus(id, 1); // 1表示已发布状态
        return success ? Result.success(true) : Result.error("发布失败");
    }
    
    /**
     * 获取考试列表（不分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ExamVO>> listExams(@RequestParam(required = false) Long subjectId) {
        List<ExamVO> exams = examService.listExams(subjectId);
        return Result.success(exams);
    }
    
    /**
     * 分页获取题目列表（用于考试题目选择）
     */
    @GetMapping("/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<QuestionVO>> getQuestionsForExam(QuestionQueryRequest queryRequest) {
        Result<IPage<QuestionVO>> result = questionService.getQuestionPage(queryRequest);
        return result;
    }
    
    /**
     * 导出考试数据
     */
    @GetMapping("/{id}/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportExamData(@PathVariable Long id, HttpServletResponse response) {
        try {
            examService.exportExamData(id, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}