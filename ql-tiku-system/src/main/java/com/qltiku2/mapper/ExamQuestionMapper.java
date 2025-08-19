package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.ExamQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 考试题目关联Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {
    
    /**
     * 根据考试ID获取题目列表（按排序）
     */
    @Select("SELECT * FROM exam_question WHERE exam_id = #{examId} ORDER BY sort_order ASC")
    List<ExamQuestion> selectByExamId(@Param("examId") Long examId);
    
    /**
     * 根据考试ID和题目ID查询
     */
    @Select("SELECT * FROM exam_question WHERE exam_id = #{examId} AND question_id = #{questionId}")
    ExamQuestion selectByExamIdAndQuestionId(@Param("examId") Long examId, @Param("questionId") Long questionId);
    
    /**
     * 统计考试题目数量
     */
    @Select("SELECT COUNT(*) FROM exam_question WHERE exam_id = #{examId}")
    Integer countByExamId(@Param("examId") Long examId);
    
    /**
     * 计算考试总分
     */
    @Select("SELECT SUM(score) FROM exam_question WHERE exam_id = #{examId}")
    Integer sumScoreByExamId(@Param("examId") Long examId);
}