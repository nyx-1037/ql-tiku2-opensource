package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AnswerSubmitRequest;
import com.qltiku2.dto.QuestionQueryRequest;
import com.qltiku2.dto.QuestionSaveRequest;
import com.qltiku2.vo.AnswerSubmitResponse;
import com.qltiku2.vo.QuestionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 题目服务接口
 * 
 * @author qltiku2
 */
public interface QuestionService {
    
    /**
     * 分页查询题目列表
     */
    Result<IPage<QuestionVO>> getQuestionPage(QuestionQueryRequest request);
    
    /**
     * 根据ID获取题目详情
     */
    Result<QuestionVO> getQuestionById(Long id);
    
    /**
     * 创建题目
     */
    Result<String> createQuestion(QuestionSaveRequest request);
    
    /**
     * 更新题目
     */
    Result<String> updateQuestion(Long id, QuestionSaveRequest request);
    
    /**
     * 删除题目
     */
    Result<String> deleteQuestion(Long id);
    
    /**
     * 批量删除题目
     */
    Result<String> batchDeleteQuestions(List<Long> ids);
    
    /**
     * 随机获取题目（用于练习）
     */
    Result<List<QuestionVO>> getRandomQuestions(Long subjectId, Integer questionType, 
                                               Integer difficulty, Integer count);
    
    /**
     * 随机获取题目（用于模拟考试）
     */
    List<QuestionVO> getRandomQuestions(Long subjectId, Integer count, List<String> difficulties);
    
    /**
     * 获取练习题目（支持顺序和随机不重复）
     */
    Result<List<QuestionVO>> getPracticeQuestions(Long userId, Long subjectId, Integer questionType, 
                                                 Integer difficulty, Integer count, String mode, String excludeIds);
    
    /**
     * 搜索题目
     */
    Result<IPage<QuestionVO>> searchQuestions(String keyword, Integer current, Integer size);
    
    /**
    /**
     * 获取题目统计信息
     */
    Result<Object> getQuestionStatistics();
    
    /**
    /**
     * 获取科目题目数量统计
     */
    Result<Map<String, Object>> getSubjectQuestionStats();
    
    /**
     * 获取指定科目的题目数量
     */
    Result<Integer> getQuestionCountBySubject(Long subjectId);
    
    /**
     * 导入题目（批量）
     */
    Result<String> importQuestions(List<QuestionSaveRequest> questions);
    
    /**
     * 导出题目
     */
    Result<List<QuestionVO>> exportQuestions(QuestionQueryRequest request);
    
    /**
     * 提交答案
     */
    Result<AnswerSubmitResponse> submitAnswer(AnswerSubmitRequest request);
    
    /**
     * AI判题
     */
    Result<Map<String, Object>> aiGrading(Map<String, Object> request);
    
    /**
     * AI判题流式响应
     */
    void aiGradingStream(Map<String, Object> request, Long userId, SseEmitter emitter);
    
    /**
     * 获取AI判题历史记录
     */
    Result<Map<String, Object>> getAiGradingHistory(Long questionId);
}