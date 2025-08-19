package com.qltiku2.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qltiku2.common.Result;
import com.qltiku2.dto.QuestionSaveRequest;
import com.qltiku2.entity.Question;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.QuestionMapper;
import com.qltiku2.mapper.SysUserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 题目批量导入服务
 * 提供Redis缓存和限流功能
 * 
 * @author qltiku2
 */
@Service
public class QuestionBatchImportService {
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String IMPORT_LIMIT_KEY = "question:import:limit:";
    private static final String IMPORT_CACHE_KEY = "question:import:cache:";
    private static final int MAX_IMPORT_PER_HOUR = 5000; // 每小时最多导入5000道题目
    private static final int BATCH_SIZE = 100; // 每批处理100道题目
    
    /**
     * 批量导入题目（带限流和缓存）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchImportQuestions(List<QuestionSaveRequest> questions) {
        try {
            if (questions == null || questions.isEmpty()) {
                return Result.badRequest("导入数据不能为空");
            }
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Result.unauthorized("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 检查导入限制
            String limitKey = IMPORT_LIMIT_KEY + user.getId();
            String currentCount = redisTemplate.opsForValue().get(limitKey);
            int importedCount = currentCount != null ? Integer.parseInt(currentCount) : 0;
            
            if (importedCount + questions.size() > MAX_IMPORT_PER_HOUR) {
                return Result.error("超出每小时导入限制（" + MAX_IMPORT_PER_HOUR + "道题目），当前已导入：" + importedCount + "道");
            }
            
            System.out.println("=== 开始批量导入题目 ===");
            System.out.println("用户：" + username + "（ID:" + user.getId() + "）");
            System.out.println("题目数量：" + questions.size());
            System.out.println("当前小时已导入：" + importedCount + "道");
            
            int successCount = 0;
            int failCount = 0;
            
            // 分批处理，减轻数据库压力
            for (int i = 0; i < questions.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, questions.size());
                List<QuestionSaveRequest> batch = questions.subList(i, endIndex);
                
                System.out.println("正在处理第 " + (i / BATCH_SIZE + 1) + " 批，题目 " + (i + 1) + "-" + endIndex);
                
                for (QuestionSaveRequest request : batch) {
                    try {
                        // 创建题目对象
                        Question question = new Question();
                        BeanUtils.copyProperties(request, question);
                        
                        // 处理选项JSON
                        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            question.setOptions(objectMapper.writeValueAsString(request.getOptions()));
                        }
                        
                        // 设置创建信息
                        question.setCreateUserId(user.getId());
                        question.setCreateTime(LocalDateTime.now());
                        question.setUpdateTime(LocalDateTime.now());
                        
                        // 插入数据库
                        questionMapper.insert(question);
                        successCount++;
                        
                        // 缓存题目信息（可选，用于快速查询）
                        String cacheKey = IMPORT_CACHE_KEY + question.getId();
                        redisTemplate.opsForValue().set(cacheKey, question.getTitle(), 24, TimeUnit.HOURS);
                        
                    } catch (Exception e) {
                        failCount++;
                        System.err.println("导入题目失败：" + e.getMessage());
                        // 继续处理下一道题目，不中断整个批次
                    }
                }
                
                // 批次间添加短暂延迟，避免数据库压力过大
                if (i + BATCH_SIZE < questions.size()) {
                    try {
                        Thread.sleep(100); // 延迟100毫秒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // 更新导入计数
            redisTemplate.opsForValue().set(limitKey, 
                String.valueOf(importedCount + successCount), 
                1, TimeUnit.HOURS);
            
            System.out.println("=== 批量导入完成 ===");
            System.out.println("成功导入：" + successCount + "道题目");
            System.out.println("失败：" + failCount + "道题目");
            System.out.println("用户当前小时导入总数：" + (importedCount + successCount));
            
            if (successCount > 0) {
                return Result.success("导入完成！成功：" + successCount + "道，失败：" + failCount + "道");
            } else {
                return Result.error("导入失败，请检查数据格式");
            }
            
        } catch (Exception e) {
            System.err.println("批量导入异常：" + e.getMessage());
            e.printStackTrace();
            return Result.error("批量导入失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户当前小时的导入统计
     */
    public Result<Object> getImportStatistics(Long userId) {
        try {
            String limitKey = IMPORT_LIMIT_KEY + userId;
            String currentCount = redisTemplate.opsForValue().get(limitKey);
            int importedCount = currentCount != null ? Integer.parseInt(currentCount) : 0;
            
            return Result.success(Map.of(
                "importedCount", importedCount,
                "maxImportPerHour", MAX_IMPORT_PER_HOUR,
                "remainingCount", MAX_IMPORT_PER_HOUR - importedCount
            ));
        } catch (Exception e) {
            return Result.error("获取导入统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 清除用户导入限制（管理员功能）
     */
    public Result<String> clearImportLimit(Long userId) {
        try {
            String limitKey = IMPORT_LIMIT_KEY + userId;
            redisTemplate.delete(limitKey);
            return Result.success("清除导入限制成功");
        } catch (Exception e) {
            return Result.error("清除导入限制失败：" + e.getMessage());
        }
    }
}