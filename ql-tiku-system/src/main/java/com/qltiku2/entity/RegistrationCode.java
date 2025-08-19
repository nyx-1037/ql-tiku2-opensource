package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 注册码实体类
 * 
 * @author qltiku2
 */
@Data
@TableName("registration_code")
public class RegistrationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String code;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private Long createdBy;
    
    /**
     * 会员等级代码，关联membership表的level_code
     */
    private Integer membershipLevel;
    
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}