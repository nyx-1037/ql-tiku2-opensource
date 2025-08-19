package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 错题本Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {
    
    /**
     * 分页查询用户错题（带题目详情）
     */
    @Select("SELECT wq.*, q.title as question_title, q.content as question_content, " +
            "q.options, q.correct_answer, q.analysis, q.question_type, q.difficulty, " +
            "q.knowledge_points, s.name as subject_name " +
            "FROM wrong_question wq " +
            "LEFT JOIN question q ON wq.question_id = q.id " +
            "LEFT JOIN subject s ON q.subject_id = s.id " +
            "WHERE wq.user_id = #{userId} " +
            "${ew.customSqlSegment}")
    IPage<WrongQuestion> selectUserWrongQuestionsPage(Page<WrongQuestion> page, 
                                                      @Param("userId") Long userId,
                                                      @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<WrongQuestion> queryWrapper);
    
    /**
     * 查询用户错题列表（用于练习）
     */
    @Select("SELECT wq.*, q.title as question_title, q.content as question_content, " +
            "q.options, q.correct_answer, q.analysis, q.question_type, q.difficulty " +
            "FROM wrong_question wq " +
            "LEFT JOIN question q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} AND wq.is_mastered = 0 " +
            "ORDER BY wq.last_wrong_time DESC")
    List<WrongQuestion> selectUserUnmasteredQuestions(@Param("userId") Long userId);
    
    /**
     * 根据科目查询用户错题
     */
    @Select("SELECT wq.*, q.title as question_title, q.content as question_content, " +
            "q.options, q.correct_answer, q.analysis, q.question_type, q.difficulty " +
            "FROM wrong_question wq " +
            "LEFT JOIN question q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} AND q.subject_id = #{subjectId} AND wq.is_mastered = 0 " +
            "ORDER BY wq.last_wrong_time DESC")
    List<WrongQuestion> selectUserWrongQuestionsBySubject(@Param("userId") Long userId, @Param("subjectId") Long subjectId);
    
    /**
     * 根据题型查询用户错题
     */
    @Select("SELECT wq.*, q.title as question_title, q.content as question_content, " +
            "q.options, q.correct_answer, q.analysis, q.question_type, q.difficulty " +
            "FROM wrong_question wq " +
            "LEFT JOIN question q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} AND q.question_type = #{questionType} AND wq.is_mastered = 0 " +
            "ORDER BY wq.last_wrong_time DESC")
    List<WrongQuestion> selectUserWrongQuestionsByType(@Param("userId") Long userId, @Param("questionType") Integer questionType);
    
    /**
     * 统计用户错题情况
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN is_mastered = 0 THEN 1 ELSE 0 END) as unmastered_count, " +
            "SUM(CASE WHEN is_mastered = 1 THEN 1 ELSE 0 END) as mastered_count " +
            "FROM wrong_question WHERE user_id = #{userId}")
    Map<String, Object> selectUserWrongStats(@Param("userId") Long userId);
    
    /**
     * 统计用户各科目错题情况
     */
    @Select("SELECT s.name as subject_name, " +
            "COUNT(wq.id) as total_count, " +
            "SUM(CASE WHEN wq.is_mastered = 0 THEN 1 ELSE 0 END) as unmastered_count " +
            "FROM wrong_question wq " +
            "LEFT JOIN question q ON wq.question_id = q.id " +
            "LEFT JOIN subject s ON q.subject_id = s.id " +
            "WHERE wq.user_id = #{userId} " +
            "GROUP BY s.id, s.name")
    List<Map<String, Object>> selectUserWrongSubjectStats(@Param("userId") Long userId);
    
    /**
     * 统计用户各题型错题情况
     */
    @Select("SELECT q.question_type, " +
            "COUNT(wq.id) as total_count, " +
            "SUM(CASE WHEN wq.is_mastered = 0 THEN 1 ELSE 0 END) as unmastered_count " +
            "FROM wrong_question wq " +
            "LEFT JOIN question q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} " +
            "GROUP BY q.question_type")
    List<Map<String, Object>> selectUserWrongQuestionTypeStats(@Param("userId") Long userId);
    
    /**
     * 检查用户是否有某道错题
     */
    @Select("SELECT * FROM wrong_question WHERE user_id = #{userId} AND question_id = #{questionId}")
    WrongQuestion selectByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
    
    /**
     * 标记错题为已掌握
     */
    @Update("UPDATE wrong_question SET is_mastered = 1, master_time = NOW() WHERE user_id = #{userId} AND question_id = #{questionId}")
    Integer markAsMastered(@Param("userId") Long userId, @Param("questionId") Long questionId);
    
    /**
     * 增加错误次数
     */
    @Update("UPDATE wrong_question SET wrong_count = wrong_count + 1, last_wrong_time = NOW() WHERE user_id = #{userId} AND question_id = #{questionId}")
    Integer increaseWrongCount(@Param("userId") Long userId, @Param("questionId") Long questionId);
}