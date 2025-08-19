package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.entity.SysConfig;
import com.qltiku2.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private SysConfigMapper sysConfigMapper;
    
    @GetMapping("/auth-check")
    @PreAuthorize("permitAll()")
    public Result<Map<String, Object>> checkAuth(HttpServletRequest request) {
        Map<String, Object> authInfo = new HashMap<>();
        
        // 获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        authInfo.put("authenticated", authentication != null && authentication.isAuthenticated());
        authInfo.put("principal", authentication != null ? authentication.getPrincipal() : null);
        authInfo.put("authorities", authentication != null ? authentication.getAuthorities() : null);
        
        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        authInfo.put("authorizationHeader", authHeader);
        
        // 获取所有请求头
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> 
            headers.put(name, request.getHeader(name))
        );
        authInfo.put("allHeaders", headers);
        
        // 获取请求信息
        authInfo.put("requestURI", request.getRequestURI());
        authInfo.put("method", request.getMethod());
        authInfo.put("remoteAddr", request.getRemoteAddr());
        
        return Result.success(authInfo);
    }
    
    @GetMapping(value = "/stream-auth-check", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("permitAll()")
    public Flux<String> checkStreamAuth(HttpServletRequest request) {
        Map<String, Object> authInfo = new HashMap<>();
        
        // 获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        authInfo.put("authenticated", authentication != null && authentication.isAuthenticated());
        authInfo.put("principal", authentication != null ? authentication.getPrincipal() : null);
        authInfo.put("authorities", authentication != null ? authentication.getAuthorities() : null);
        
        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        authInfo.put("authorizationHeader", authHeader);
        
        // 获取所有请求头
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> 
            headers.put(name, request.getHeader(name))
        );
        authInfo.put("allHeaders", headers);
        
        return Flux.just("data: " + authInfo.toString() + "\n\n", "data: [DONE]\n\n");
    }
    
    @GetMapping("/db-check")
    @PreAuthorize("permitAll()")
    public Map<String, Object> checkDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询所有配置
            List<SysConfig> allConfigs = sysConfigMapper.selectList(null);
            result.put("allConfigsCount", allConfigs.size());
            result.put("allConfigs", allConfigs);
            
            // 查询系统配置
            List<SysConfig> systemConfigs = sysConfigMapper.getSystemConfigs();
            result.put("systemConfigsCount", systemConfigs.size());
            result.put("systemConfigs", systemConfigs);
            
            result.put("status", "success");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
}