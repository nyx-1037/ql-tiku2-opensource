package com.qltiku2.config;

import com.qltiku2.utils.QuestionCacheUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 缓存清理配置 - 应用启动时清理缓存
 * 
 * @author qltiku2
 */
@Component
public class CacheCleanConfig implements CommandLineRunner {
    
    @Autowired
    private QuestionCacheUtils questionCache;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("应用启动，开始清理题目缓存...");
            questionCache.clearAll();
            System.out.println("题目缓存清理完成");
        } catch (Exception e) {
            System.err.println("清理缓存失败: " + e.getMessage());
        }
    }
}