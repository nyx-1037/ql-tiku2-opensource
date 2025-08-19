package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 练习记录 Mapper 接口
 */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {
    
    /**
     * 分页查询用户练习记录
     *
     * @param page 分页对象
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @return 练习记录列表
     */
    Page<PracticeRecord> selectPracticeRecordPage(Page<PracticeRecord> page,
                                                  @Param("userId") Long userId,
                                                  @Param("subjectId") Long subjectId,
                                                  @Param("questionType") Integer questionType,
                                                  @Param("difficulty") Integer difficulty);
    
    /**
     * 获取练习记录详情（从answer_record表查询）
     *
     * @param userId 用户ID
     * @param practiceType 练习类型
     * @return 答题记录列表
     */
    List<Map<String, Object>> selectAnswerRecordsByUser(@Param("userId") Long userId,
                                                        @Param("practiceType") Integer practiceType);
    
    /**
     * 根据练习记录ID获取答题记录详情
     *
     * @param userId 用户ID
     * @param recordId 练习记录ID
     * @param startTime 练习开始时间
     * @param endTime 练习结束时间
     * @return 答题记录列表
     */
    List<Map<String, Object>> selectAnswerRecordsByRecordId(@Param("userId") Long userId,
                                                            @Param("recordId") Long recordId,
                                                            @Param("startTime") java.time.LocalDateTime startTime,
                                                            @Param("endTime") java.time.LocalDateTime endTime);
}