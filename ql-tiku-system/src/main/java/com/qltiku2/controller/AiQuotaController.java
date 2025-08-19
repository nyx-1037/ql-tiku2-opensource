package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.entity.UserAiQuota;
import com.qltiku2.service.AiQuotaService;
import com.qltiku2.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.SysUserMapper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AI配额控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/ai-quota")
public class AiQuotaController {
    
    @Autowired
    private AiQuotaService aiQuotaService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    /**
     * 获取当前用户的AI配额信息
     */
    @GetMapping("/info")
    public Result<UserAiQuota> getQuotaInfo(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserAiQuota quota = aiQuotaService.getUserQuota(userId);
        if (quota == null) {
            aiQuotaService.initializeQuota(userId);
            quota = aiQuotaService.getUserQuota(userId);
        }
        return Result.success(quota);
    }
    
    /**
     * 获取今日剩余AI次数
     */
    @GetMapping("/remaining")
    public Result<Integer> getRemainingQuota(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        int remaining = aiQuotaService.getRemainingDailyQuota(userId);
        return Result.success(remaining);
    }
    
    /**
     * 检查是否有AI使用配额
     */
    @GetMapping("/check")
    public Result<Boolean> checkQuota(@RequestParam String aiType, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean hasQuota = aiQuotaService.hasQuota(userId, aiType);
        return Result.success(hasQuota);
    }
    
    /**
     * 消耗AI配额
     */
    @PostMapping("/consume")
    public Result<String> consumeQuota(@RequestParam String aiType, @RequestParam(defaultValue = "1") int tokens, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean success = aiQuotaService.consumeQuota(userId, aiType, tokens);
        if (success) {
            return Result.success("配额消耗成功");
        } else {
            return Result.error("配额不足或消耗失败");
        }
    }
    
    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 优先使用SecurityContextHolder获取用户信息（适用于Spring Security已认证的情况）
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !(authentication instanceof AnonymousAuthenticationToken)) {
                
                // 获取用户详情
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    UserDetails userDetails = (UserDetails) principal;
                    String username = userDetails.getUsername();
                    
                    // 通过用户名查询用户ID
                    SysUser user = sysUserMapper.selectByUsername(username);
                    if (user != null) {
                        return user.getId();
                    }
                } else if (principal instanceof String && !"anonymousUser".equals(principal)) {
                    // 如果principal是字符串用户名
                    String username = (String) principal;
                    SysUser user = sysUserMapper.selectByUsername(username);
                    if (user != null) {
                        return user.getId();
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理，继续尝试从token获取
        }
        
        // 如果SecurityContext中没有，则尝试从token中获取（兼容旧逻辑）
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                return jwtUtils.getUserIdFromToken(token);
            } catch (Exception e) {
                // 静默处理，继续抛出异常
            }
        }
        
        throw new RuntimeException("无法获取用户信息");
    }
}