package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.AiChatRecord;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI聊天服务接口
 *
 * @author qltiku2
 * @since 2024-01-01
 */
public interface AiChatService extends IService<AiChatRecord> {

    /**
     * 发送消息给AI并获取流式响应
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param message 用户消息
     * @param questionId 关联题目ID（可选）
     * @param modelId AI模型ID（可选，为空时使用默认模型）
     * @return AI响应流
     */
    Flux<String> sendMessage(Long userId, String sessionId, String message, Long questionId, Long modelId);

    /**
     * 题目解析
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param questionContent 题目内容
     * @param options 题目选项（可选）
     * @param questionId 题目ID
     * @param modelId AI模型ID（可选，为空时使用默认模型）
     * @return AI解析响应流
     */
    Flux<String> analyzeQuestion(Long userId, String sessionId, String questionContent, String options, Long questionId, Long modelId);

    /**
     * 获取聊天历史记录
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param limit 限制条数
     * @return 聊天记录列表
     */
    List<AiChatRecord> getChatHistory(Long userId, String sessionId, Integer limit);

    /**
     * 获取最近的会话列表
     *
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 会话ID列表
     */
    List<String> getRecentSessions(Long userId, Integer limit);

    /**
     * 保存聊天记录
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param messageType 消息类型
     * @param questionId 关联题目ID（可选）
     */
    void saveChatRecord(Long userId, String sessionId, String content, Integer messageType, Long questionId);
}