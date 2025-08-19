package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 注册码使用记录实体类
 * 
 * @author qltiku2
 */
@Data
@TableName("registration_code_usage")
public class RegistrationCodeUsage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 注册码ID
     */
    private Long codeId;
    
    /**
     * 使用用户ID
     */
    private Long userId;
    
    /**
     * 使用时间
     */
    @TableField(value = "used_time", fill = FieldFill.INSERT)
    private LocalDateTime usedTime;
    
    /**
     * 用户IP地址
     */
    private String ipAddress;
}