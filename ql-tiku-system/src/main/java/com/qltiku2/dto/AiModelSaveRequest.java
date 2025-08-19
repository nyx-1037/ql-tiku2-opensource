package com.qltiku2.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AI模型保存请求DTO
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Data
public class AiModelSaveRequest {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String name;

    /**
     * 模型代码标识
     */
    @NotBlank(message = "模型代码不能为空")
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
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    /**
     * 模型配置参数（JSON格式）
     */
    private String config;

    /**
     * 排序权重
     */
    private Integer sortOrder;
}