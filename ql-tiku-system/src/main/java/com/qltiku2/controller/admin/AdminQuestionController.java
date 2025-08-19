package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.QuestionQueryRequest;
import com.qltiku2.dto.QuestionSaveRequest;
import com.qltiku2.service.QuestionService;
import com.qltiku2.service.OssService;
import com.qltiku2.vo.QuestionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端题库管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/questions")
@CrossOrigin(origins = "*")
public class AdminQuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private OssService ossService;
    
    /**
     * 获取题目列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<QuestionVO>> getQuestions(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword) {
        
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
     * 获取题目详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<QuestionVO> getQuestion(@PathVariable Long id) {
        return questionService.getQuestionById(id);
    }
    
    /**
     * 创建题目
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createQuestion(@Valid @RequestBody QuestionSaveRequest request) {
        return questionService.createQuestion(request);
    }
    
    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateQuestion(@PathVariable Long id, 
                                        @Valid @RequestBody QuestionSaveRequest request) {
        return questionService.updateQuestion(id, request);
    }
    
    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteQuestion(@PathVariable Long id) {
        return questionService.deleteQuestion(id);
    }
    
    /**
     * 批量删除题目
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDeleteQuestions(@RequestBody List<Long> ids) {
        return questionService.batchDeleteQuestions(ids);
    }
    
    /**
     * 导入题目
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> importQuestions(@RequestBody List<QuestionSaveRequest> questions) {
        return questionService.importQuestions(questions);
    }
    
    /**
     * 导出题目
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<QuestionVO>> exportQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty) {
        
        QuestionQueryRequest request = new QuestionQueryRequest();
        request.setSubjectId(subjectId);
        request.setQuestionType(questionType);
        request.setDifficulty(difficulty);
        
        return questionService.exportQuestions(request);
    }
    
    /**
     * 上传题目图片
     */
    @PostMapping("/upload/image")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> uploadQuestionImage(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.badRequest("文件不能为空");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.equals("image/jpeg") && 
                 !contentType.equals("image/png") && 
                 !contentType.equals("image/gif"))) {
                return Result.badRequest("只支持JPG、PNG和GIF格式的图片");
            }
            
            // 验证文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.badRequest("文件大小不能超过5MB");
            }
            
            // 上传到OSS
            String url = ossService.uploadQuestionImage(file);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}