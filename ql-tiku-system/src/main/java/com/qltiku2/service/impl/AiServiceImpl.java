package com.qltiku2.service.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import com.qltiku2.entity.AiChatRecord;
import com.qltiku2.service.AiChatRecordService;
import com.qltiku2.service.AiService;
import com.qltiku2.service.SysConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiServiceImpl implements AiService {
    private final ChatClient chatClient;
    private final AiChatRecordService chatRecordService;
    private final SysConfigService sysConfigService;

    public AiServiceImpl(ChatClient chatClient, AiChatRecordService chatRecordService, SysConfigService sysConfigService) {
        this.chatClient = chatClient;
        this.chatRecordService = chatRecordService;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public Flux<ResponseEntity<String>> analyzeQuestion(Map<String, Object> params) {
        String question = (String) params.get("question");
        String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
        Long userId = (Long) params.get("userId");
        Long questionId = (Long) params.get("questionId");
        String prefix = sysConfigService.getConfigValue("ai.analysis.prefix", "请分析以下题目：");
        String fullMessage = prefix + question;

        AiChatRecord userRecord = new AiChatRecord();
        userRecord.setUserId(userId);
        userRecord.setSessionId(sessionId);
        userRecord.setQuestionId(questionId);
        userRecord.setMessageType(AiChatRecord.MESSAGE_TYPE_USER);
        userRecord.setContent(fullMessage);
        chatRecordService.saveChatRecord(userRecord);

        return chatClient.prompt()
                .user(fullMessage)
                .stream()
                .content()
                .map(content -> ResponseEntity.ok("data: " + content + "\n\n"));
    }

    @Override
    public Flux<ResponseEntity<String>> sendMessage(Map<String, Object> params) {
        String message = (String) params.get("message");
        String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
        Long userId = (Long) params.get("userId");

        AiChatRecord userRecord = new AiChatRecord();
        userRecord.setUserId(userId);
        userRecord.setSessionId(sessionId);
        userRecord.setMessageType(AiChatRecord.MESSAGE_TYPE_USER);
        userRecord.setContent(message);
        chatRecordService.saveChatRecord(userRecord);

        List<Message> history = loadHistory(sessionId);
        history.add(new UserMessage(message));

        Flux<String> responseFlux = chatClient.prompt()
                .messages(history)
                .stream()
                .content();

        return responseFlux.doOnNext(content -> {
            AiChatRecord aiRecord = new AiChatRecord();
            aiRecord.setUserId(userId);
            aiRecord.setSessionId(sessionId);
            aiRecord.setMessageType(AiChatRecord.MESSAGE_TYPE_AI);
            aiRecord.setContent(content);
            chatRecordService.saveChatRecord(aiRecord);
        }).map(content -> ResponseEntity.ok("data: " + content + "\n\n"));
    }

    private List<Message> loadHistory(String sessionId) {
        List<AiChatRecord> records = chatRecordService.getChatHistory(sessionId);
        List<Message> messages = new ArrayList<>();
        for (AiChatRecord record : records) {
            if (record.getMessageType() != null && record.getMessageType() == AiChatRecord.MESSAGE_TYPE_USER) {
                messages.add(new UserMessage(record.getContent()));
            } else if (record.getMessageType() != null && record.getMessageType() == AiChatRecord.MESSAGE_TYPE_AI) {
                messages.add(new AssistantMessage(record.getContent()));
            }
        }
        return messages;
    }
}