package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.UserAiQuota;
import com.qltiku2.entity.AiUsageLog;
import com.qltiku2.mapper.UserAiQuotaMapper;
import com.qltiku2.mapper.AiUsageLogMapper;
import com.qltiku2.service.AiQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI配额服务实现类
 * 
 * @author qltiku2
 */
@Slf4j
@Service
public class AiQuotaServiceImpl extends ServiceImpl<UserAiQuotaMapper, UserAiQuota> implements AiQuotaService {
    
    @Autowired
    private UserAiQuotaMapper userAiQuotaMapper;
    
    @Autowired
    private AiUsageLogMapper aiUsageLogMapper;
    
    @Override
    public boolean hasQuota(Long userId, String aiType) {
        UserAiQuota quota = getUserQuota(userId);
        if (quota == null) {
            // 如果没有配额记录，初始化一个
            initializeQuota(userId);
            quota = getUserQuota(userId);
        }
        
        // 检查是否需要重置每日配额
        if (quota.getLastResetDate() == null || !quota.getLastResetDate().equals(LocalDate.now())) {
            quota.setUsedDaily(0);
            quota.setLastResetDate(LocalDate.now());
            userAiQuotaMapper.updateById(quota);
        }
        
        return quota.getUsedDaily() < quota.getDailyQuota() && quota.getUsedMonthly() < quota.getMonthlyQuota();
    }
    
    @Override
    @Transactional
    public boolean consumeQuota(Long userId, String aiType, int tokens) {
        if (!hasQuota(userId, aiType)) {
            return false;
        }
        
        UserAiQuota quota = getUserQuota(userId);
        quota.setUsedDaily(quota.getUsedDaily() + 1);
        quota.setUsedMonthly(quota.getUsedMonthly() + 1);
        userAiQuotaMapper.updateById(quota);
        
        // 记录使用日志
        AiUsageLog log = new AiUsageLog();
        log.setUserId(userId);
        log.setAiType(aiType);
        log.setTokensUsed(tokens);
        log.setUsageTime(LocalDateTime.now());
        
        // 获取IP地址和User-Agent
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setIpAddress(getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        
        aiUsageLogMapper.insert(log);
        
        return true;
    }
    
    @Override
    public UserAiQuota getUserQuota(Long userId) {
        QueryWrapper<UserAiQuota> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return userAiQuotaMapper.selectOne(wrapper);
    }
    
    @Override
    public void initializeQuota(Long userId) {
        UserAiQuota quota = new UserAiQuota();
        quota.setUserId(userId);
        quota.setDailyQuota(10); // 默认每日10次
        quota.setMonthlyQuota(100); // 默认每月100次
        quota.setUsedDaily(0);
        quota.setUsedMonthly(0);
        quota.setLastResetDate(LocalDate.now());
        quota.setVipLevel(0);
        quota.setCreateTime(LocalDateTime.now());
        quota.setUpdateTime(LocalDateTime.now());
        userAiQuotaMapper.insert(quota);
    }
    
    @Override
    public void resetDailyQuota() {
        userAiQuotaMapper.resetAllDailyQuota();
        log.info("已重置所有用户的每日AI配额");
    }
    
    @Override
    public void resetMonthlyQuota() {
        userAiQuotaMapper.resetAllMonthlyQuota();
        log.info("已重置所有用户的每月AI配额");
    }
    
    @Override
    public int getRemainingDailyQuota(Long userId) {
        UserAiQuota quota = getUserQuota(userId);
        if (quota == null) {
            initializeQuota(userId);
            quota = getUserQuota(userId);
        }
        
        // 检查是否需要重置每日配额
        if (quota.getLastResetDate() == null || !quota.getLastResetDate().equals(LocalDate.now())) {
            quota.setUsedDaily(0);
            quota.setLastResetDate(LocalDate.now());
            userAiQuotaMapper.updateById(quota);
        }
        
        return Math.max(0, quota.getDailyQuota() - quota.getUsedDaily());
    }
    
    @Override
    public boolean resetDailyQuota(Long userId) {
        try {
            UpdateWrapper<UserAiQuota> wrapper = new UpdateWrapper<>();
            wrapper.eq("user_id", userId)
                   .set("used_daily", 0)
                   .set("last_reset_date", LocalDate.now())
                   .set("updated_time", LocalDateTime.now());
            return userAiQuotaMapper.update(null, wrapper) > 0;
        } catch (Exception e) {
            log.error("重置用户{}每日配额失败", userId, e);
            return false;
        }
    }
    
    @Override
    public boolean resetMonthlyQuota(Long userId) {
        try {
            UpdateWrapper<UserAiQuota> wrapper = new UpdateWrapper<>();
            wrapper.eq("user_id", userId)
                   .set("used_monthly", 0)
                   .set("updated_time", LocalDateTime.now());
            return userAiQuotaMapper.update(null, wrapper) > 0;
        } catch (Exception e) {
            log.error("重置用户{}每月配额失败", userId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean updateUserQuota(Long userId, Integer vipLevel, Integer dailyQuota, Integer monthlyQuota) {
        try {
            UserAiQuota quota = getUserQuota(userId);
            if (quota == null) {
                initializeQuota(userId);
                quota = getUserQuota(userId);
            }
            
            if (vipLevel != null) {
                quota.setVipLevel(vipLevel);
            }
            if (dailyQuota != null) {
                quota.setDailyQuota(dailyQuota);
            }
            if (monthlyQuota != null) {
                quota.setMonthlyQuota(monthlyQuota);
            }
            quota.setUpdateTime(LocalDateTime.now());
            
            return userAiQuotaMapper.updateById(quota) > 0;
        } catch (Exception e) {
            log.error("更新用户{}配额失败", userId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean batchUpdateQuotas(List<Long> userIds, Integer vipLevel, Integer dailyQuota, Integer monthlyQuota) {
        try {
            for (Long userId : userIds) {
                updateUserQuota(userId, vipLevel, dailyQuota, monthlyQuota);
            }
            return true;
        } catch (Exception e) {
            log.error("批量更新用户配额失败", e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getQuotaStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 总用户数
            long totalUsers = userAiQuotaMapper.selectCount(null);
            stats.put("totalUsers", totalUsers);
            
            // VIP用户数
            QueryWrapper<UserAiQuota> vipWrapper = new QueryWrapper<>();
            vipWrapper.gt("vip_level", 0);
            long vipUsers = userAiQuotaMapper.selectCount(vipWrapper);
            stats.put("vipUsers", vipUsers);
            
            // 今日活跃用户数（今日有使用记录的用户）
            QueryWrapper<UserAiQuota> activeWrapper = new QueryWrapper<>();
            activeWrapper.gt("used_daily", 0)
                        .eq("last_reset_date", LocalDate.now());
            long activeUsers = userAiQuotaMapper.selectCount(activeWrapper);
            stats.put("activeUsers", activeUsers);
            
            // 今日总使用次数
            QueryWrapper<UserAiQuota> usageWrapper = new QueryWrapper<>();
            usageWrapper.eq("last_reset_date", LocalDate.now());
            List<UserAiQuota> quotas = userAiQuotaMapper.selectList(usageWrapper);
            int totalUsage = quotas.stream().mapToInt(UserAiQuota::getUsedDaily).sum();
            stats.put("totalUsage", totalUsage);
            
        } catch (Exception e) {
            log.error("获取配额统计信息失败", e);
        }
        return stats;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}