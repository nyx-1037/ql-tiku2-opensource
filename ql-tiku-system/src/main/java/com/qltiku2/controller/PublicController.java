package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开接口控制器
 * 提供无需认证的公开接口
 *
 * @author qltiku2
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private SysConfigService sysConfigService;
    
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取系统基本配置信息
     * 包括系统名称、Logo、备案信息等
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getPublicConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 获取系统基本信息
            config.put("siteName", sysConfigService.getConfigValue("sys.site.name", "在线刷题系统"));
            config.put("siteLogo", sysConfigService.getConfigValue("sys.site.logo", ""));
            config.put("siteDescription", sysConfigService.getConfigValue("sys.site.description", "在线刷题系统"));
            config.put("siteVersion", sysConfigService.getConfigValue("sys.site.version", "1.0.0"));
            
            // 获取备案信息
            config.put("icpBeian", sysConfigService.getConfigValue("sys.site.icp", ""));
            config.put("icpBeianUrl", sysConfigService.getConfigValue("sys.site.icp.url", "http://beian.miit.gov.cn/"));
            config.put("policeBeian", sysConfigService.getConfigValue("sys.site.police", ""));
            config.put("policeBeianUrl", sysConfigService.getConfigValue("sys.site.police.url", ""));
            config.put("policeBeianIcon", sysConfigService.getConfigValue("sys.site.police.icon", ""));
            config.put("copyright", sysConfigService.getConfigValue("sys.site.copyright", ""));
            
            // 获取注册开关
            config.put("allowRegister", "true".equals(sysConfigService.getConfigValue("sys.allow.register", "true")));
            
            // 获取注册码校验开关
            config.put("registrationCodeRequired", "true".equals(sysConfigService.getConfigValue("sys.registration.code.required", "false")));
            
            // 获取维护模式配置
            config.put("maintenanceMode", "true".equals(sysConfigService.getConfigValue("sys.maintenance.mode", "false")));
            config.put("maintenanceMessage", sysConfigService.getConfigValue("sys.maintenance.message", "系统正在维护中，请稍后再试"));
            
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取公开配置失败", e);
            return Result.error("获取配置失败");
        }
    }

    /**
     * 检查是否允许注册
     */
    @GetMapping("/register/check")
    public Result<Boolean> checkRegisterAllowed() {
        try {
            boolean allowRegister = "true".equals(sysConfigService.getConfigValue("sys.allow.register", "true"));
            return Result.success(allowRegister);
        } catch (Exception e) {
            log.error("检查注册开关失败", e);
            return Result.error("检查失败");
        }
    }
    
    /**
     * 获取每日一语
     */
    @GetMapping("/daily-quote")
    public Result<String> getDailyQuote() {
        try {
            // 从配置中获取API地址
            String apiUrl = sysConfigService.getConfigValue("sys.daily.quote.api", "");
            if (apiUrl.isEmpty()) {
                return Result.error("每日一语API未配置");
            }
            
            // 调用外部API
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            if (response != null) {
                Object codeObj = response.get("code");
                if ((codeObj instanceof Integer && (Integer)codeObj == 200) || 
                    (codeObj instanceof String && "200".equals(codeObj.toString()))) {
                    String quote = (String) response.get("data");
                    return Result.success(quote);
                }
            }
            return Result.error("获取每日一语失败");
        } catch (Exception e) {
            log.error("获取每日一语失败", e);
            return Result.error("获取每日一语失败：" + e.getMessage());
        }
    }
}