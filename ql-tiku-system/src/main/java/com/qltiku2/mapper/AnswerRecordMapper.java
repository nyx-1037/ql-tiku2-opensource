package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.AnswerRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 答题记录Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {
    
    /**
     * 分页查询用户答题记录（带题目和科目信息）
     */
    @Select("SELECT ar.*, q.title as question_title, q.correct_answer, q.question_type, s.name as subject_name " +
            "FROM answer_record ar " +
            "LEFT JOIN question q ON ar.question_id = q.id " +
            "LEFT JOIN subject s ON q.subject_id = s.id " +
            "WHERE ar.user_id = #{userId} " +
            "ORDER BY ar.create_time DESC")
    IPage<AnswerRecord> selectUserRecordsPage(Page<AnswerRecord> page, @Param("userId") Long userId);
    
    /**
     * 统计用户答题情况
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct_count, " +
            "SUM(CASE WHEN is_correct = 0 THEN 1 ELSE 0 END) as wrong_count, " +
            "ROUND(SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as accuracy_rate " +
            "FROM answer_record WHERE user_id = #{userId}")
    Map<String, Object> selectUserStats(@Param("userId") Long userId);
    
    /**
     * 统计用户各科目答题情况
     */
    @Select("SELECT s.name as subject_name, " +
            "COUNT(ar.id) as total_count, " +
            "SUM(CASE WHEN ar.is_correct = 1 THEN 1 ELSE 0 END) as correct_count, " +
            "ROUND(SUM(CASE WHEN ar.is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(ar.id), 2) as accuracy_rate " +
            "FROM answer_record ar " +
            "LEFT JOIN question q ON ar.question_id = q.id " +
            "LEFT JOIN subject s ON q.subject_id = s.id " +
            "WHERE ar.user_id = #{userId} " +
            "GROUP BY s.id, s.name")
    List<Map<String, Object>> selectUserSubjectStats(@Param("userId") Long userId);
    
    /**
     * 统计用户各题型答题情况
     */
    @Select("SELECT q.question_type, " +
            "COUNT(ar.id) as total_count, " +
            "SUM(CASE WHEN ar.is_correct = 1 THEN 1 ELSE 0 END) as correct_count, " +
            "ROUND(SUM(CASE WHEN ar.is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(ar.id), 2) as accuracy_rate " +
            "FROM answer_record ar " +
            "LEFT JOIN question q ON ar.question_id = q.id " +
            "WHERE ar.user_id = #{userId} " +
            "GROUP BY q.question_type")
    List<Map<String, Object>> selectUserQuestionTypeStats(@Param("userId") Long userId);
    
    /**
     * 获取用户最近答题记录
     */
    @Select("SELECT ar.*, q.title as question_title, s.name as subject_name " +
            "FROM answer_record ar " +
            "LEFT JOIN question q ON ar.question_id = q.id " +
            "LEFT JOIN subject s ON q.subject_id = s.id " +
            "WHERE ar.user_id = #{userId} " +
            "ORDER BY ar.create_time DESC LIMIT #{limit}")
    List<AnswerRecord> selectRecentRecords(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 统计用户每日答题数量
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
            "FROM answer_record " +
            "WHERE user_id = #{userId} AND create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> selectDailyStats(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 检查用户是否已答过某题
     */
    @Select("SELECT COUNT(*) FROM answer_record WHERE user_id = #{userId} AND question_id = #{questionId}")
    Integer checkUserAnswered(@Param("userId") Long userId, @Param("questionId") Long questionId);
    
    /**
     * 统计用户参加的不同考试次数
     */
    @Select("SELECT COUNT(DISTINCT exam_id) FROM answer_record WHERE user_id = #{userId} AND practice_type = 2 AND exam_id IS NOT NULL")
    Integer countDistinctExamsByUserId(@Param("userId") Long userId);
}