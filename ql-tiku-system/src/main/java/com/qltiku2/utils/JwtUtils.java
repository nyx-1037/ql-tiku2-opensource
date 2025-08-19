package com.qltiku2.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT工具类
 * 
 * @author qltiku2
 */
@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.header}")
    private String header;
    
    @Value("${jwt.prefix}")
    private String prefix;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Redis中JWT token的key前缀
    private static final String JWT_TOKEN_PREFIX = "jwt:token:";
    // Redis中用户登录状态的key前缀
    private static final String USER_LOGIN_PREFIX = "user:login:";
    // JWT token在Redis中的过期时间（7天，单位：秒）
    private static final long JWT_REDIS_EXPIRATION = 7 * 24 * 60 * 60L;
    
    /**
     * 生成JWT令牌
     */
    public String generateToken(Long userId, String username, Integer userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", userType);
        
        String token = createToken(claims, username);
        
        // 将token存储到Redis中，设置7天过期时间
        String tokenKey = JWT_TOKEN_PREFIX + token;
        String userKey = USER_LOGIN_PREFIX + username;
        
        // 存储token信息
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", userId);
        tokenInfo.put("username", username);
        tokenInfo.put("userType", userType);
        tokenInfo.put("createTime", System.currentTimeMillis());
        
        redisTemplate.opsForValue().set(tokenKey, tokenInfo, JWT_REDIS_EXPIRATION, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(userKey, token, JWT_REDIS_EXPIRATION, TimeUnit.SECONDS);
        
        return token;
    }
    
    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        // 使用7天过期时间
        Date expiryDate = new Date(now.getTime() + JWT_REDIS_EXPIRATION * 1000);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }
    
    /**
     * 从令牌中获取用户类型
     */
    public Integer getUserTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return Integer.valueOf(claims.get("userType").toString());
    }
    
    /**
     * 从令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * 从令牌中获取指定声明
     */
    public <T> T getClaimFromToken(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.resolve(claims);
    }
    
    /**
     * 从令牌中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 检查令牌是否过期
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, String username) {
        try {
            logger.info("=== Token验证开始 ===");
            logger.info("验证用户名: " + username);
            logger.info("Token前20字符: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            
            // 1. 验证JWT token格式和签名
            final String tokenUsername = getUsernameFromToken(token);
            logger.info("Token中的用户名: " + tokenUsername);
            
            if (!username.equals(tokenUsername)) {
                logger.warn("用户名不匹配: 期望=" + username + ", 实际=" + tokenUsername);
                return false;
            }
            
            boolean expired = isTokenExpired(token);
            logger.info("Token是否过期: " + expired);
            if (expired) {
                Date expiration = getExpirationDateFromToken(token);
                logger.warn("Token已过期，过期时间: " + expiration + ", 当前时间: " + new Date());
                return false;
            }
            
            // 2. 验证token是否在Redis中存在
            String tokenKey = JWT_TOKEN_PREFIX + token;
            logger.info("检查Redis中的tokenKey: " + tokenKey);
            Object tokenInfo = redisTemplate.opsForValue().get(tokenKey);
            logger.info("Redis中的token信息: " + tokenInfo);
            if (tokenInfo == null) {
                logger.warn("Token在Redis中不存在");
                return false;
            }
            
            // 3. 验证用户当前登录的token是否匹配
            String userKey = USER_LOGIN_PREFIX + username;
            logger.info("检查Redis中的userKey: " + userKey);
            String currentToken = (String) redisTemplate.opsForValue().get(userKey);
            logger.info("用户当前token前20字符: " + (currentToken != null ? currentToken.substring(0, Math.min(20, currentToken.length())) + "..." : "null"));
            
            boolean tokenMatch = token.equals(currentToken);
            logger.info("Token是否匹配: " + tokenMatch);
            if (!tokenMatch) {
                logger.warn("用户当前token与请求token不匹配");
                return false;
            }
            
            logger.info("=== Token验证成功 ===");
            return true;
        } catch (Exception e) {
            logger.error("Token验证异常: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证令牌格式
     */
    public Boolean validateTokenFormat(String authHeader) {
        return authHeader != null && authHeader.startsWith(prefix + " ");
    }
    
    /**
     * 从请求头中提取令牌
     */
    public String getTokenFromHeader(String authHeader) {
        if (validateTokenFormat(authHeader)) {
            return authHeader.substring(prefix.length() + 1);
        }
        return null;
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * 声明解析器接口
     */
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
    
    // Getter方法
    public String getHeader() {
        return header;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public Long getExpiration() {
        return JWT_REDIS_EXPIRATION;
    }
    
    /**
     * 刷新token
     */
    public String refreshToken(String oldToken) {
        try {
            // 1. 获取旧token信息
            String username = getUsernameFromToken(oldToken);
            Long userId = getUserIdFromToken(oldToken);
            Integer userType = getUserTypeFromToken(oldToken);
            
            // 2. 删除旧token
            invalidateToken(oldToken);
            
            // 3. 生成新token
            return generateToken(userId, username, userType);
        } catch (Exception e) {
            throw new RuntimeException("刷新token失败");
        }
    }
    
    /**
     * 使token失效（注销）
     */
    public void invalidateToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            String tokenKey = JWT_TOKEN_PREFIX + token;
            String userKey = USER_LOGIN_PREFIX + username;
            
            // 从Redis中删除token信息
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(userKey);
        } catch (Exception e) {
            // 忽略异常，可能token已经过期或不存在
        }
    }
    
    /**
     * 检查token是否在Redis中存在
     */
    public Boolean isTokenExistsInRedis(String token) {
        try {
            String tokenKey = JWT_TOKEN_PREFIX + token;
            return redisTemplate.hasKey(tokenKey);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取用户当前有效的token
     */
    public String getCurrentTokenByUsername(String username) {
        try {
            String userKey = USER_LOGIN_PREFIX + username;
            return (String) redisTemplate.opsForValue().get(userKey);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 强制用户下线（删除所有相关token）
     */
    public void forceLogout(String username) {
        try {
            String userKey = USER_LOGIN_PREFIX + username;
            String currentToken = (String) redisTemplate.opsForValue().get(userKey);
            
            if (currentToken != null) {
                invalidateToken(currentToken);
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }
}