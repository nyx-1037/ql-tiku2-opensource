package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.AiGradingRecord;

import java.util.List;

/**
 * AI判题记录服务接口
 *
 * @author system
 * @since 2024-01-01
 */
public interface AiGradingRecordService extends IService<AiGradingRecord> {

    /**
     * 保存AI判题记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param userAnswer 用户答案
     * @param aiResult AI判题结果
     * @param isCorrect 是否正确
     * @return 保存的记录
     */
    AiGradingRecord saveGradingRecord(Long userId, Long questionId, String userAnswer, String aiResult, Boolean isCorrect);

    /**
     * 获取用户某题目的最新AI判题记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return AI判题记录
     */
    AiGradingRecord getLatestRecord(Long userId, Long questionId);

    /**
     * 获取用户某题目的所有历史记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return AI判题记录列表
     */
    List<AiGradingRecord> getHistoryRecords(Long userId, Long questionId);

    /**
     * 重新判题时软删除旧记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 是否成功
     */
    boolean softDeleteOldRecords(Long userId, Long questionId);
}