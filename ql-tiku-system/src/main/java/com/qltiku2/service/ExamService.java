package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.dto.AnswerSubmitRequest;
import com.qltiku2.dto.ExamQueryRequest;
import com.qltiku2.dto.ExamSaveRequest;
import com.qltiku2.dto.SimulationExamRequest;
import com.qltiku2.entity.Exam;
import com.qltiku2.vo.ExamVO;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 考试服务接口
 * 
 * @author qltiku2
 */
public interface ExamService {
    
    /**
     * 分页查询考试
     * 
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    IPage<ExamVO> pageExams(ExamQueryRequest queryRequest);
    
    /**
     * 根据ID获取考试详情
     * 
     * @param id 考试ID
     * @return 考试详情
     */
    ExamVO getExamById(Long id);
    
    /**
     * 创建考试
     * 
     * @param saveRequest 保存请求
     * @param createBy 创建人ID
     * @return 考试ID
     */
    Long createExam(ExamSaveRequest saveRequest, Long createBy);
    
    /**
     * 更新考试
     * 
     * @param saveRequest 保存请求
     * @return 是否成功
     */
    boolean updateExam(ExamSaveRequest saveRequest);
    
    /**
     * 删除考试
     * 
     * @param id 考试ID
     * @return 是否成功
     */
    boolean deleteExam(Long id);
    
    /**
     * 批量删除考试
     * 
     * @param ids 考试ID列表
     * @return 是否成功
     */
    boolean batchDeleteExams(List<Long> ids);
    
    /**
     * 更新考试状态
     * 
     * @param id 考试ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateExamStatus(Long id, Integer status);
    
    /**
     * 获取考试列表（不分页）
     * 
     * @param subjectId 科目ID（可选）
     * @return 考试列表
     */
    List<ExamVO> listExams(Long subjectId);
    
    /**
     * 检查考试是否存在
     * 
     * @param id 考试ID
     * @return 是否存在
     */
    boolean existsById(Long id);
    
    /**
     * 获取考试实体
     * 
     * @param id 考试ID
     * @return 考试实体
     */
    Exam getExamEntityById(Long id);
    
    /**
     * 获取可用考试列表（客户端）
     * 
     * @param subjectId 科目ID（可选）
     * @return 可用考试列表
     */
    List<ExamVO> listAvailableExams(Long subjectId);
    
    /**
     * 获取固定试卷列表（客户端）
     * 
     * @param subjectId 科目ID（可选）
     * @return 固定试卷列表
     */
    List<ExamVO> listFixedPapers(Long subjectId);
    
    /**
     * 分页查询固定试卷列表（支持模糊查询）
     * 
     * @param current 当前页
     * @param size 页大小
     * @param subjectId 科目ID（可选）
     * @param keyword 关键词（可选）
     * @return 固定试卷分页列表
     */
    IPage<ExamVO> pageFixedPapers(Integer current, Integer size, Long subjectId, String keyword);
    
    /**
     * 开始固定试卷考试
     * 
     * @param examId 固定试卷ID
     * @param userId 用户ID
     * @return 考试会话信息
     */
    Map<String, Object> startFixedPaperExam(Long examId, Long userId);
    
    /**
     * 提交固定试卷考试
     * 
     * @param examId 固定试卷ID
     * @param userId 用户ID
     * @param answers 答案列表
     * @return 考试结果
     */
    Map<String, Object> submitFixedPaperExam(Long examId, Long userId, List<AnswerSubmitRequest> answers);
    
    /**
     * 开始考试
     * 
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试会话信息
     */
    Map<String, Object> startExam(Long examId, Long userId);
    
    /**
     * 提交考试
     * 
     * @param examId 考试ID
     * @param userId 用户ID
     * @param answers 答案列表
     * @return 考试结果
     */
    Map<String, Object> submitExam(Long examId, Long userId, List<AnswerSubmitRequest> answers);
    
    /**
     * 获取考试结果
     * 
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试结果
     */
    Map<String, Object> getExamResult(Long examId, Long userId);
    
    /**
     * 获取用户考试记录（支持搜索和分页）
     * 
     * @param userId 用户ID
     * @param current 当前页
     * @param size 页大小
     * @param keyword 关键词搜索
     * @param subjectId 科目ID
     * @param status 考试状态
     * @return 考试记录分页
     */
    IPage<Map<String, Object>> getUserExamRecords(Long userId, Integer current, Integer size, String keyword, Long subjectId, Integer status);
    
    /**
     * 获取考试记录详情
     * @param recordId 考试记录ID（正数获取最新记录，负数获取特定批次记录）
     * @param userId 用户ID
     * @return 考试记录详情
     */
    Map<String, Object> getExamRecordDetail(Long recordId, Long userId);
    
    /**
     * 获取用户的所有考试记录批次
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试记录批次列表
     */
    List<Map<String, Object>> getUserExamRecords(Long examId, Long userId);
    
    /**
     * 创建模拟考试
     * 
     * @param request 模拟考试请求
     * @param userId 用户ID
     * @return 考试会话信息
     */
    Map<String, Object> createSimulationExam(SimulationExamRequest request, Long userId);
    
    /**
     * 提交模拟考试
     * 
     * @param examId 模拟考试ID
     * @param userId 用户ID
     * @param answers 答案列表
     * @return 考试结果
     */
    Map<String, Object> submitSimulationExam(String examId, Long userId, List<AnswerSubmitRequest> answers);
    
    /**
     * 获取模拟考试记录详情
     * 
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 考试记录详情
     */
    Map<String, Object> getSimulationExamRecordDetail(String examId, Long userId);
    
    /**
     * 导出考试数据
     * 
     * @param examId 考试ID
     * @param response HTTP响应
     */
    void exportExamData(Long examId, HttpServletResponse response) throws Exception;
}