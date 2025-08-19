package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.AiChatRecord;
import com.qltiku2.mapper.AiChatRecordMapper;
import com.qltiku2.service.AiChatRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiChatRecordServiceImpl extends ServiceImpl<AiChatRecordMapper, AiChatRecord> implements AiChatRecordService {

    @Override
    public List<AiChatRecord> getChatHistory(String sessionId) {
        QueryWrapper<AiChatRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.orderByAsc("create_time");
        return list(wrapper);
    }

    @Override
    public void saveChatRecord(AiChatRecord record) {
        save(record);
    }
}