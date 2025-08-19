package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.AiGradingRecord;
import com.qltiku2.mapper.AiGradingRecordMapper;
import com.qltiku2.service.AiGradingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI判题记录服务实现类
 *
 * @author system
 * @since 2024-01-01
 */
@Service
public class AiGradingRecordServiceImpl extends ServiceImpl<AiGradingRecordMapper, AiGradingRecord> implements AiGradingRecordService {

    @Autowired
    private AiGradingRecordMapper aiGradingRecordMapper;

    @Override
    @Transactional
    public AiGradingRecord saveGradingRecord(Long userId, Long questionId, String userAnswer, String aiResult, Boolean isCorrect) {
        AiGradingRecord record = new AiGradingRecord();
        record.setUserId(userId);
        record.setQuestionId(questionId);
        record.setUserAnswer(userAnswer);
        record.setAiResult(aiResult);
        record.setIsCorrect(isCorrect);
        record.setIsDeleted(false);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        
        save(record);
        return record;
    }

    @Override
    public AiGradingRecord getLatestRecord(Long userId, Long questionId) {
        return aiGradingRecordMapper.getLatestRecord(userId, questionId);
    }

    @Override
    public List<AiGradingRecord> getHistoryRecords(Long userId, Long questionId) {
        return aiGradingRecordMapper.getHistoryRecords(userId, questionId);
    }

    @Override
    @Transactional
    public boolean softDeleteOldRecords(Long userId, Long questionId) {
        int affectedRows = aiGradingRecordMapper.softDeleteByUserAndQuestion(userId, questionId);
        return affectedRows > 0;
    }
}