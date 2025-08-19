package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.service.WrongBookService;
import com.qltiku2.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 错题本控制器
 */
@RestController
@RequestMapping("/wrong-book")
@CrossOrigin(origins = "*")
public class WrongBookController {

    @Autowired
    private WrongBookService wrongBookService;

    /**
     * 获取错题列表
     */
    @GetMapping
    public Result getWrongQuestions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String wrongType
    ) {
        System.out.println("=== WrongBookController.getWrongQuestions 开始 ===");
        System.out.println("请求参数: page=" + page + ", size=" + size + ", subjectId=" + subjectId + ", type=" + type + ", difficulty=" + difficulty);
        
        try {
            System.out.println("开始获取当前用户ID...");
            Long userId = UserContext.getCurrentUserId();
            System.out.println("当前用户ID: " + userId);
            
            System.out.println("开始调用 wrongBookService.getWrongQuestions...");
            Result result = wrongBookService.getWrongQuestions(userId, page, size, subjectId, type, difficulty, wrongType);
            System.out.println("wrongBookService.getWrongQuestions 调用成功，结果: " + result);
            
            System.out.println("=== WrongBookController.getWrongQuestions 成功结束 ===");
            return result;
        } catch (Exception e) {
            System.err.println("=== WrongBookController.getWrongQuestions 发生异常 ===");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 移除错题
     */
    @DeleteMapping("/{id}")
    public Result removeWrongQuestion(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return wrongBookService.removeWrongQuestion(userId, id);
    }

    /**
     * 添加错题
     */
    @PostMapping
    public Result addWrongQuestion(@RequestBody Map<String, Object> params) {
        Long userId = UserContext.getCurrentUserId();
        Long questionId = Long.valueOf(params.get("questionId").toString());
        String userAnswer = params.get("userAnswer").toString();
        String wrongType = params.get("wrongType") != null ? params.get("wrongType").toString() : "PRACTICE";
        return wrongBookService.addWrongQuestion(userId, questionId, userAnswer, wrongType);
    }

    /**
     * 清空错题本
     */
    @DeleteMapping("/clear")
    public Result clearWrongQuestions() {
        Long userId = UserContext.getCurrentUserId();
        return wrongBookService.clearWrongQuestions(userId);
    }
}