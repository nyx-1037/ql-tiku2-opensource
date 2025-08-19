package com.qltiku2.service;

import com.qltiku2.entity.CdnPrefix;
import com.qltiku2.mapper.CdnPrefixMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDN前缀配置服务类
 * 提供CDN前缀的业务逻辑处理
 * 
 * @author system
 */
@Service
public class CdnPrefixService {
    
    @Autowired
    private CdnPrefixMapper cdnPrefixMapper;
    
    /**
     * 获取所有启用的CDN前缀
     * @return CDN前缀列表
     */
    public List<CdnPrefix> getAllActivePrefixes() {
        return cdnPrefixMapper.selectAllActive();
    }
    
    /**
     * 获取所有CDN前缀
     * @return CDN前缀列表
     */
    public List<CdnPrefix> getAllPrefixes() {
        return cdnPrefixMapper.selectAll();
    }
    
    /**
     * 获取默认CDN前缀
     * @return 默认CDN前缀
     */
    public CdnPrefix getDefaultPrefix() {
        return cdnPrefixMapper.selectDefault();
    }
    
    /**
     * 根据ID查询CDN前缀
     * @param id CDN前缀ID
     * @return CDN前缀
     */
    public CdnPrefix getPrefixById(Long id) {
        return cdnPrefixMapper.selectById(id);
    }
    
    /**
     * 添加CDN前缀
     * @param name 前缀名称
     * @param prefix 前缀URL
     * @param description 描述
     * @param isDefault 是否为默认
     * @param isActive 是否启用
     * @return 添加的CDN前缀
     */
    @Transactional
    public CdnPrefix addPrefix(String name, String prefix, String description, 
                              Boolean isDefault, Boolean isActive) {
        // 检查名称是否已存在
        if (cdnPrefixMapper.countByName(name, 0L) > 0) {
            throw new RuntimeException("前缀名称已存在");
        }
        
        // 检查前缀URL是否已存在
        if (cdnPrefixMapper.countByPrefix(prefix, 0L) > 0) {
            throw new RuntimeException("前缀URL已存在");
        }
        
        // 如果设置为默认，先清除其他默认标记
        if (isDefault != null && isDefault) {
            cdnPrefixMapper.clearAllDefault();
        }
        
        // 创建CDN前缀对象
        CdnPrefix cdnPrefix = new CdnPrefix(name, prefix, description, isDefault, isActive);
        
        // 保存到数据库
        int result = cdnPrefixMapper.insert(cdnPrefix);
        if (result > 0) {
            return cdnPrefix;
        } else {
            throw new RuntimeException("CDN前缀保存失败");
        }
    }
    
    /**
     * 更新CDN前缀
     * @param id ID
     * @param name 前缀名称
     * @param prefix 前缀URL
     * @param description 描述
     * @param isDefault 是否为默认
     * @param isActive 是否启用
     * @return 更新是否成功
     */
    @Transactional
    public boolean updatePrefix(Long id, String name, String prefix, String description, 
                               Boolean isDefault, Boolean isActive) {
        // 检查CDN前缀是否存在
        CdnPrefix existingPrefix = cdnPrefixMapper.selectById(id);
        if (existingPrefix == null) {
            throw new RuntimeException("CDN前缀不存在");
        }
        
        // 检查名称是否已被其他记录使用
        if (cdnPrefixMapper.countByName(name, id) > 0) {
            throw new RuntimeException("前缀名称已存在");
        }
        
        // 检查前缀URL是否已被其他记录使用
        if (cdnPrefixMapper.countByPrefix(prefix, id) > 0) {
            throw new RuntimeException("前缀URL已存在");
        }
        
        // 如果设置为默认，先清除其他默认标记
        if (isDefault != null && isDefault) {
            cdnPrefixMapper.clearAllDefault();
        }
        
        // 更新CDN前缀
        existingPrefix.setName(name);
        existingPrefix.setPrefix(prefix);
        existingPrefix.setDescription(description);
        existingPrefix.setIsDefault(isDefault);
        existingPrefix.setIsActive(isActive);
        existingPrefix.setUpdateTime(LocalDateTime.now());
        
        int result = cdnPrefixMapper.update(existingPrefix);
        return result > 0;
    }
    
    /**
     * 设置默认CDN前缀
     * @param id CDN前缀ID
     * @return 设置是否成功
     */
    @Transactional
    public boolean setDefaultPrefix(Long id) {
        // 检查CDN前缀是否存在
        CdnPrefix cdnPrefix = cdnPrefixMapper.selectById(id);
        if (cdnPrefix == null) {
            throw new RuntimeException("CDN前缀不存在");
        }
        
        // 先清除所有默认标记
        cdnPrefixMapper.clearAllDefault();
        
        // 设置指定前缀为默认
        int result = cdnPrefixMapper.setDefault(id);
        return result > 0;
    }
    
    /**
     * 删除CDN前缀
     * @param id CDN前缀ID
     * @return 删除是否成功
     */
    @Transactional
    public boolean deletePrefix(Long id) {
        // 检查CDN前缀是否存在
        CdnPrefix cdnPrefix = cdnPrefixMapper.selectById(id);
        if (cdnPrefix == null) {
            return false;
        }
        
        // 如果是默认前缀，不允许删除
        if (cdnPrefix.getIsDefault() != null && cdnPrefix.getIsDefault()) {
            throw new RuntimeException("不能删除默认CDN前缀");
        }
        
        int result = cdnPrefixMapper.deleteById(id);
        return result > 0;
    }
    
    /**
     * 初始化默认CDN前缀（如果不存在）
     */
    @Transactional
    public void initializeDefaultPrefix() {
        // 检查是否已有默认前缀
        CdnPrefix defaultPrefix = cdnPrefixMapper.selectDefault();
        if (defaultPrefix == null) {
            // 创建默认CDN前缀
            CdnPrefix cdnPrefix = new CdnPrefix(
                "默认CDN",
                "https://cdn.example.cn",
                "系统默认CDN前缀",
                true,
                true
            );
            
            cdnPrefixMapper.insert(cdnPrefix);
        }
    }
}