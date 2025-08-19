package com.qltiku2.controller;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 科目控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {
    
    @Autowired
    private SubjectService subjectService;
    
    /**
     * 分页查询科目列表（管理员和教师）
     */
    @PostMapping("/page")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<IPage<SubjectVO>> getSubjectPage(@RequestBody SubjectQueryRequest request) {
        return subjectService.getSubjectPage(request);
    }
    
    /**
     * 获取所有科目列表（所有用户）
     */
    @GetMapping
    public Result<List<SubjectVO>> getAllSubjects() {
        return subjectService.getEnabledSubjects();
    }
    
    /**
     * 获取所有启用的科目列表（所有用户）
     */
    @GetMapping("/enabled")
    public Result<List<SubjectVO>> getEnabledSubjects() {
        return subjectService.getEnabledSubjects();
    }
    
    /**
     * 根据ID获取科目详情
     */
    @GetMapping("/{id}")
    public Result<SubjectVO> getSubjectById(@PathVariable Long id) {
        return subjectService.getSubjectById(id);
    }
    
    /**
     * 创建科目（管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createSubject(@Valid @RequestBody SubjectSaveRequest request) {
        return subjectService.createSubject(request);
    }
    
    /**
     * 更新科目（管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateSubject(@PathVariable Long id, 
                                       @Valid @RequestBody SubjectSaveRequest request) {
        return subjectService.updateSubject(id, request);
    }
    
    /**
     * 删除科目（管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteSubject(@PathVariable Long id) {
        return subjectService.deleteSubject(id);
    }
    
    /**
     * 批量删除科目（管理员）
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDeleteSubjects(@RequestBody List<Long> ids) {
        return subjectService.batchDeleteSubjects(ids);
    }
    
    /**
     * 更新科目状态（管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateSubjectStatus(@PathVariable Long id, 
                                             @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.badRequest("状态值不正确");
        }
        return subjectService.updateSubjectStatus(id, status);
    }
    
    /**
     * 更新科目排序（管理员）
     */
    @PutMapping("/{id}/sort")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateSubjectSort(@PathVariable Long id, 
                                           @RequestBody Map<String, Integer> request) {
        Integer sortOrder = request.get("sortOrder");
        if (sortOrder == null || sortOrder < 0) {
            return Result.badRequest("排序值不正确");
        }
        return subjectService.updateSubjectSort(id, sortOrder);
    }
    
    /**
     * 获取科目统计信息（管理员和教师）
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<Object> getSubjectStatistics() {
        return subjectService.getSubjectStatistics();
    }
    
    /**
     * 获取科目状态列表
     */
    @GetMapping("/statuses")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getSubjectStatuses() {
        List<Map<String, Object>> statuses = new ArrayList<>();
        
        Map<String, Object> disabled = new HashMap<>();
        disabled.put("value", 0);
        disabled.put("label", "禁用");
        statuses.add(disabled);
        
        Map<String, Object> enabled = new HashMap<>();
        enabled.put("value", 1);
        enabled.put("label", "启用");
        statuses.add(enabled);
        
        return Result.success(statuses);
    }
}