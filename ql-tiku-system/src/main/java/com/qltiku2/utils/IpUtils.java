package com.qltiku2.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

/**
 * IP地址获取工具类
 * 用于在反向代理环境下正确获取客户端真实IP地址
 */
public class IpUtils {
    
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    
    /**
     * 获取客户端真实IP地址
     * 支持多层代理环境，按优先级检查各种代理头
     * 
     * @param request HTTP请求对象
     * @return 客户端真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = null;
        
        // 1. 检查 X-Forwarded-For 头（最常用的代理头）
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // X-Forwarded-For 可能包含多个IP，格式：client, proxy1, proxy2
            // 第一个IP是真实客户端IP
            StringTokenizer tokenizer = new StringTokenizer(ip, ",");
            if (tokenizer.hasMoreTokens()) {
                ip = tokenizer.nextToken().trim();
                if (isValidIp(ip)) {
                    return processIp(ip, request);
                }
            }
        }
        
        // 2. 检查 X-Real-IP 头（Nginx常用）
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return processIp(ip, request);
        }
        
        // 3. 检查 X-Original-Forwarded-For 头
        ip = request.getHeader("X-Original-Forwarded-For");
        if (isValidIp(ip)) {
            return processIp(ip, request);
        }
        
        // 4. 检查 Proxy-Client-IP 头（Apache服务器代理）
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return processIp(ip, request);
        }
        
        // 5. 检查 WL-Proxy-Client-IP 头（WebLogic服务器代理）
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return processIp(ip, request);
        }
        
        // 6. 检查 HTTP_CLIENT_IP 头
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return processIp(ip, request);
        }
        
        // 7. 检查 HTTP_X_FORWARDED_FOR 头
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return processIp(ip, request);
        }
        
        // 8. 最后使用 getRemoteAddr() 方法获取
        ip = request.getRemoteAddr();
        
        return processIp(ip, request);
    }
    
    /**
     * 获取客户端真实IP地址（包含内网IP）
     * 在某些场景下，内网IP也可能是有效的客户端IP
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        return getIpAddr(request);
    }
    
    /**
     * 获取客户端真实IP地址（生产环境专用）
     * 支持反向代理环境，优先获取真实客户端IP
     * 
     * @param request HTTP请求对象
     * @return 客户端真实IP地址
     */
    public static String getRealIpAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = null;
        
        // 按优先级检查各种代理头
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "X-Original-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // 处理多个IP的情况
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return processIp(ip, request);
            }
        }
        
        // 最后使用 getRemoteAddr()
        ip = request.getRemoteAddr();
        
        return processIp(ip, request);
    }
    
    /**
     * 检查IP地址是否有效（用于生产环境）
     * 
     * @param ip IP地址字符串
     * @return 如果IP有效返回true，否则返回false
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        return true;
    }
    
    /**
     * 处理IP地址（生产环境逻辑）
     * 
     * @param ip 原始IP地址
     * @param request HTTP请求对象
     * @return 处理后的IP地址
     */
    private static String processIp(String ip, HttpServletRequest request) {
        if (ip == null) {
            return UNKNOWN;
        }
        
        // 如果是IPv6的本地回环地址，转换为IPv4
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IPV4;
        }
        
        return ip;
    }
    

    
    /**
     * 检查是否为内网IP地址
     * 
     * @param ip IP地址
     * @return 如果是内网IP返回true，否则返回false
     */
    private static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.0.0.0 - 10.255.255.255
            if (first == 10) {
                return true;
            }
            
            // 172.16.0.0 - 172.31.255.255
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            
            // 192.168.0.0 - 192.168.255.255
            if (first == 192 && second == 168) {
                return true;
            }
            
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 判断是否为本地IP地址
     * 
     * @param ipAddress IP地址
     * @return 是否为本地IP
     */
    public static boolean isLocalIp(String ipAddress) {
        if (ipAddress == null) {
            return true;
        }
        
        // 常见的本地IP地址模式
        return ipAddress.equals("127.0.0.1") ||
               ipAddress.equals("localhost") ||
               ipAddress.equals("0:0:0:0:0:0:0:1") ||
               ipAddress.equals("::1") ||
               isInternalIp(ipAddress);
    }
}