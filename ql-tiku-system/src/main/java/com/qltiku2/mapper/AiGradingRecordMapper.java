package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.AiGradingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI判题记录 Mapper 接口
 *
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface AiGradingRecordMapper extends BaseMapper<AiGradingRecord> {

    /**
     * 根据用户ID和题目ID查询最新的有效记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return AI判题记录
     */
    @Select("SELECT * FROM ai_grading_records WHERE user_id = #{userId} AND question_id = #{questionId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT 1")
    AiGradingRecord getLatestRecord(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 根据用户ID和题目ID查询所有历史记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return AI判题记录列表
     */
    @Select("SELECT * FROM ai_grading_records WHERE user_id = #{userId} AND question_id = #{questionId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<AiGradingRecord> getHistoryRecords(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 软删除用户某题目的所有记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 影响行数
     */
    @Update("UPDATE ai_grading_records SET is_deleted = 1 WHERE user_id = #{userId} AND question_id = #{questionId} AND is_deleted = 0")
    int softDeleteByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
}