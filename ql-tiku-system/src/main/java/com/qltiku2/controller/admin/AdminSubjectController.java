package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.SubjectQueryRequest;
import com.qltiku2.dto.SubjectSaveRequest;
import com.qltiku2.service.SubjectService;
import com.qltiku2.vo.SubjectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理端科目管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/subjects")
@CrossOrigin(origins = "*")
public class AdminSubjectController {
    
    @Autowired
    private SubjectService subjectService;
    
    /**
     * 获取科目列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<SubjectVO>> getSubjects(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        
        SubjectQueryRequest request = new SubjectQueryRequest();
        request.setCurrent(current);
        request.setSize(size);
        request.setKeyword(keyword);
        request.setStatus(status);
        
        return subjectService.getSubjectPage(request);
    }
    
    /**
     * 获取所有启用的科目
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SubjectVO>> getEnabledSubjects() {
        return subjectService.getEnabledSubjects();
    }
    
    /**
     * 创建科目
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createSubject(@Valid @RequestBody SubjectSaveRequest request) {
        return subjectService.createSubject(request);
    }
    
    /**
     * 更新科目
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateSubject(@PathVariable Long id, 
                                       @Valid @RequestBody SubjectSaveRequest request) {
        return subjectService.updateSubject(id, request);
    }
    
    /**
     * 删除科目
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteSubject(@PathVariable Long id) {
        return subjectService.deleteSubject(id);
    }
    
    /**
     * 批量删除科目
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDeleteSubjects(@RequestBody List<Long> ids) {
        return subjectService.batchDeleteSubjects(ids);
    }
    
    /**
     * 更新科目状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateSubjectStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        if (status == null) {
            return Result.badRequest("状态不能为空");
        }
        return subjectService.updateSubjectStatus(id, status);
    }
}