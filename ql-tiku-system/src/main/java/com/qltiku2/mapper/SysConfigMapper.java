package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统配置 Mapper 接口
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @Select("SELECT config_value FROM sys_config WHERE config_key = #{configKey}")
    String getConfigValue(@Param("configKey") String configKey);

    /**
     * 更新配置值
     *
     * @param configKey   配置键
     * @param configValue 配置值
     * @return 影响行数
     */
    @Update("UPDATE sys_config SET config_value = #{configValue}, update_time = NOW() WHERE config_key = #{configKey}")
    int updateConfigValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

    /**
     * 获取所有系统配置
     *
     * @return 系统配置列表
     */
    @Select("SELECT * FROM sys_config WHERE config_type = 'Y' ORDER BY id")
    List<SysConfig> getSystemConfigs();

    /**
     * 批量获取配置
     *
     * @param configKeys 配置键列表
     * @return 配置列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_config WHERE config_key IN " +
            "<foreach collection='configKeys' item='key' open='(' separator=',' close=')'>" +
            "#{key}" +
            "</foreach>" +
            "</script>")
    List<SysConfig> getConfigsByKeys(@Param("configKeys") List<String> configKeys);
}