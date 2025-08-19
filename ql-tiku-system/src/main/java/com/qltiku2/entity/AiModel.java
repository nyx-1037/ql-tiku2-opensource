package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI模型配置实体类
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ai_models")
public class AiModel {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模型名称
     */
    @TableField("name")
    private String name;

    /**
     * 模型代码标识
     */
    @TableField("code")
    private String code;

    /**
     * 模型描述
     */
    @TableField("description")
    private String description;

    /**
     * 模型提供商
     */
    @TableField("provider")
    private String provider;

    /**
     * 是否启用：1-启用，0-禁用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 模型配置参数（JSON格式）
     */
    @TableField("config")
    private String config;

    /**
     * 排序权重
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}