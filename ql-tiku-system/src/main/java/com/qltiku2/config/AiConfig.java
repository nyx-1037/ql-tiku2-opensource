package com.qltiku2.config;

import org.springframework.ai.chat.client.ChatClient;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.qltiku2.service.SysConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class AiConfig {
    private final SysConfigService sysConfigService;

    public AiConfig(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Autowired
    private DashScopeChatModel dashScopeChatModel;
    
    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(dashScopeChatModel).build();
    }
}