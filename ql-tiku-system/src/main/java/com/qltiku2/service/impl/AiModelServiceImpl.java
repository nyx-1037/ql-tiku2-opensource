package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AiModelSaveRequest;
import com.qltiku2.entity.AiModel;
import com.qltiku2.mapper.AiModelMapper;
import com.qltiku2.service.AiModelService;
import com.qltiku2.vo.AiModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI模型配置服务实现类
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Slf4j
@Service
public class AiModelServiceImpl implements AiModelService {

    @Autowired
    private AiModelMapper aiModelMapper;

    @Override
    public Result<IPage<AiModelVO>> getModelPage(Integer current, Integer size, String keyword, Boolean enabled) {
        try {
            Page<AiModel> page = new Page<>(current, size);
            
            LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                    .like(AiModel::getName, keyword)
                    .or()
                    .like(AiModel::getCode, keyword)
                    .or()
                    .like(AiModel::getDescription, keyword)
                );
            }
            
            // 启用状态筛选
            if (enabled != null) {
                queryWrapper.eq(AiModel::getEnabled, enabled);
            }
            
            queryWrapper.orderByAsc(AiModel::getSortOrder)
                       .orderByDesc(AiModel::getCreateTime);
            
            IPage<AiModel> modelPage = aiModelMapper.selectPage(page, queryWrapper);
            
            // 转换为VO
            IPage<AiModelVO> voPage = modelPage.convert(this::convertToVO);
            
            return Result.success(voPage);
        } catch (Exception e) {
            log.error("分页查询AI模型失败", e);
            return Result.error("查询失败");
        }
    }

    @Override
    public Result<List<AiModelVO>> getEnabledModels() {
        try {
            List<AiModel> models = aiModelMapper.selectEnabledModels();
            List<AiModelVO> voList = models.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取启用的AI模型列表失败", e);
            return Result.error("获取模型列表失败");
        }
    }

    @Override
    public Result<AiModelVO> getModelById(Long id) {
        try {
            AiModel model = aiModelMapper.selectById(id);
            if (model == null) {
                return Result.error("模型不存在");
            }
            return Result.success(convertToVO(model));
        } catch (Exception e) {
            log.error("根据ID获取AI模型失败", e);
            return Result.error("获取模型详情失败");
        }
    }

    @Override
    public Result<AiModelVO> getModelByCode(String code) {
        try {
            AiModel model = aiModelMapper.selectByCode(code);
            if (model == null) {
                return Result.error("模型不存在");
            }
            return Result.success(convertToVO(model));
        } catch (Exception e) {
            log.error("根据代码获取AI模型失败", e);
            return Result.error("获取模型详情失败");
        }
    }

    @Override
    public Result<String> createModel(AiModelSaveRequest request) {
        try {
            // 检查代码是否已存在
            AiModel existingModel = aiModelMapper.selectByCode(request.getCode());
            if (existingModel != null) {
                return Result.error("模型代码已存在");
            }
            
            AiModel model = new AiModel();
            BeanUtils.copyProperties(request, model);
            
            // 设置默认排序权重
            if (model.getSortOrder() == null) {
                model.setSortOrder(0);
            }
            
            int result = aiModelMapper.insert(model);
            if (result > 0) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建AI模型失败", e);
            return Result.error("创建失败");
        }
    }

    @Override
    public Result<String> updateModel(Long id, AiModelSaveRequest request) {
        try {
            AiModel existingModel = aiModelMapper.selectById(id);
            if (existingModel == null) {
                return Result.error("模型不存在");
            }
            
            // 检查代码是否被其他模型使用
            AiModel codeModel = aiModelMapper.selectByCode(request.getCode());
            if (codeModel != null && !codeModel.getId().equals(id)) {
                return Result.error("模型代码已被其他模型使用");
            }
            
            BeanUtils.copyProperties(request, existingModel);
            existingModel.setId(id);
            
            int result = aiModelMapper.updateById(existingModel);
            if (result > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新AI模型失败", e);
            return Result.error("更新失败");
        }
    }

    @Override
    public Result<String> deleteModel(Long id) {
        try {
            AiModel model = aiModelMapper.selectById(id);
            if (model == null) {
                return Result.error("模型不存在");
            }
            
            int result = aiModelMapper.deleteById(id);
            if (result > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除AI模型失败", e);
            return Result.error("删除失败");
        }
    }

    @Override
    public Result<String> batchDeleteModels(List<Long> ids) {
        try {
            int result = aiModelMapper.deleteBatchIds(ids);
            if (result > 0) {
                return Result.success("批量删除成功");
            } else {
                return Result.error("批量删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除AI模型失败", e);
            return Result.error("批量删除失败");
        }
    }

    @Override
    public Result<String> toggleModelStatus(Long id, Boolean enabled) {
        try {
            AiModel model = aiModelMapper.selectById(id);
            if (model == null) {
                return Result.error("模型不存在");
            }
            
            model.setEnabled(enabled);
            int result = aiModelMapper.updateById(model);
            if (result > 0) {
                return Result.success(enabled ? "启用成功" : "禁用成功");
            } else {
                return Result.error("操作失败");
            }
        } catch (Exception e) {
            log.error("切换AI模型状态失败", e);
            return Result.error("操作失败");
        }
    }

    @Override
    public AiModel getDefaultModel() {
        try {
            List<AiModel> models = aiModelMapper.selectEnabledModels();
            if (!models.isEmpty()) {
                return models.get(0); // 返回第一个启用的模型作为默认模型
            }
            
            // 如果没有启用的模型，返回null或抛出异常
            log.warn("没有找到启用的AI模型");
            return null;
        } catch (Exception e) {
            log.error("获取默认AI模型失败", e);
            return null;
        }
    }

    /**
     * 转换为VO对象
     */
    private AiModelVO convertToVO(AiModel model) {
        AiModelVO vo = new AiModelVO();
        BeanUtils.copyProperties(model, vo);
        return vo;
    }
}