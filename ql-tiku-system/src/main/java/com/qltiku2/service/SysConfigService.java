package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.SysConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 *
 * @author qltiku2
 * @since 2024-01-01
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值，如果不存在则返回默认值
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 更新配置值
     *
     * @param configKey   配置键
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean updateConfigValue(String configKey, String configValue);

    /**
     * 批量更新配置
     *
     * @param configMap 配置映射
     * @return 是否成功
     */
    boolean batchUpdateConfig(Map<String, String> configMap);

    /**
     * 获取所有系统配置
     *
     * @return 配置映射
     */
    Map<String, Object> getAllSystemConfig();

    /**
     * 获取指定配置键的配置列表
     *
     * @param configKeys 配置键列表
     * @return 配置列表
     */
    List<SysConfig> getConfigsByKeys(List<String> configKeys);

    /**
     * 添加或更新配置
     *
     * @param configKey    配置键
     * @param configValue  配置值
     * @param configType   配置类型
     * @param remark       备注
     * @param isSystemType 是否系统配置类型
     * @return 是否成功
     */
    boolean saveOrUpdateConfig(String configKey, String configValue, String configType, String remark, String isSystemType);

    /**
     * 删除配置
     *
     * @param configKey 配置键
     * @return 是否成功
     */
    boolean deleteConfig(String configKey);

    /**
     * 获取网站基本信息
     *
     * @return 网站信息
     */
    Map<String, Object> getSiteInfo();
}