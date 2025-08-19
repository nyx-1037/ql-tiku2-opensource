package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.PracticeRecord;

import java.util.List;
import java.util.Map;

/**
 * 练习记录服务接口
 */
public interface PracticeRecordService extends IService<PracticeRecord> {
    
    /**
     * 分页查询用户练习记录
     *
     * @param page 当前页
     * @param size 每页大小
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @return 分页结果
     */
    Page<PracticeRecord> getPracticeRecordPage(Integer page, Integer size, Long userId,
                                              Long subjectId, Integer questionType, Integer difficulty);
    
    /**
     * 获取练习记录详情
     *
     * @param userId 用户ID
     * @param practiceType 练习类型
     * @return 练习记录详情列表
     */
    List<Map<String, Object>> getPracticeRecordDetails(Long userId, Integer practiceType);
    
    /**
     * 根据练习记录ID获取练习详情
     *
     * @param userId 用户ID
     * @param recordId 练习记录ID
     * @return 练习记录详情列表
     */
    List<Map<String, Object>> getPracticeRecordDetailsByRecordId(Long userId, Long recordId);
    
    /**
     * 创建练习记录
     *
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param subjectName 科目名称
     * @param questionType 题目类型
     * @param difficulty 难度
     * @return 练习记录ID
     */
    Long createPracticeRecord(Long userId, Long subjectId, String subjectName,
                             Integer questionType, Integer difficulty);
    
    /**
     * 更新练习记录
     *
     * @param practiceRecordId 练习记录ID
     * @param questionCount 题目数量
     * @param correctCount 正确数量
     * @param wrongCount 错误数量
     * @param totalTime 总用时
     * @param status 状态
     */
    void updatePracticeRecord(Long practiceRecordId, Integer questionCount, Integer correctCount,
                             Integer wrongCount, Integer totalTime, Integer status);
    
    /**
     * 添加答题记录
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param userAnswer 用户答案
     * @param isCorrect 是否正确
     * @param timeSpent 答题耗时
     * @param practiceType 练习类型
     */
    void addAnswerRecord(Long userId, Long questionId, String userAnswer, Boolean isCorrect, Integer timeSpent, Integer practiceType);
    
    /**
     * 获取练习记录统计信息
     *
     * @param userId 用户ID
     * @param recordId 练习记录ID
     * @return 统计信息
     */
    Map<String, Object> getPracticeRecordStats(Long userId, Long recordId);
}