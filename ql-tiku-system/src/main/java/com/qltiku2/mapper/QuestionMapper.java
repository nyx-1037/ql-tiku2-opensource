package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 题目Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
    
    /**
     * 分页查询题目（带科目名称）
     */
    IPage<Question> selectQuestionPage(Page<Question> page, @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<Question> queryWrapper);
    
    /**
     * 随机获取题目
     */
    @Select("SELECT * FROM question WHERE subject_id = #{subjectId} AND deleted = 0 ORDER BY RAND() LIMIT #{count}")
    List<Question> selectRandomQuestions(@Param("subjectId") Long subjectId, @Param("count") Integer count);
    
    /**
     * 根据题型随机获取题目
     */
    @Select("SELECT * FROM question WHERE subject_id = #{subjectId} AND question_type = #{questionType} AND deleted = 0 ORDER BY RAND() LIMIT #{count}")
    List<Question> selectRandomQuestionsByType(@Param("subjectId") Long subjectId, 
                                              @Param("questionType") Integer questionType, 
                                              @Param("count") Integer count);
    
    /**
     * 根据难度随机获取题目
     */
    @Select("SELECT * FROM question WHERE subject_id = #{subjectId} AND difficulty = #{difficulty} AND deleted = 0 ORDER BY RAND() LIMIT #{count}")
    List<Question> selectRandomQuestionsByDifficulty(@Param("subjectId") Long subjectId, 
                                                     @Param("difficulty") Integer difficulty, 
                                                     @Param("count") Integer count);
    
    /**
     * 统计题目数量按科目
     */
    @Select("SELECT s.name as subject_name, COUNT(q.id) as count " +
            "FROM subject s LEFT JOIN question q ON s.id = q.subject_id AND q.deleted = 0 " +
            "WHERE s.deleted = 0 GROUP BY s.id, s.name")
    List<Map<String, Object>> countQuestionsBySubject();
    
    /**
     * 统计题目数量按类型
     */
    @Select("SELECT question_type, COUNT(*) as count FROM question WHERE deleted = 0 GROUP BY question_type")
    List<Map<String, Object>> countQuestionsByType();
    
    /**
     * 统计题目数量按难度
     */
    @Select("SELECT difficulty, COUNT(*) as count FROM question WHERE deleted = 0 GROUP BY difficulty")
    List<Map<String, Object>> countQuestionsByDifficulty();
    
    /**
     * 搜索题目
     */
    @Select("SELECT q.*, s.name as subject_name FROM question q " +
            "LEFT JOIN subject s ON q.subject_id = s.id " +
            "WHERE q.deleted = 0 AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))")
    List<Question> searchQuestions(@Param("keyword") String keyword);
    
    /**
     * 统计指定科目的题目数量
     */
    @Select("SELECT COUNT(*) FROM question WHERE subject_id = #{subjectId} AND deleted = 0")
    Integer countQuestionsBySubjectId(@Param("subjectId") Long subjectId);
    
    /**
     * 获取指定练习记录中已做过的题目ID列表
     */
    @Select("SELECT question_id FROM practice_session_questions WHERE practice_record_id = #{practiceRecordId}")
    List<Long> selectAnsweredQuestionIds(@Param("practiceRecordId") Long practiceRecordId);
}