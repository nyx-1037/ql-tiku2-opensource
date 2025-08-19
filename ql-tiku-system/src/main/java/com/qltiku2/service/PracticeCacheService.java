package com.qltiku2.service;

import com.qltiku2.utils.QuestionCacheUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 练习缓存服务
 * 负责管理Redis中的题目ID缓存，支持顺序和随机刷题模式
 * 
 * @author qltiku2
 */
@Service
public class PracticeCacheService {
    
    @Autowired
    private QuestionCacheUtils questionCacheUtils;
    
    private static final String PRACTICE_CACHE_PREFIX = "practice:ids:";
    private static final String PRACTICE_INDEX_PREFIX = "practice:index:";
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    /**
     * 生成并缓存题目ID数组
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式（sequential/random）
     * @param questionIds 题目ID列表
     */
    public void cacheQuestionIds(Long userId, Long subjectId, Integer questionType, 
                               Integer difficulty, String mode, List<Long> questionIds) {
        String cacheKey = buildCacheKey(userId, subjectId, questionType, difficulty, mode);
        
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        
        // 存储题目ID数组
        questionCacheUtils.set(cacheKey, questionIds);
        
        // 初始化当前索引为0
        String indexKey = buildIndexKey(userId, subjectId, questionType, difficulty, mode);
        questionCacheUtils.set(indexKey, 0);
    }
    
    /**
     * 获取下一个题目ID
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式
     * @return 下一个题目ID，如果没有则返回null
     */
    public Long getNextQuestionId(Long userId, Long subjectId, Integer questionType, 
                                Integer difficulty, String mode) {
        String cacheKey = buildCacheKey(userId, subjectId, questionType, difficulty, mode);
        String indexKey = buildIndexKey(userId, subjectId, questionType, difficulty, mode);
        
        // 获取题目ID数组
        List<Long> questionIds = getCachedQuestionIds(userId, subjectId, questionType, difficulty, mode);
        if (questionIds == null || questionIds.isEmpty()) {
            return null;
        }
        
        // 获取当前索引
        Integer currentIndex = questionCacheUtils.get(indexKey, Integer.class);
        if (currentIndex == null) {
            currentIndex = 0;
        }
        
        // 检查索引是否越界
        if (currentIndex >= questionIds.size()) {
            return null;
        }
        
        // 获取题目ID
        Long questionId = questionIds.get(currentIndex);
        
        // 更新索引
        questionCacheUtils.set(indexKey, currentIndex + 1);
        
        return questionId;
    }
    
    /**
     * 获取缓存的题目ID数组
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式
     * @return 题目ID列表
     */
    public List<Long> getCachedQuestionIds(Long userId, Long subjectId, Integer questionType, 
                                         Integer difficulty, String mode) {
        String cacheKey = buildCacheKey(userId, subjectId, questionType, difficulty, mode);
        Object cachedData = questionCacheUtils.get(cacheKey);

        if (cachedData == null) {
            return null;
        }

        try {
            if (cachedData instanceof List) {
                List<?> rawList = (List<?>) cachedData;
                List<Long> result = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof Integer) {
                        result.add(((Integer) item).longValue());
                    } else if (item instanceof Long) {
                        result.add((Long) item);
                    } else {
                        // 类型不符，清除缓存
                        clearCache(userId, subjectId, questionType, difficulty, mode);
                        return null;
                    }
                }
                return result;
            }
        } catch (Exception e) {
            // 类型转换失败，清除缓存
            clearCache(userId, subjectId, questionType, difficulty, mode);
        }

        return null;
    }
    
    /**
     * 检查缓存是否存在
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式
     * @return 是否存在缓存
     */
    public boolean hasCache(Long userId, Long subjectId, Integer questionType, 
                          Integer difficulty, String mode) {
        String cacheKey = buildCacheKey(userId, subjectId, questionType, difficulty, mode);
        return questionCacheUtils.get(cacheKey) != null;
    }
    
    /**
     * 重置索引（重新开始刷题）
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式
     */
    public void resetIndex(Long userId, Long subjectId, Integer questionType, 
                         Integer difficulty, String mode) {
        String indexKey = buildIndexKey(userId, subjectId, questionType, difficulty, mode);
        questionCacheUtils.set(indexKey, 0);
    }
    
    /**
     * 清除缓存
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度等级
     * @param mode 刷题模式
     */
    public void clearCache(Long userId, Long subjectId, Integer questionType, 
                          Integer difficulty, String mode) {
        String cacheKey = buildCacheKey(userId, subjectId, questionType, difficulty, mode);
        String indexKey = cacheKey + ":index";
        
        questionCacheUtils.delete(cacheKey);
        questionCacheUtils.delete(indexKey);
    }
    
    /**
     * 获取当前进度
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式
     * @return 进度信息（当前索引/总数）
     */
    public Map<String, Integer> getProgress(Long userId, Long subjectId, Integer questionType, 
                                          Integer difficulty, String mode) {
        String cacheKey = buildCacheKey(userId, subjectId, questionType, difficulty, mode);
        String indexKey = buildIndexKey(userId, subjectId, questionType, difficulty, mode);
        
        List<Long> questionIds = getCachedQuestionIds(userId, subjectId, questionType, difficulty, mode);
        if (questionIds == null) {
            return Map.of("current", 0, "total", 0);
        }
        
        Integer currentIndex = questionCacheUtils.get(indexKey, Integer.class);
        if (currentIndex == null) {
            currentIndex = 0;
        }
        
        return Map.of("current", currentIndex, "total", questionIds.size());
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(Long userId, Long subjectId, Integer questionType, 
                               Integer difficulty, String mode) {
        return PRACTICE_CACHE_PREFIX + userId + ":" + 
               (subjectId != null ? subjectId : "all") + ":" +
               (questionType != null ? questionType : "all") + ":" +
               (difficulty != null ? difficulty : "all") + ":" + mode;
    }
    
    /**
     * 构建索引键
     */
    private String buildIndexKey(Long userId, Long subjectId, Integer questionType, 
                               Integer difficulty, String mode) {
        return PRACTICE_INDEX_PREFIX + userId + ":" + 
               (subjectId != null ? subjectId : "all") + ":" +
               (questionType != null ? questionType : "all") + ":" +
               (difficulty != null ? difficulty : "all") + ":" + mode;
    }
}