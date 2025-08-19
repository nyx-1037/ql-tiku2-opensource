package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 练习会话题目Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface PracticeSessionQuestionMapper {
    
    /**
     * 添加练习会话题目记录
     */
    @Insert("INSERT INTO practice_session_questions (practice_record_id, question_id) VALUES (#{practiceRecordId}, #{questionId})")
    int insertPracticeSessionQuestion(@Param("practiceRecordId") Long practiceRecordId, @Param("questionId") Long questionId);
    
    /**
     * 检查题目是否已存在于练习会话中
     */
    @Select("SELECT COUNT(*) FROM practice_session_questions WHERE practice_record_id = #{practiceRecordId} AND question_id = #{questionId}")
    int countByPracticeRecordIdAndQuestionId(@Param("practiceRecordId") Long practiceRecordId, @Param("questionId") Long questionId);
    
    /**
     * 获取练习会话中的所有题目ID
     */
    @Select("SELECT question_id FROM practice_session_questions WHERE practice_record_id = #{practiceRecordId}")
    List<Long> selectQuestionIdsByPracticeRecordId(@Param("practiceRecordId") Long practiceRecordId);
    
    /**
     * 删除练习会话的所有题目记录
     */
    @Select("DELETE FROM practice_session_questions WHERE practice_record_id = #{practiceRecordId}")
    int deleteByPracticeRecordId(@Param("practiceRecordId") Long practiceRecordId);
}