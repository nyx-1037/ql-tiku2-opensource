package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qltiku2.entity.AiChatRecord;
import com.qltiku2.entity.AiModel;
import com.qltiku2.entity.AiUsageLog;
import com.qltiku2.mapper.AiChatRecordMapper;
import com.qltiku2.mapper.AiModelMapper;
import com.qltiku2.mapper.AiUsageLogMapper;
import com.qltiku2.service.AiChatService;
import com.qltiku2.service.AiModelService;
import com.qltiku2.service.SysConfigService;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天服务实现类
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Slf4j
@Service
public class AiChatServiceImpl extends ServiceImpl<AiChatRecordMapper, AiChatRecord> implements AiChatService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private AiUsageLogMapper aiUsageLogMapper;

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Flux<String> sendMessage(Long userId, String sessionId, String message, Long questionId, Long modelId) {
        try {
            // 获取当前安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();

            // 获取AI模型配置
            AiModel aiModel = getAiModel(modelId);
            if (aiModel == null) {
                return Flux.just("AI模型配置错误，请联系管理员");
            }

            // 保存用户消息
            saveChatRecord(userId, sessionId, message, AiChatRecord.MESSAGE_TYPE_USER, questionId);

            log.info("开始AI聊天，用户ID: {}, 会话ID: {}, 模型: {}, 消息: {}", userId, sessionId, aiModel.getName(), message);

            // 生成唯一的缓存键用于存储AI回复
            String cacheKey = "ai_response:" + sessionId + ":" + UUID.randomUUID().toString();

            // 创建使用日志记录
            AiUsageLog usageLog = createUsageLog(userId, "chat", aiModel, message);

            // 创建独立的ChatClient实例，确保模型配置完全隔离
            ChatClient localChatClient = createChatClient(aiModel);

            // 使用指定模型进行流式响应
            return localChatClient.prompt()
                    .user(message)
                    .stream()
                    .content()
                    .contextWrite(Context.of(SecurityContext.class, securityContext))
                    .doOnNext(content -> {
                        log.debug("AI响应片段: {}", content);
                        // 将每个响应片段追加到Redis缓存中
                        try {
                            redisTemplate.opsForValue().append(cacheKey, content);
                            redisTemplate.expire(cacheKey, 10, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存AI响应片段失败: {}", e.getMessage());
                        }
                    })
                    .doOnError(error -> {
                        log.error("AI服务调用失败", error);
                        String errorMsg = "AI服务调用失败: " + error.getMessage();
                        saveChatRecord(userId, sessionId, errorMsg, AiChatRecord.MESSAGE_TYPE_AI, questionId);
                        redisTemplate.delete(cacheKey);

                        // 记录失败的使用日志
                        updateUsageLogOnFailure(usageLog);
                    })
                    .doOnComplete(() -> {
                        log.info("AI聊天完成，用户ID: {}, 会话ID: {}", userId, sessionId);
                        try {
                            String fullResponse = redisTemplate.opsForValue().get(cacheKey);
                            if (StringUtils.hasText(fullResponse)) {
                                saveChatRecord(userId, sessionId, fullResponse, AiChatRecord.MESSAGE_TYPE_AI, questionId);
                                log.info("AI回复已保存到数据库，长度: {}", fullResponse.length());

                                // 更新使用日志
                                updateUsageLogOnSuccess(usageLog, message, fullResponse);
                            }
                            redisTemplate.delete(cacheKey);
                        } catch (Exception e) {
                            log.error("保存AI回复到数据库失败", e);
                        }
                    })
                    .onErrorResume(error -> {
                        String errorResponse = "抱歉，AI服务暂时不可用: " + error.getMessage();
                        saveChatRecord(userId, sessionId, errorResponse, AiChatRecord.MESSAGE_TYPE_AI, questionId);
                        redisTemplate.delete(cacheKey);
                        return Flux.just(errorResponse);
                    });

        } catch (Exception e) {
            log.error("发送AI消息失败", e);
            String errorResponse = "抱歉，AI服务暂时不可用";
            saveChatRecord(userId, sessionId, errorResponse, AiChatRecord.MESSAGE_TYPE_AI, questionId);
            return Flux.just(errorResponse);
        }
    }

    @Override
    public Flux<String> analyzeQuestion(Long userId, String sessionId, String questionContent, String options, Long questionId, Long modelId) {
        try {
            // 获取当前安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();

            // 获取AI模型配置
            AiModel aiModel = getAiModel(modelId);
            if (aiModel == null) {
                return Flux.just("AI模型配置错误，请联系管理员");
            }

            // 从数据库获取AI解析前缀配置
            String analysisPrefix = sysConfigService.getConfigValue("ai.analysis.prefix", "请详细解析以下题目，包括解题思路、知识点分析和答案解释：");

            // 构建题目解析提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(analysisPrefix).append("\n\n");
            promptBuilder.append("题目：").append(questionContent);
            if (StringUtils.hasText(options)) {
                promptBuilder.append("\n\n选项：\n").append(options);
            }
            promptBuilder.append("\n\n请提供详细的解析过程。");

            String promptMessage = promptBuilder.toString();
            saveChatRecord(userId, sessionId, promptMessage, AiChatRecord.MESSAGE_TYPE_USER, questionId);

            log.info("开始AI题目解析，用户ID: {}, 题目ID: {}, 模型: {}", userId, questionId, aiModel.getName());

            // 生成唯一的缓存键用于存储AI回复
            String cacheKey = "ai_analysis:" + sessionId + ":" + UUID.randomUUID().toString();

            // 创建使用日志记录
            AiUsageLog usageLog = createUsageLog(userId, "analyze", aiModel, promptMessage);
            
            // 创建独立的ChatClient实例，确保模型配置完全隔离
            ChatClient localChatClient = createChatClient(aiModel);

            // 使用指定模型进行题目解析
            return localChatClient.prompt()
                    .user(promptMessage)
                    .stream()
                    .content()
                    .contextWrite(Context.of(SecurityContext.class, securityContext))
                    .doOnNext(content -> {
                        log.debug("AI解析片段: {}", content);
                        try {
                            redisTemplate.opsForValue().append(cacheKey, content);
                            redisTemplate.expire(cacheKey, 10, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            log.warn("缓存AI解析片段失败: {}", e.getMessage());
                        }
                    })
                    .doOnError(error -> {
                        log.error("AI题目解析失败", error);
                        String errorMsg = "AI解析失败: " + error.getMessage();
                        saveChatRecord(userId, sessionId, errorMsg, AiChatRecord.MESSAGE_TYPE_AI, questionId);
                        redisTemplate.delete(cacheKey);

                        // 记录失败的使用日志
                        updateUsageLogOnFailure(usageLog);
                    })
                    .doOnComplete(() -> {
                        log.info("AI题目解析完成，用户ID: {}, 题目ID: {}", userId, questionId);
                        try {
                            String fullResponse = redisTemplate.opsForValue().get(cacheKey);
                            if (StringUtils.hasText(fullResponse)) {
                                saveChatRecord(userId, sessionId, fullResponse, AiChatRecord.MESSAGE_TYPE_AI, questionId);
                                log.info("AI解析已保存到数据库，长度: {}", fullResponse.length());

                                // 更新使用日志
                                updateUsageLogOnSuccess(usageLog, promptMessage, fullResponse);
                            }
                            redisTemplate.delete(cacheKey);
                        } catch (Exception e) {
                            log.error("保存AI解析到数据库失败", e);
                        }
                    })
                    .onErrorResume(error -> {
                        String errorResponse = "抱歉，AI解析服务暂时不可用: " + error.getMessage();
                        saveChatRecord(userId, sessionId, errorResponse, AiChatRecord.MESSAGE_TYPE_AI, questionId);
                        redisTemplate.delete(cacheKey);
                        return Flux.just(errorResponse);
                    });

        } catch (Exception e) {
            log.error("AI题目解析失败", e);
            String errorResponse = "抱歉，AI解析服务暂时不可用";
            saveChatRecord(userId, sessionId, errorResponse, AiChatRecord.MESSAGE_TYPE_AI, questionId);
            return Flux.just(errorResponse);
        }
    }

    @Override
    public List<AiChatRecord> getChatHistory(Long userId, String sessionId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        return baseMapper.getChatHistory(userId, sessionId, limit);
    }

    @Override
    public List<String> getRecentSessions(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return baseMapper.getRecentSessions(userId, limit);
    }

    @Override
    public void saveChatRecord(Long userId, String sessionId, String content, Integer messageType, Long questionId) {
        AiChatRecord record = new AiChatRecord()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setContent(content)
                .setMessageType(messageType)
                .setQuestionId(questionId);
        save(record);
    }

    /**
     * 获取AI模型配置，如果指定ID不存在则返回默认模型
     */
    private AiModel getAiModel(Long modelId) {
        if (modelId != null) {
            // 直接从数据库获取模型实体
            AiModel model = aiModelMapper.selectById(modelId);
            if (model != null && model.getEnabled()) {
                return model;
            }
        }
        // 如果没有指定有效的模型ID，则返回默认模型
        return aiModelService.getDefaultModel();
    }

    /**
     * 根据模型配置创建DashScopeChatOptions
     */
    private DashScopeChatOptions createChatOptions(AiModel aiModel) {
        log.info("使用AI模型: {} ({})", aiModel.getName(), aiModel.getCode());
        String modelCode = aiModel.getCode();

        // 简化配置，只设置模型代码，避免API兼容性问题
        return DashScopeChatOptions.builder()
                .withModel(modelCode)
                .build();
    }

    /**
     * 创建独立的ChatClient实例，确保模型配置完全隔离
     */
    private ChatClient createChatClient(AiModel aiModel) {
        DashScopeChatOptions options = createChatOptions(aiModel);
        
        // 使用全局的DashScopeChatModel但设置特定的默认选项
        ChatClient client = ChatClient.builder(dashScopeChatModel)
                .defaultOptions(options)
                .build();
                
        log.info("创建ChatClient完成，使用模型: {}", aiModel.getCode());
        return client;
    }

    /**
     * 创建使用日志记录
     */
    private AiUsageLog createUsageLog(Long userId, String aiType, AiModel aiModel, String requestContent) {
        AiUsageLog usageLog = new AiUsageLog();
        usageLog.setUserId(userId);
        usageLog.setAiType(aiType);
        usageLog.setModelId(aiModel.getId());
        usageLog.setModelCode(aiModel.getCode());
        usageLog.setUsageTime(LocalDateTime.now());
        usageLog.setTokensUsed(0);
        usageLog.setPromptTokens(0);
        usageLog.setCompletionTokens(0);
        usageLog.setTotalTokens(0);

        log.debug("创建使用日志，请求内容长度: {}", requestContent != null ? requestContent.length() : 0);
        return usageLog;
    }

    /**
     * AI调用成功后更新使用日志
     */
    private void updateUsageLogOnSuccess(AiUsageLog usageLog, String prompt, String response) {
        int estimatedPromptTokens = estimateTokens(prompt);
        int estimatedCompletionTokens = estimateTokens(response);
        int estimatedTotalTokens = estimatedPromptTokens + estimatedCompletionTokens;

        usageLog.setTokensUsed(estimatedTotalTokens);
        usageLog.setPromptTokens(estimatedPromptTokens);
        usageLog.setCompletionTokens(estimatedCompletionTokens);
        usageLog.setTotalTokens(estimatedTotalTokens);
        aiUsageLogMapper.insert(usageLog);

        log.info("Token使用统计已记录: prompt={}, completion={}, total={}",
                estimatedPromptTokens, estimatedCompletionTokens, estimatedTotalTokens);
    }
    
    /**
     * AI调用失败后更新使用日志
     */
    private void updateUsageLogOnFailure(AiUsageLog usageLog) {
        usageLog.setTokensUsed(0);
        usageLog.setPromptTokens(0);
        usageLog.setCompletionTokens(0);
        usageLog.setTotalTokens(0);
        aiUsageLogMapper.insert(usageLog);
    }

    /**
     * 估算token数量（简单实现，实际应该使用更精确的方法）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：中文字符按1.5个token计算，英文单词按1个token计算
        int chineseChars = 0;
        int englishWords = 0;

        for (char c : text.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fff) {
                chineseChars++;
            }
        }

        String[] words = text.replaceAll("[\\u4e00-\\u9fff]", "").split("\\s+");
        englishWords = words.length;

        return (int) (chineseChars * 1.5 + englishWords);
    }
}