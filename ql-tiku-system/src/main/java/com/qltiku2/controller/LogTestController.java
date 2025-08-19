package com.qltiku2.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志测试控制器 - 用于测试操作日志系统
 */
@RestController
@RequestMapping("/log-test")
public class LogTestController {
    
    /**
     * 测试GET请求日志记录
     */
    @GetMapping("/get")
    public Map<String, Object> testGet(@RequestParam(required = false) String param) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "GET请求测试成功");
        result.put("param", param);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 测试POST请求日志记录
     */
    @PostMapping("/post")
    public Map<String, Object> testPost(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "POST请求测试成功");
        result.put("receivedData", requestData);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 测试PUT请求日志记录
     */
    @PutMapping("/put/{id}")
    public Map<String, Object> testPut(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "PUT请求测试成功");
        result.put("id", id);
        result.put("receivedData", requestData);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 测试DELETE请求日志记录
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> testDelete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "DELETE请求测试成功");
        result.put("deletedId", id);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 测试异常情况的日志记录
     */
    @GetMapping("/error")
    public Map<String, Object> testError() {
        // 故意抛出异常来测试错误日志记录
        throw new RuntimeException("这是一个测试异常，用于验证错误日志记录功能");
    }
}