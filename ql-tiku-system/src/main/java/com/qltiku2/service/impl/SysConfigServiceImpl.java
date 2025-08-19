package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.SysConfig;
import com.qltiku2.mapper.SysConfigMapper;
import com.qltiku2.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务实现类
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Slf4j
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String CONFIG_CACHE_PREFIX = "sys_config:";
    private static final long CACHE_EXPIRE_TIME = 24; // 24小时过期
    
    /**
     * 应用启动时加载所有配置到Redis
     */
    @PostConstruct
    public void initConfigCache() {
        try {
            log.info("开始加载系统配置到Redis缓存...");
            System.out.println("=== 开始加载系统配置到Redis缓存 ===");
            List<SysConfig> allConfigs = list();
            for (SysConfig config : allConfigs) {
                String cacheKey = CONFIG_CACHE_PREFIX + config.getConfigKey();
                redisTemplate.opsForValue().set(cacheKey, config.getConfigValue(), CACHE_EXPIRE_TIME, TimeUnit.HOURS);
                System.out.println("缓存配置: " + config.getConfigKey() + " = " + config.getConfigValue());
            }
            log.info("系统配置加载到Redis缓存完成，共加载{}条配置", allConfigs.size());
            System.out.println("=== 系统配置加载到Redis缓存完成，共加载" + allConfigs.size() + "条配置 ===");
        } catch (Exception e) {
            log.error("加载系统配置到Redis缓存失败", e);
            System.err.println("=== 加载系统配置到Redis缓存失败: " + e.getMessage() + " ===");
        }
    }

    @Override
    public String getConfigValue(String configKey) {
        try {
            // 优先从Redis缓存获取
            String cacheKey = CONFIG_CACHE_PREFIX + configKey;
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }
            
            // 缓存中没有，从数据库获取
            String dbValue = baseMapper.getConfigValue(configKey);
            if (dbValue != null) {
                // 将数据库值缓存到Redis
                redisTemplate.opsForValue().set(cacheKey, dbValue, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
            }
            return dbValue;
        } catch (Exception e) {
            log.warn("从Redis获取配置失败，降级到数据库查询，configKey: {}", configKey, e);
            return baseMapper.getConfigValue(configKey);
        }
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfigValue(String configKey, String configValue) {
        try {
            // 更新数据库
            int result = baseMapper.updateConfigValue(configKey, configValue);
            if (result > 0) {
                // 更新Redis缓存
                try {
                    String cacheKey = CONFIG_CACHE_PREFIX + configKey;
                    redisTemplate.opsForValue().set(cacheKey, configValue, CACHE_EXPIRE_TIME, TimeUnit.HOURS);
                    log.debug("更新Redis缓存成功，configKey: {}, configValue: {}", configKey, configValue);
                } catch (Exception redisException) {
                    log.warn("更新Redis缓存失败，但数据库更新成功，configKey: {}", configKey, redisException);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("更新配置失败，configKey: {}, configValue: {}", configKey, configValue, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateConfig(Map<String, String> configMap) {
        try {
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                updateConfigValue(entry.getKey(), entry.getValue());
            }
            return true;
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getAllSystemConfig() {
        List<SysConfig> configs = baseMapper.getSystemConfigs();
        Map<String, Object> configMap = new HashMap<>();
        
        for (SysConfig config : configs) {
            // 直接使用配置值，因为config_type现在表示是否为系统配置
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        
        return configMap;
    }

    @Override
    public List<SysConfig> getConfigsByKeys(List<String> configKeys) {
        return baseMapper.getConfigsByKeys(configKeys);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateConfig(String configKey, String configValue, String configType, String remark, String isSystemType) {
        try {
            QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", configKey);
            SysConfig existConfig = getOne(queryWrapper);
            
            if (existConfig != null) {
                // 更新
                existConfig.setConfigValue(configValue);
                existConfig.setRemark(remark);
                return updateById(existConfig);
            } else {
                // 新增
                SysConfig newConfig = new SysConfig();
                newConfig.setConfigKey(configKey);
                newConfig.setConfigValue(configValue);
                newConfig.setConfigType(isSystemType != null ? isSystemType : "N");
                newConfig.setRemark(remark);
                return save(newConfig);
            }
        } catch (Exception e) {
            log.error("保存或更新配置失败，configKey: {}", configKey, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(String configKey) {
        try {
            QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", configKey);
            return remove(queryWrapper);
        } catch (Exception e) {
            log.error("删除配置失败，configKey: {}", configKey, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getSiteInfo() {
        Map<String, Object> siteInfo = new HashMap<>();
        
        // 获取网站基本信息
        siteInfo.put("siteName", getConfigValue("site.name", "题库管理系统"));
        siteInfo.put("siteUrl", getConfigValue("site.url", "http://localhost:8080"));
        siteInfo.put("siteLogo", getConfigValue("site.logo", "/logo.png"));
        siteInfo.put("siteDescription", getConfigValue("site.description", "在线刷题学习平台"));
        siteInfo.put("adminEmail", getConfigValue("admin.email", "admin@example.com"));
        siteInfo.put("systemVersion", getConfigValue("system.version", "1.0.0"));
        
        return siteInfo;
    }

    /**
     * 根据配置类型转换配置值
     *
     * @param configValue 配置值
     * @param configType  配置类型
     * @return 转换后的值
     */
    private Object convertConfigValue(String configValue, String configType) {
        if (!StringUtils.hasText(configValue)) {
            return null;
        }
        
        switch (configType) {
            case "number":
                try {
                    return Integer.parseInt(configValue);
                } catch (NumberFormatException e) {
                    return configValue;
                }
            case "boolean":
                return Boolean.parseBoolean(configValue);
            case "json":
                // 这里可以根据需要解析JSON
                return configValue;
            default:
                return configValue;
        }
    }
}