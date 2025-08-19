package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.entity.RegistrationCode;
import com.qltiku2.service.RegistrationCodeService;
import com.qltiku2.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 注册码控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/registration-code")
public class RegistrationCodeController {
    
    @Autowired
    private RegistrationCodeService registrationCodeService;
    
    /**
     * 验证注册码
     */
    @PostMapping("/validate")
    public Result<Boolean> validateCode(@RequestParam String code) {
        boolean isValid = registrationCodeService.validateCode(code);
        return Result.success(isValid);
    }
    
    /**
     * 使用注册码
     */
    @PostMapping("/use")
    public Result<String> useCode(@RequestParam String code) {
        Long userId = UserContext.getCurrentUserId();
        String ipAddress = getClientIpAddress();
        
        boolean success = registrationCodeService.useCode(code, userId, ipAddress);
        if (success) {
            return Result.success("注册码使用成功");
        } else {
            return Result.error("注册码无效或已过期");
        }
    }
    
    /**
     * 生成注册码（管理员功能）
     */
    @PostMapping("/generate")
    public Result<RegistrationCode> generateCode(@RequestParam Integer maxUses, @RequestParam Integer validDays) {
        RegistrationCode code = registrationCodeService.generateCode(maxUses, validDays);
        return Result.success(code);
    }
    
    /**
     * 获取注册码列表（管理员功能）
     */
    @GetMapping("/list")
    public Result<List<RegistrationCode>> getCodeList() {
        List<RegistrationCode> codes = registrationCodeService.getCodeList();
        return Result.success(codes);
    }
    
    /**
     * 禁用注册码（管理员功能）
     */
    @PostMapping("/disable/{id}")
    public Result<String> disableCode(@PathVariable Long id) {
        boolean success = registrationCodeService.disableCode(id);
        if (success) {
            return Result.success("注册码已禁用");
        } else {
            return Result.error("禁用失败");
        }
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0];
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}