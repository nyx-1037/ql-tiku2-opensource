package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.AiChatRecord;
import java.util.List;

public interface AiChatRecordService extends IService<AiChatRecord> {
    List<AiChatRecord> getChatHistory(String sessionId);
    void saveChatRecord(AiChatRecord record);
}