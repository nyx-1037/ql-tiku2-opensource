package com.qltiku2.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 题目缓存工具类
 * 
 * @author qltiku2
 */
@Component
public class QuestionCacheUtils {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 缓存前缀
    private static final String CACHE_PREFIX = "question:";
    
    // 缓存过期时间（24小时）
    private static final long CACHE_TTL = 24;
    
    /**
     * 设置缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.opsForValue().set(cacheKey, value, CACHE_TTL, TimeUnit.HOURS);
    }
    
    /**
     * 获取缓存
     * 
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        String cacheKey = CACHE_PREFIX + key;
        return redisTemplate.opsForValue().get(cacheKey);
    }
    
    /**
     * 获取缓存并转换为指定类型
     * 
     * @param key 缓存键
     * @param clazz 目标类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> clazz) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }
        
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        } else {
            try {
                // 处理LinkedHashMap转换为具体类型的情况
                if (obj instanceof java.util.LinkedHashMap) {
                    String json = objectMapper.writeValueAsString(obj);
                    return objectMapper.readValue(json, clazz);
                }
                
                // 尝试通过JSON转换
                String json = objectMapper.writeValueAsString(obj);
                return objectMapper.readValue(json, clazz);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
    }
    
    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    public void delete(String key) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
    }
    
    /**
     * 删除指定前缀的所有缓存
     * 
     * @param prefix 前缀
     */
    public void deleteByPrefix(String prefix) {
        String pattern = CACHE_PREFIX + prefix + "*";
        redisTemplate.keys(pattern).forEach(key -> redisTemplate.delete(key));
    }
    
    /**
     * 清除所有题目缓存
     */
    public void clearAll() {
        String pattern = CACHE_PREFIX + "*";
        redisTemplate.keys(pattern).forEach(key -> redisTemplate.delete(key));
    }
}