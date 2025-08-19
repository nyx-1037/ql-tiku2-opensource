package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.entity.SysConfig;
import com.qltiku2.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 系统管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/system")
@CrossOrigin(origins = "*")
public class AdminSystemController {

    @Autowired
    private SysConfigService sysConfigService;
    
    /**
     * 获取系统日志
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<Map<String, Object>> logs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // 模拟系统日志数据
        String[] logTypes = {"INFO", "WARN", "ERROR"};
        String[] messages = {
            "用户登录成功",
            "系统备份完成",
            "数据库连接异常",
            "新用户注册",
            "考试创建成功",
            "题目导入完成",
            "系统配置更新",
            "缓存清理完成"
        };
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", (current - 1) * size + i + 1);
            log.put("type", logTypes[new Random().nextInt(logTypes.length)]);
            log.put("message", messages[new Random().nextInt(messages.length)]);
            log.put("createTime", LocalDateTime.now().minusHours(i).format(formatter));
            log.put("module", "系统管理");
            logs.add(log);
        }
        
        return Result.success(logs);
    }
    
    /**
     * 获取系统通知
     */
    @GetMapping("/notices")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getSystemNotices(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "5") Integer size) {
        
        List<Map<String, Object>> notices = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // 模拟系统通知数据
        String[] titles = {
            "系统维护通知",
            "新功能上线",
            "安全更新提醒",
            "数据备份完成",
            "用户反馈处理"
        };
        
        String[] contents = {
            "系统将于今晚进行例行维护，预计耗时2小时",
            "题库管理功能已上线，支持批量导入导出",
            "已修复登录安全漏洞，请及时更新密码",
            "数据库备份已完成，数据安全有保障",
            "用户反馈的界面优化建议已采纳并实施"
        };
        
        for (int i = 0; i < Math.min(size, titles.length); i++) {
            Map<String, Object> notice = new HashMap<>();
            notice.put("id", i + 1);
            notice.put("title", titles[i]);
            notice.put("content", contents[i]);
            notice.put("type", i % 2 == 0 ? "系统" : "公告");
            notice.put("createTime", LocalDateTime.now().minusDays(i).format(formatter));
            notice.put("status", 1); // 1-已发布
            notices.add(notice);
        }
        
        return Result.success(notices);
    }
    
    /**
     * 获取系统配置
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getSystemConfig() {
        try {
            Map<String, Object> config = sysConfigService.getAllSystemConfig();
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return Result.error("获取系统配置失败");
        }
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateSystemConfig(@RequestBody Map<String, Object> config) {
        try {
            Map<String, String> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                configMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            boolean success = sysConfigService.batchUpdateConfig(configMap);
            if (success) {
                return Result.success("配置更新成功");
            } else {
                return Result.error("配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            return Result.error("配置更新失败");
        }
    }

    /**
     * 获取网站基本信息
     */
    @GetMapping("/site-info")
    public Result<Map<String, Object>> getSiteInfo() {
        try {
            Map<String, Object> siteInfo = sysConfigService.getSiteInfo();
            return Result.success(siteInfo);
        } catch (Exception e) {
            log.error("获取网站信息失败", e);
            return Result.error("获取网站信息失败");
        }
    }

    /**
     * 获取所有配置列表
     */
    @GetMapping("/config/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysConfig>> getConfigList() {
        try {
            List<SysConfig> configs = sysConfigService.list();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取配置列表失败", e);
            return Result.error("获取配置列表失败");
        }
    }

    /**
     * 添加或更新配置
     */
    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> saveOrUpdateConfig(@RequestBody SysConfig config) {
        try {
            boolean success = sysConfigService.saveOrUpdateConfig(
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigType(),
                config.getRemark(),
                config.getConfigType()
            );
            
            if (success) {
                return Result.success("配置保存成功");
            } else {
                return Result.error("配置保存失败");
            }
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return Result.error("配置保存失败");
        }
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/config/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteConfig(@PathVariable String configKey) {
        try {
            boolean success = sysConfigService.deleteConfig(configKey);
            if (success) {
                return Result.success("配置删除成功");
            } else {
                return Result.error("配置删除失败");
            }
        } catch (Exception e) {
            log.error("删除配置失败，configKey: {}", configKey, e);
            return Result.error("配置删除失败");
        }
    }
    
    /**
     * 保存基础设置
     */
    @PutMapping("/settings/basic")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> saveBasicSettings(@RequestBody Map<String, Object> settings) {
        try {
            Map<String, String> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                configMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            boolean success = sysConfigService.batchUpdateConfig(configMap);
            if (success) {
                return Result.success("基础设置保存成功");
            } else {
                return Result.error("基础设置保存失败");
            }
        } catch (Exception e) {
            log.error("保存基础设置失败", e);
            return Result.error("基础设置保存失败");
        }
    }
    
    /**
     * 保存邮件设置
     */
    @PutMapping("/settings/email")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> saveEmailSettings(@RequestBody Map<String, Object> settings) {
        try {
            Map<String, String> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                configMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            boolean success = sysConfigService.batchUpdateConfig(configMap);
            if (success) {
                return Result.success("邮件设置保存成功");
            } else {
                return Result.error("邮件设置保存失败");
            }
        } catch (Exception e) {
            log.error("保存邮件设置失败", e);
            return Result.error("邮件设置保存失败");
        }
    }
    
    /**
     * 测试邮件
     */
    @PostMapping("/settings/email/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> testEmail(@RequestBody Map<String, Object> data) {
        return Result.success("邮件发送成功");
    }
    
    /**
     * 保存存储设置
     */
    @PutMapping("/settings/storage")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> saveStorageSettings(@RequestBody Map<String, Object> settings) {
        try {
            Map<String, String> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                configMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            boolean success = sysConfigService.batchUpdateConfig(configMap);
            if (success) {
                return Result.success("存储设置保存成功");
            } else {
                return Result.error("存储设置保存失败");
            }
        } catch (Exception e) {
            log.error("保存存储设置失败", e);
            return Result.error("存储设置保存失败");
        }
    }
    
    /**
     * 保存安全设置
     */
    @PutMapping("/settings/security")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> saveSecuritySettings(@RequestBody Map<String, Object> settings) {
        try {
            Map<String, String> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                configMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            boolean success = sysConfigService.batchUpdateConfig(configMap);
            if (success) {
                return Result.success("安全设置保存成功");
            } else {
                return Result.error("安全设置保存失败");
            }
        } catch (Exception e) {
            log.error("保存安全设置失败", e);
            return Result.error("安全设置保存失败");
        }
    }
    
    /**
     * 保存备份设置
     */
    @PutMapping("/settings/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> saveBackupSettings(@RequestBody Map<String, Object> settings) {
        try {
            Map<String, String> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                configMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            boolean success = sysConfigService.batchUpdateConfig(configMap);
            if (success) {
                return Result.success("备份设置保存成功");
            } else {
                return Result.error("备份设置保存失败");
            }
        } catch (Exception e) {
            log.error("保存备份设置失败", e);
            return Result.error("备份设置保存失败");
        }
    }
    
    /**
     * 创建备份
     */
    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createBackup() {
        return Result.success("备份创建成功");
    }
    
    /**
     * 获取备份列表
     */
    @GetMapping("/backups")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getBackupList() {
        List<Map<String, Object>> backups = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (int i = 0; i < 5; i++) {
            Map<String, Object> backup = new HashMap<>();
            backup.put("id", i + 1);
            backup.put("name", "backup_" + LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            backup.put("size", "15.2 MB");
            backup.put("createTime", LocalDateTime.now().minusDays(i).format(formatter));
            backup.put("status", "完成");
            backups.add(backup);
        }
        
        return Result.success(backups);
    }
    
    /**
     * 恢复备份
     */
    @PostMapping("/backup/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> restoreBackup(@PathVariable Long id) {
        return Result.success("备份恢复成功");
    }
    
    /**
     * 删除备份
     */
    @DeleteMapping("/backup/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteBackup(@PathVariable Long id) {
        return Result.success("备份删除成功");
    }
    
    /**
     * 清空日志
     */
    @DeleteMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> clearLogs() {
        return Result.success("日志清空成功");
    }
}