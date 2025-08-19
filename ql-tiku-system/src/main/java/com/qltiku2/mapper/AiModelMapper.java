package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI模型配置Mapper接口
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {

    /**
     * 查询所有启用的模型
     */
    @Select("SELECT * FROM ai_models WHERE enabled = 1 AND deleted = 0 ORDER BY sort_order ASC, create_time ASC")
    List<AiModel> selectEnabledModels();

    /**
     * 根据模型代码查询模型
     */
    @Select("SELECT * FROM ai_models WHERE code = #{code} AND deleted = 0")
    AiModel selectByCode(String code);
}