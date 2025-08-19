package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AiModelSaveRequest;
import com.qltiku2.entity.AiModel;
import com.qltiku2.vo.AiModelVO;

import java.util.List;

/**
 * AI模型配置服务接口
 *
 * @author qltiku2
 * @since 2024-01-01
 */
public interface AiModelService {

    /**
     * 分页查询AI模型列表
     */
    Result<IPage<AiModelVO>> getModelPage(Integer current, Integer size, String keyword, Boolean enabled);

    /**
     * 获取所有启用的模型列表
     */
    Result<List<AiModelVO>> getEnabledModels();

    /**
     * 根据ID获取模型详情
     */
    Result<AiModelVO> getModelById(Long id);

    /**
     * 根据代码获取模型详情
     */
    Result<AiModelVO> getModelByCode(String code);

    /**
     * 创建AI模型
     */
    Result<String> createModel(AiModelSaveRequest request);

    /**
     * 更新AI模型
     */
    Result<String> updateModel(Long id, AiModelSaveRequest request);

    /**
     * 删除AI模型
     */
    Result<String> deleteModel(Long id);

    /**
     * 批量删除AI模型
     */
    Result<String> batchDeleteModels(List<Long> ids);

    /**
     * 启用/禁用AI模型
     */
    Result<String> toggleModelStatus(Long id, Boolean enabled);

    /**
     * 获取默认模型
     */
    AiModel getDefaultModel();
}