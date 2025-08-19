package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.entity.CdnPrefix;
import com.qltiku2.service.CdnPrefixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDN前缀管理控制器
 * 提供CDN前缀的增删改查等管理功能
 * 
 * @author system
 */
@RestController
@RequestMapping("/admin/cdn-prefix")
public class AdminCdnPrefixController {
    
    @Autowired
    private CdnPrefixService cdnPrefixService;
    
    /**
     * 获取所有CDN前缀
     * @return CDN前缀列表
     */
    @GetMapping
    public Result<List<CdnPrefix>> getAllPrefixes() {
        try {
            List<CdnPrefix> prefixes = cdnPrefixService.getAllPrefixes();
            return Result.success(prefixes);
        } catch (Exception e) {
            return Result.error("获取CDN前缀列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有启用的CDN前缀
     * @return 启用的CDN前缀列表
     */
    @GetMapping("/active")
    public Result<List<CdnPrefix>> getActivePrefixes() {
        try {
            List<CdnPrefix> prefixes = cdnPrefixService.getAllActivePrefixes();
            return Result.success(prefixes);
        } catch (Exception e) {
            return Result.error("获取启用CDN前缀列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取默认CDN前缀
     * @return 默认CDN前缀
     */
    @GetMapping("/default")
    public Result<CdnPrefix> getDefaultPrefix() {
        try {
            CdnPrefix prefix = cdnPrefixService.getDefaultPrefix();
            if (prefix == null) {
                return Result.error("未配置默认CDN前缀");
            }
            return Result.success(prefix);
        } catch (Exception e) {
            return Result.error("获取默认CDN前缀失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取CDN前缀
     * @param id CDN前缀ID
     * @return CDN前缀详情
     */
    @GetMapping("/{id}")
    public Result<CdnPrefix> getPrefixById(@PathVariable Long id) {
        try {
            CdnPrefix prefix = cdnPrefixService.getPrefixById(id);
            if (prefix == null) {
                return Result.error("CDN前缀不存在");
            }
            return Result.success(prefix);
        } catch (Exception e) {
            return Result.error("获取CDN前缀详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 添加CDN前缀
     * @param requestBody 请求体
     * @return 添加结果
     */
    @PostMapping
    public Result<CdnPrefix> addPrefix(@RequestBody Map<String, Object> requestBody) {
        try {
            String name = (String) requestBody.get("name");
            String prefix = (String) requestBody.get("prefix");
            String description = (String) requestBody.get("description");
            Boolean isDefault = (Boolean) requestBody.get("isDefault");
            Boolean isActive = (Boolean) requestBody.get("isActive");
            
            if (name == null || name.trim().isEmpty()) {
                return Result.error("前缀名称不能为空");
            }
            if (prefix == null || prefix.trim().isEmpty()) {
                return Result.error("前缀URL不能为空");
            }
            
            CdnPrefix cdnPrefix = cdnPrefixService.addPrefix(name, prefix, description, isDefault, isActive);
            return Result.success(cdnPrefix);
        } catch (Exception e) {
            return Result.error("添加CDN前缀失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新CDN前缀
     * @param id CDN前缀ID
     * @param requestBody 请求体
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<String> updatePrefix(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        try {
            String name = (String) requestBody.get("name");
            String prefix = (String) requestBody.get("prefix");
            String description = (String) requestBody.get("description");
            Boolean isDefault = (Boolean) requestBody.get("isDefault");
            Boolean isActive = (Boolean) requestBody.get("isActive");
            
            if (name == null || name.trim().isEmpty()) {
                return Result.error("前缀名称不能为空");
            }
            if (prefix == null || prefix.trim().isEmpty()) {
                return Result.error("前缀URL不能为空");
            }
            
            boolean success = cdnPrefixService.updatePrefix(id, name, prefix, description, isDefault, isActive);
            if (success) {
                return Result.success("CDN前缀更新成功");
            } else {
                return Result.error("CDN前缀更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新CDN前缀失败：" + e.getMessage());
        }
    }
    
    /**
     * 设置默认CDN前缀
     * @param id CDN前缀ID
     * @return 设置结果
     */
    @PutMapping("/{id}/set-default")
    public Result<String> setDefaultPrefix(@PathVariable Long id) {
        try {
            boolean success = cdnPrefixService.setDefaultPrefix(id);
            if (success) {
                return Result.success("默认CDN前缀设置成功");
            } else {
                return Result.error("默认CDN前缀设置失败");
            }
        } catch (Exception e) {
            return Result.error("设置默认CDN前缀失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除CDN前缀
     * @param id CDN前缀ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deletePrefix(@PathVariable Long id) {
        try {
            boolean success = cdnPrefixService.deletePrefix(id);
            if (success) {
                return Result.success("CDN前缀删除成功");
            } else {
                return Result.error("CDN前缀删除失败，前缀不存在");
            }
        } catch (Exception e) {
            return Result.error("删除CDN前缀失败：" + e.getMessage());
        }
    }
}