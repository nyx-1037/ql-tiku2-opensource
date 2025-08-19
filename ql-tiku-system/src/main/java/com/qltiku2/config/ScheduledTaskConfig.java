package com.qltiku2.config;

import com.qltiku2.service.AiQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定时任务配置类
 * 用于自动重置AI配额
 * 
 * @author qltiku2
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {
    
    @Autowired
    private AiQuotaService aiQuotaService;
    
    /**
     * 每天0点重置所有用户的每日AI配额
     * 使用cron表达式: 0 0 0 * * ? 表示每天0点0分0秒执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyAiQuota() {
        try {
            log.info("开始执行每日AI配额重置任务 - {}", LocalDateTime.now());
            aiQuotaService.resetDailyQuota();
            log.info("每日AI配额重置任务执行完成 - {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("执行每日AI配额重置任务失败", e);
        }
    }
    
    /**
     * 每月1号0点重置所有用户的每月AI配额
     * 使用cron表达式: 0 0 0 1 * ? 表示每月1号0点0分0秒执行
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyAiQuota() {
        try {
            log.info("开始执行每月AI配额重置任务 - {}", LocalDateTime.now());
            aiQuotaService.resetMonthlyQuota();
            log.info("每月AI配额重置任务执行完成 - {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("执行每月AI配额重置任务失败", e);
        }
    }
    
    /**
     * 测试用：每分钟执行一次的重置任务（开发测试时使用）
     * 注意：生产环境请注释掉此方法
     */
    // @Scheduled(cron = "0 * * * * ?")
    public void testResetTask() {
        log.info("测试定时任务执行中 - {}", LocalDateTime.now());
    }
}