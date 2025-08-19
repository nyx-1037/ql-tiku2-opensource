package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI对话会话Mapper
 * 
 * @author qltiku2
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {
    
    /**
     * 根据用户ID查询会话列表
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    @Select("SELECT * FROM ai_chat_session WHERE user_id = #{userId} AND deleted = 0 ORDER BY update_time DESC")
    List<AiChatSession> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据会话ID查询会话
     * 
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @Select("SELECT * FROM ai_chat_session WHERE session_id = #{sessionId} AND deleted = 0")
    AiChatSession selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 根据用户ID和会话ID查询会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @Select("SELECT * FROM ai_chat_session WHERE user_id = #{userId} AND session_id = #{sessionId} AND deleted = 0")
    AiChatSession selectByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}