package com.qltiku2.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户AI配额视图对象
 * 
 * @author qltiku2
 */
@Data
public class UserAiQuotaVO {
    
    /**
     * 配额记录ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * VIP等级
     */
    private Integer vipLevel;
    
    /**
     * 每日配额
     */
    private Integer dailyQuota;
    
    /**
     * 每日已使用
     */
    private Integer usedDaily;
    
    /**
     * 每月配额
     */
    private Integer monthlyQuota;
    
    /**
     * 每月已使用
     */
    private Integer usedMonthly;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    
    /**
     * 每日剩余配额
     */
    public Integer getDailyRemaining() {
        return dailyQuota - usedDaily;
    }
    
    /**
     * 每月剩余配额
     */
    public Integer getMonthlyRemaining() {
        return monthlyQuota - usedMonthly;
    }
}
