package com.qltiku2.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.OperationLog;
import com.qltiku2.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/operation-log")
public class OperationLogController {
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    public Map<String, Object> getOperationLogPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationMethod,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        Page<OperationLog> page = new Page<>(current, size);
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like("username", username);
        }
        if (operationMethod != null && !operationMethod.trim().isEmpty()) {
            queryWrapper.like("operation_method", operationMethod);
        }
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            queryWrapper.like("ip_address", ipAddress);
        }
        if (statusCode != null) {
            queryWrapper.eq("status_code", statusCode);
        }
        if (startTime != null) {
            queryWrapper.ge("create_time", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("create_time", endTime);
        }
        
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");
        
        IPage<OperationLog> result = operationLogService.page(page, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", result);
        
        return response;
    }
    
    /**
     * 根据ID查询操作日志详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getOperationLogById(@PathVariable Long id) {
        OperationLog operationLog = operationLogService.getById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (operationLog != null) {
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", operationLog);
        } else {
            response.put("code", 404);
            response.put("message", "操作日志不存在");
        }
        
        return response;
    }
    
    /**
     * 删除操作日志
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteOperationLog(@PathVariable Long id) {
        boolean success = operationLogService.removeById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", 200);
            response.put("message", "删除成功");
        } else {
            response.put("code", 500);
            response.put("message", "删除失败");
        }
        
        return response;
    }
    
    /**
     * 批量删除操作日志
     */
    @DeleteMapping("/batch")
    public Map<String, Object> batchDeleteOperationLog(@RequestBody java.util.List<Long> ids) {
        boolean success = operationLogService.removeByIds(ids);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("code", 200);
            response.put("message", "批量删除成功");
        } else {
            response.put("code", 500);
            response.put("message", "批量删除失败");
        }
        
        return response;
    }
}