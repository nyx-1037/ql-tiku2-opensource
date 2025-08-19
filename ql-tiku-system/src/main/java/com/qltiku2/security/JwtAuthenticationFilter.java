package com.qltiku2.security;

import com.qltiku2.service.UserDetailsServiceImpl;
import com.qltiku2.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 * 处理每个请求的JWT令牌验证
 * 
 * @author qltiku2
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        logger.info("=== JWT过滤器开始处理请求 ===");
        logger.info("请求URI: " + requestURI);
        logger.info("请求方法: " + method);
        logger.info("请求来源: " + request.getRemoteAddr());
        logger.debug("JWT过滤器处理请求: " + requestURI);
        
        try {
            // 获取请求头中的Authorization
            String authHeader = request.getHeader(jwtUtils.getHeader());
            logger.debug("Authorization头: " + authHeader);
            
            String username = null;
            String token = null;
            
            // 验证token格式并提取
            if (jwtUtils.validateTokenFormat(authHeader)) {
                token = jwtUtils.getTokenFromHeader(authHeader);
                logger.debug("提取的token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
                try {
                    username = jwtUtils.getUsernameFromToken(token);
                    logger.debug("从token中解析的用户名: " + username);
                } catch (Exception e) {
                    logger.warn("JWT令牌解析失败: " + e.getMessage());
                }
            } else {
                logger.debug("Token格式验证失败，authHeader: " + authHeader);
            }
            
            // 如果用户名不为空且当前没有认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.debug("开始验证用户: " + username);
                
                // 加载用户详情
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                logger.debug("加载的用户详情: " + userDetails.getUsername() + ", 权限: " + userDetails.getAuthorities());
                
                // 验证token
                if (jwtUtils.validateToken(token, userDetails.getUsername())) {
                    logger.debug("Token验证成功，设置认证信息");
                    
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    // 设置详情
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置到安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("认证信息已设置到SecurityContext");
                } else {
                    logger.warn("Token验证失败，用户: " + username);
                }
            } else {
                if (username == null) {
                    logger.debug("未从token中解析到用户名");
                } else {
                    logger.debug("SecurityContext中已存在认证信息");
                }
            }
        } catch (Exception e) {
            logger.error("JWT认证过滤器异常: " + e.getMessage(), e);
        }
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
}