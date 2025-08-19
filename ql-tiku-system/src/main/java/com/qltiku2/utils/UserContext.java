package com.qltiku2.utils;

import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 用户上下文工具类
 * 
 * @author qltiku2
 */
@Component
public class UserContext {
    
    private static SysUserMapper sysUserMapper;
    
    @Autowired
    public void setSysUserMapper(SysUserMapper sysUserMapper) {
        UserContext.sysUserMapper = sysUserMapper;
    }
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        System.out.println("=== UserContext.getCurrentUserId 开始 ===");
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Authentication: " + authentication);
            
            if (authentication == null) {
                System.err.println("Authentication 为 null");
                throw new RuntimeException("用户未登录");
            }
            
            System.out.println("Authentication.isAuthenticated(): " + authentication.isAuthenticated());
            System.out.println("Authentication.getPrincipal(): " + authentication.getPrincipal());
            
            if (!authentication.isAuthenticated()) {
                System.err.println("用户未认证");
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            System.out.println("从 Authentication 获取的用户名: " + username);
            
            if ("anonymousUser".equals(username)) {
                System.err.println("匿名用户");
                throw new RuntimeException("用户未登录");
            }
            
            System.out.println("开始查询用户信息...");
            SysUser user = sysUserMapper.selectByUsername(username);
            System.out.println("查询到的用户: " + user);
            
            if (user == null) {
                System.err.println("用户不存在: " + username);
                throw new RuntimeException("用户不存在");
            }
            
            System.out.println("用户ID: " + user.getId());
            System.out.println("=== UserContext.getCurrentUserId 结束 ===");
            return user.getId();
        } catch (Exception e) {
            System.err.println("获取当前用户ID失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取当前用户ID失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    public static SysUser getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            if ("anonymousUser".equals(username)) {
                throw new RuntimeException("用户未登录");
            }
            
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            return user;
        } catch (Exception e) {
            throw new RuntimeException("获取当前用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            if ("anonymousUser".equals(username)) {
                throw new RuntimeException("用户未登录");
            }
            
            return username;
        } catch (Exception e) {
            throw new RuntimeException("获取当前用户名失败: " + e.getMessage());
        }
    }
}