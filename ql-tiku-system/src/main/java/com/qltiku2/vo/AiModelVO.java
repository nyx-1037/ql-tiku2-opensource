package com.qltiku2.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI模型配置VO
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Data
public class AiModelVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型代码标识
     */
    private String code;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 模型提供商
     */
    private String provider;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 模型配置参数
     */
    private String config;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}