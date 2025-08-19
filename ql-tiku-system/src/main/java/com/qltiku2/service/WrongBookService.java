package com.qltiku2.service;

import com.qltiku2.common.Result;

/**
 * 错题本服务接口
 */
public interface WrongBookService {

    /**
     * 获取错题列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @param subjectId 科目ID
     * @param type 题目类型
     * @param difficulty 难度
     * @param wrongType 错题类型
     * @return 错题列表
     */
    Result getWrongQuestions(Long userId, Integer page, Integer size, Long subjectId, String type, String difficulty, String wrongType);

    /**
     * 移除错题
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 操作结果
     */
    Result removeWrongQuestion(Long userId, Long questionId);

    /**
     * 添加错题
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param userAnswer 用户答案
     * @param wrongType 错题类型
     * @return 操作结果
     */
    Result addWrongQuestion(Long userId, Long questionId, String userAnswer, String wrongType);

    /**
     * 清空错题本
     * @param userId 用户ID
     * @return 操作结果
     */
    Result clearWrongQuestions(Long userId);
}