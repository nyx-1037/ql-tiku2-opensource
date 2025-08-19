package com.qltiku2.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qltiku2.entity.OperationLog;
import com.qltiku2.entity.SysUser;
import com.qltiku2.service.OperationLogService;
import com.qltiku2.service.IpLocationService;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志AOP切面
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private IpLocationService ipLocationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    /**
     * 定义切点：拦截所有controller包下的方法，但排除OperationLogController
     */
    @Pointcut("execution(* com.qltiku2.controller..*.*(..)) && !execution(* com.qltiku2.controller.OperationLogController.*(..))")
    public void controllerPointcut() {}
    
    /**
     * 环绕通知：记录操作日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        
        // 创建操作日志对象
        OperationLog operationLog = new OperationLog();
        operationLog.setCreateTime(LocalDateTime.now());
        
        // 设置基本信息
        operationLog.setRequestMethod(request.getMethod());
        operationLog.setRequestUrl(request.getRequestURL().toString());
        
        // 获取真实IP地址（支持反向代理）
    String ipAddress = IpUtils.getRealIpAddress(request);
    operationLog.setIpAddress(ipAddress);
        
        // 异步获取IP地理位置信息（避免阻塞主流程）
        try {
            String location = ipLocationService.getSimpleLocationByIp(ipAddress);
            operationLog.setLocation(location);
        } catch (Exception e) {
            log.warn("获取IP地理位置失败，IP: {}, 错误: {}", ipAddress, e.getMessage());
            operationLog.setLocation("位置获取失败");
        }
        
        operationLog.setUserAgent(request.getHeader("User-Agent"));
        operationLog.setOperationMethod(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        
        // 获取请求参数
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 过滤掉HttpServletRequest、HttpServletResponse等对象
                String params = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest) 
                                && !(arg instanceof HttpServletResponse)
                                && arg != null)
                    .map(arg -> {
                        try {
                            return objectMapper.writeValueAsString(arg);
                        } catch (Exception e) {
                            return arg.toString();
                        }
                    })
                    .collect(Collectors.joining(", "));
                operationLog.setRequestParams(params.length() > 2000 ? params.substring(0, 2000) + "..." : params);
            }
        } catch (Exception e) {
            log.warn("获取请求参数失败", e);
            operationLog.setRequestParams("参数解析失败");
        }
        
        // 获取用户信息（从Spring Security上下文中获取）
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                
                String username = authentication.getName();
                operationLog.setUsername(username);
                
                // 通过用户名查询用户ID
                try {
                    SysUser user = sysUserMapper.selectByUsername(username);
                    if (user != null) {
                        operationLog.setUserId(user.getId());
                    }
                } catch (Exception e) {
                    log.warn("通过用户名查询用户ID失败: {}", username, e);
                }
            } else {
                // 对于公开接口，标记为系统
                operationLog.setUsername("系统");
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败", e);
            operationLog.setUsername("未知用户");
        }
        
        Object result = null;
        Exception thrownException = null;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 设置响应数据（限制长度）
            if (result != null) {
                try {
                    String responseData = objectMapper.writeValueAsString(result);
                    operationLog.setResponseData(responseData.length() > 2000 ? responseData.substring(0, 2000) + "..." : responseData);
                } catch (Exception e) {
                    operationLog.setResponseData("响应数据序列化失败");
                }
            }
            
        } catch (Exception e) {
            // 记录异常信息
            thrownException = e;
            operationLog.setErrorMessage(e.getMessage());
            log.error("控制器方法执行异常: {}", joinPoint.getSignature().getName(), e);
        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            operationLog.setExecutionTime(endTime - startTime);
            
            // 设置状态码 - 优先从HttpServletResponse获取实际状态码
            if (response != null) {
                int statusCode = response.getStatus();
                operationLog.setStatusCode(statusCode);
                
                // 如果状态码表示错误但没有异常信息，记录状态码相关信息
                if (statusCode >= 400 && operationLog.getErrorMessage() == null) {
                    operationLog.setErrorMessage("HTTP状态码: " + statusCode);
                }
            } else if (thrownException != null) {
                // 如果没有response但有异常，设置为500
                operationLog.setStatusCode(500);
            } else {
                // 默认200
                operationLog.setStatusCode(200);
            }
            
            // 异步保存日志
            operationLogService.saveLogAsync(operationLog);
        }
        
        // 如果有异常，重新抛出
        if (thrownException != null) {
            throw thrownException;
        }
        
        return result;
    }
}