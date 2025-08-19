package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员等级实体类
 * 
 * @author qltiku2
 * @since 2025-01-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("membership")
public class Membership implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会员等级名称
     */
    @TableField("level_name")
    private String levelName;

    /**
     * 会员等级代码
     */
    @TableField("level_code")
    private Integer levelCode;

    /**
     * 每日AI配额
     */
    @TableField("daily_quota")
    private Integer dailyQuota;

    /**
     * 每月AI配额
     */
    @TableField("monthly_quota")
    private Integer monthlyQuota;

    /**
     * 会员等级描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否启用
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}