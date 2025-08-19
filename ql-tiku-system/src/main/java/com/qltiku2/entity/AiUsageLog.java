package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI使用日志实体类
 * 
 * @author qltiku2
 */
@Data
@TableName("ai_usage_log")
public class AiUsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String aiType;
    private Integer tokensUsed;
    private String ipAddress;
    private String userAgent;
    private Long modelId;
    private String modelCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime usageTime;
}
