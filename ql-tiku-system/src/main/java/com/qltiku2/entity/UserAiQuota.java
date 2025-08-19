package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户AI配额实体类
 * 
 * @author qltiku2
 */
@Data
@TableName("user_ai_quota")
public class UserAiQuota {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Integer dailyQuota;
    private Integer monthlyQuota;
    private Integer usedDaily;
    private Integer usedMonthly;
    private LocalDate lastResetDate;
    private Integer vipLevel;
    
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}