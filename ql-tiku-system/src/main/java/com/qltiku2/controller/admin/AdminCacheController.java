package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.utils.QuestionCacheUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员缓存管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/cache")
public class AdminCacheController {
    
    @Autowired
    private QuestionCacheUtils questionCache;
    
    /**
     * 清除所有题目缓存（管理员专用）
     * 
     * @return 操作结果
     */
    @PostMapping("/clear/question")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> clearQuestionCache() {
        try {
            questionCache.clearAll();
            return Result.success("题目缓存已清除");
        } catch (Exception e) {
            return Result.error("清除缓存失败：" + e.getMessage());
        }
    }
    
    /**
     * 清除所有缓存（管理员专用）
     * 
     * @return 操作结果
     */
    @PostMapping("/clear/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> clearAllCache() {
        try {
            questionCache.clearAll();
            return Result.success("所有缓存已清除");
        } catch (Exception e) {
            return Result.error("清除缓存失败：" + e.getMessage());
        }
    }
}