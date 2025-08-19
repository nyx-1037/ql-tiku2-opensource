package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.UserAiQuota;

import java.util.List;
import java.util.Map;

/**
 * AI配额服务接口
 * 
 * @author qltiku2
 */
public interface AiQuotaService extends IService<UserAiQuota> {
    /**
     * 检查用户是否还有AI使用配额
     */
    boolean hasQuota(Long userId, String aiType);
    
    /**
     * 消耗AI使用次数
     */
    boolean consumeQuota(Long userId, String aiType, int tokens);
    
    /**
     * 获取用户配额信息
     */
    UserAiQuota getUserQuota(Long userId);
    
    /**
     * 初始化用户配额
     */
    void initializeQuota(Long userId);
    
    /**
     * 重置所有用户每日配额
     */
    void resetDailyQuota();
    
    /**
     * 重置指定用户每日配额
     */
    boolean resetDailyQuota(Long userId);
    
    /**
     * 重置所有用户每月配额
     */
    void resetMonthlyQuota();
    
    /**
     * 重置指定用户每月配额
     */
    boolean resetMonthlyQuota(Long userId);
    
    /**
     * 获取用户今日剩余AI次数
     */
    int getRemainingDailyQuota(Long userId);
    
    /**
     * 更新用户配额
     */
    boolean updateUserQuota(Long userId, Integer vipLevel, Integer dailyQuota, Integer monthlyQuota);
    
    /**
     * 批量更新用户配额
     */
    boolean batchUpdateQuotas(List<Long> userIds, Integer vipLevel, Integer dailyQuota, Integer monthlyQuota);
    
    /**
     * 获取配额统计信息
     */
    Map<String, Object> getQuotaStats();
}