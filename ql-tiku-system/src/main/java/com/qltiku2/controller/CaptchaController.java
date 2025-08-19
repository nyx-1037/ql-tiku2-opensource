package com.qltiku2.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.qltiku2.common.Result;
import com.qltiku2.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 * 
 * @author qltiku2
 */
@Slf4j
@RestController
@RequestMapping("/captcha")
@CrossOrigin(origins = "*")
public class CaptchaController {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SysConfigService sysConfigService;
    
    /**
     * 生成验证码
     */
    @GetMapping("/generate")
    public Result<Map<String, Object>> generateCaptcha() {
        try {
            // 检查是否启用验证码
            boolean captchaEnabled = Boolean.parseBoolean(
                sysConfigService.getConfigValue("sys.captcha.enable", "false")
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("enabled", captchaEnabled);
            
            if (!captchaEnabled) {
                log.info("验证码功能未启用");
                return Result.success(result);
            }
            
            // 生成验证码ID
            String captchaId = IdUtil.simpleUUID();
            
            // 创建线性干扰验证码
            LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
            
            // 获取验证码文本
            String code = lineCaptcha.getCode();
            
            // 获取验证码图片的Base64编码
            String imageBase64 = lineCaptcha.getImageBase64Data();
            
            // 将验证码存储到Redis，有效期5分钟
            String redisKey = "captcha:" + captchaId;
            redisTemplate.opsForValue().set(redisKey, code.toLowerCase(), 5, TimeUnit.MINUTES);
            
            result.put("captchaId", captchaId);
            result.put("captchaImage", imageBase64);
            
            log.info("生成验证码成功，ID: {}, 验证码: {}", captchaId, code);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("生成验证码失败", e);
            return Result.error("生成验证码失败");
        }
    }
    
    /**
     * 验证验证码
     */
    @PostMapping("/verify")
    public Result<Boolean> verifyCaptcha(@RequestParam String captchaId, @RequestParam String captchaCode) {
        try {
            // 检查是否启用验证码
            boolean captchaEnabled = Boolean.parseBoolean(
                sysConfigService.getConfigValue("sys.captcha.enable", "false")
            );
            
            if (!captchaEnabled) {
                log.info("验证码功能未启用，跳过验证");
                return Result.success(true);
            }
            
            if (captchaId == null || captchaCode == null) {
                log.warn("验证码ID或验证码为空");
                return Result.success(false);
            }
            
            String redisKey = "captcha:" + captchaId;
            String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (storedCode == null) {
                log.warn("验证码已过期或不存在，ID: {}", captchaId);
                return Result.success(false);
            }
            
            boolean isValid = storedCode.equalsIgnoreCase(captchaCode.trim());
            
            if (isValid) {
                // 验证成功后删除验证码
                redisTemplate.delete(redisKey);
                log.info("验证码验证成功，ID: {}", captchaId);
            } else {
                log.warn("验证码验证失败，ID: {}, 输入: {}, 期望: {}", captchaId, captchaCode, storedCode);
            }
            
            return Result.success(isValid);
            
        } catch (Exception e) {
            log.error("验证验证码失败", e);
            return Result.error("验证验证码失败");
        }
    }
    
    /**
     * 获取验证码配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getCaptchaConfig() {
        try {
            boolean captchaEnabled = Boolean.parseBoolean(
                sysConfigService.getConfigValue("sys.captcha.enable", "false")
            );
            
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", captchaEnabled);
            
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取验证码配置失败", e);
            return Result.error("获取验证码配置失败");
        }
    }
}