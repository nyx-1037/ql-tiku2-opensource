package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 科目Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {
    
    /**
     * 获取所有启用的科目（按排序）
     */
    @Select("SELECT * FROM subject WHERE status = 1 AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<Subject> selectEnabledSubjects();
    
    /**
     * 根据名称查询科目
     */
    @Select("SELECT * FROM subject WHERE name = #{name} AND deleted = 0")
    Subject selectByName(@Param("name") String name);
    
    /**
     * 获取科目及题目数量
     */
    @Select("SELECT s.*, COUNT(q.id) as question_count " +
            "FROM subject s LEFT JOIN question q ON s.id = q.subject_id AND q.deleted = 0 " +
            "WHERE s.deleted = 0 GROUP BY s.id ORDER BY s.sort_order ASC")
    List<Subject> selectSubjectsWithQuestionCount();
}