package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.AiChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI聊天记录Mapper接口
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Mapper
public interface AiChatRecordMapper extends BaseMapper<AiChatRecord> {

    /**
     * 根据用户ID和会话ID获取聊天记录
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param limit 限制条数
     * @return 聊天记录列表
     */
    @Select("SELECT * FROM ai_chat_record WHERE user_id = #{userId} AND session_id = #{sessionId} ORDER BY create_time ASC LIMIT #{limit}")
    List<AiChatRecord> getChatHistory(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("limit") Integer limit);

    /**
     * 根据用户ID获取最近的会话列表
     *
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 会话ID列表
     */
    @Select("SELECT DISTINCT session_id FROM ai_chat_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<String> getRecentSessions(@Param("userId") Long userId, @Param("limit") Integer limit);
}