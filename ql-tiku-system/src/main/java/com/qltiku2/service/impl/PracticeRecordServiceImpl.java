package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.AnswerRecord;
import com.qltiku2.entity.PracticeRecord;
import com.qltiku2.mapper.AnswerRecordMapper;
import com.qltiku2.mapper.PracticeRecordMapper;
import com.qltiku2.service.PracticeRecordService;
import com.qltiku2.service.WrongBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

/**
 * 练习记录服务实现类
 */
@Service
public class PracticeRecordServiceImpl extends ServiceImpl<PracticeRecordMapper, PracticeRecord> implements PracticeRecordService {
    
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    
    @Autowired
    private WrongBookService wrongBookService;
    
    @Override
    public Page<PracticeRecord> getPracticeRecordPage(Integer page, Integer size, Long userId,
                                                     Long subjectId, Integer questionType, Integer difficulty) {
        Page<PracticeRecord> pageObj = new Page<>(page, size);
        return baseMapper.selectPracticeRecordPage(pageObj, userId, subjectId, questionType, difficulty);
    }
    
    @Override
    public List<Map<String, Object>> getPracticeRecordDetails(Long userId, Integer practiceType) {
        return baseMapper.selectAnswerRecordsByUser(userId, practiceType);
    }
    
    @Override
    public List<Map<String, Object>> getPracticeRecordDetailsByRecordId(Long userId, Long recordId) {
        // 首先验证练习记录是否属于当前用户
        PracticeRecord practiceRecord = getById(recordId);
        if (practiceRecord == null || !practiceRecord.getUserId().equals(userId)) {
            throw new RuntimeException("练习记录不存在或无权限访问");
        }
        
        // 根据练习记录的时间范围查询对应的答题记录
        return baseMapper.selectAnswerRecordsByRecordId(userId, recordId, practiceRecord.getStartTime(), practiceRecord.getEndTime());
    }
    
    @Override
    public Long createPracticeRecord(Long userId, Long subjectId, String subjectName,
                                    Integer questionType, Integer difficulty) {
        PracticeRecord practiceRecord = new PracticeRecord();
        practiceRecord.setUserId(userId);
        practiceRecord.setSubjectId(subjectId);
        practiceRecord.setSubjectName(subjectName);
        practiceRecord.setQuestionType(questionType);
        practiceRecord.setDifficulty(difficulty);
        practiceRecord.setTotalQuestions(0);
        practiceRecord.setCorrectCount(0);
        practiceRecord.setWrongCount(0);
        practiceRecord.setUnansweredCount(0);
        practiceRecord.setAccuracyRate(0.0);
        practiceRecord.setDuration(0);
        practiceRecord.setStartTime(LocalDateTime.now());
        practiceRecord.setStatus(1); // 进行中
        practiceRecord.setCreateTime(LocalDateTime.now());
        practiceRecord.setUpdateTime(LocalDateTime.now());
        practiceRecord.setIsDeleted(0);
        
        save(practiceRecord);
        return practiceRecord.getId();
    }
    
    @Override
    public void updatePracticeRecord(Long practiceRecordId, Integer questionCount, Integer correctCount,
                                    Integer wrongCount, Integer totalTime, Integer status) {
        PracticeRecord practiceRecord = getById(practiceRecordId);
        if (practiceRecord != null) {
            practiceRecord.setTotalQuestions(questionCount);
            practiceRecord.setCorrectCount(correctCount);
            practiceRecord.setWrongCount(wrongCount);
            practiceRecord.setUnansweredCount(questionCount - correctCount - wrongCount);
            practiceRecord.setAccuracyRate(questionCount > 0 ? (double) correctCount / questionCount * 100 : 0.0);
            practiceRecord.setDuration(totalTime);
            practiceRecord.setStatus(status);
            if (status == 2) { // 已完成
                practiceRecord.setEndTime(LocalDateTime.now());
            }
            practiceRecord.setUpdateTime(LocalDateTime.now());
            updateById(practiceRecord);
        }
    }
    
    @Override
    public Map<String, Object> getPracticeRecordStats(Long userId, Long recordId) {
        // 验证练习记录是否属于当前用户
        PracticeRecord practiceRecord = getById(recordId);
        if (practiceRecord == null || !practiceRecord.getUserId().equals(userId)) {
            throw new RuntimeException("练习记录不存在或无权限访问");
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", practiceRecord.getTotalQuestions() != null ? practiceRecord.getTotalQuestions() : 0);
        stats.put("correctCount", practiceRecord.getCorrectCount() != null ? practiceRecord.getCorrectCount() : 0);
        stats.put("wrongCount", practiceRecord.getWrongCount() != null ? practiceRecord.getWrongCount() : 0);
        stats.put("accuracyRate", practiceRecord.getAccuracyRate() != null ? practiceRecord.getAccuracyRate().intValue() : 0);
        stats.put("score", practiceRecord.getCorrectCount() != null ? practiceRecord.getCorrectCount() : 0); // 简单计分：正确数即为得分
        
        return stats;
    }
    
    @Override
    public void addAnswerRecord(Long userId, Long questionId, String userAnswer, Boolean isCorrect, Integer timeSpent, Integer practiceType) {
        System.out.println("=== PracticeRecordServiceImpl.addAnswerRecord 开始 ===");
        System.out.println("参数 - userId: " + userId + ", questionId: " + questionId + ", userAnswer: " + userAnswer + ", isCorrect: " + isCorrect + ", timeSpent: " + timeSpent + ", practiceType: " + practiceType);
        
        try {
            AnswerRecord answerRecord = new AnswerRecord();
            answerRecord.setUserId(userId);
            answerRecord.setQuestionId(questionId);
            answerRecord.setUserAnswer(userAnswer);
            answerRecord.setIsCorrect(isCorrect != null && isCorrect ? 1 : 0);
            answerRecord.setAnswerTime(timeSpent);
            answerRecord.setPracticeType(practiceType);
            answerRecord.setCreateTime(LocalDateTime.now());
            
            System.out.println("准备插入答题记录: " + answerRecord);
            
            int result = answerRecordMapper.insert(answerRecord);
            System.out.println("插入结果: " + result + ", 记录ID: " + answerRecord.getId());
            
            // 如果答错了，添加到错题本
            if (isCorrect != null && !isCorrect) {
                try {
                    System.out.println("答题错误，添加到错题本...");
                    wrongBookService.addWrongQuestion(userId, questionId, userAnswer, "PRACTICE");
                    System.out.println("错题本添加成功");
                } catch (Exception e) {
                    // 记录日志但不影响主流程
                    System.err.println("添加错题失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("✅ 答题记录添加成功");
            System.out.println("=== PracticeRecordServiceImpl.addAnswerRecord 成功结束 ===");
            
        } catch (Exception e) {
            System.err.println("=== PracticeRecordServiceImpl.addAnswerRecord 异常 ===");
            e.printStackTrace();
            throw new RuntimeException("添加答题记录失败: " + e.getMessage(), e);
        }
    }
}