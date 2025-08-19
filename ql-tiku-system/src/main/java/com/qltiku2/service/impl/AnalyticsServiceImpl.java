package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qltiku2.common.Result;
import com.qltiku2.entity.*;
import com.qltiku2.mapper.*;
import com.qltiku2.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据分析服务实现类
 * 
 * @author qltiku2
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    
    @Override
    public Result<Map<String, Object>> getAnalyticsData(String dateRange, String subjectId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 获取概览数据
            Result<Map<String, Object>> overviewResult = getOverviewStats();
            if (overviewResult.getCode() == 200) {
                result.put("overview", overviewResult.getData());
            }
            
            // 获取用户数据
            Result<Map<String, Object>> userResult = getUserActivityStats(dateRange);
            if (userResult.getCode() == 200) {
                Map<String, Object> userData = userResult.getData();
                result.put("userData", userData.get("userGrowthData"));
            }
            
            // 获取题目数据
            Result<Map<String, Object>> questionResult = getQuestionStats();
            if (questionResult.getCode() == 200) {
                Map<String, Object> questionData = questionResult.getData();
                result.put("questionData", questionData.get("subjectDistribution"));
            }
            
            // 获取考试数据
            Result<Map<String, Object>> examResult = getExamAnalytics(dateRange);
            if (examResult.getCode() == 200) {
                Map<String, Object> examData = examResult.getData();
                result.put("examData", examData.get("examData"));
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取分析数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getOverviewStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 总用户数
            Long totalUsers = userMapper.selectCount(null);
            stats.put("totalUsers", totalUsers);
            
            // 总题目数
            Long totalQuestions = questionMapper.selectCount(null);
            stats.put("totalQuestions", totalQuestions);
            
            // 总考试次数（答题记录中exam_id不为空的记录数）
            QueryWrapper<AnswerRecord> examWrapper = new QueryWrapper<>();
            examWrapper.isNotNull("exam_id");
            Long totalExams = answerRecordMapper.selectCount(examWrapper);
            stats.put("totalExams", totalExams);
            
            // 活跃用户数（最近7天有答题记录的用户）
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            QueryWrapper<AnswerRecord> activeWrapper = new QueryWrapper<>();
            activeWrapper.ge("create_time", sevenDaysAgo);
            activeWrapper.select("DISTINCT user_id");
            Long activeUsers = answerRecordMapper.selectCount(activeWrapper);
            stats.put("activeUsers", activeUsers);
            
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取概览统计失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getUserActivityStats(String dateRange) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 用户增长趋势（最近7天）
            List<Map<String, Object>> userGrowthData = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            
            for (int i = 6; i >= 0; i--) {
                LocalDateTime date = now.minusDays(i);
                LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
                LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);
                
                QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
                wrapper.between("create_time", startOfDay, endOfDay);
                Long count = userMapper.selectCount(wrapper);
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.format(formatter));
                dayData.put("count", count);
                userGrowthData.add(dayData);
            }
            
            stats.put("userGrowthData", userGrowthData);
            
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取用户活跃度统计失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getQuestionStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 按科目统计题目数量
            List<Map<String, Object>> subjectStats = new ArrayList<>();
            List<Subject> subjects = subjectMapper.selectList(null);
            
            for (Subject subject : subjects) {
                QueryWrapper<Question> wrapper = new QueryWrapper<>();
                wrapper.eq("subject_id", subject.getId());
                Long count = questionMapper.selectCount(wrapper);
                
                Map<String, Object> subjectStat = new HashMap<>();
                subjectStat.put("name", subject.getName());
                subjectStat.put("value", count);
                subjectStats.add(subjectStat);
            }
            
            stats.put("subjectDistribution", subjectStats);
            
            // 按难度统计
            List<Map<String, Object>> difficultyStats = new ArrayList<>();
            String[] difficulties = {"简单", "中等", "困难"};
            
            for (int i = 1; i <= 3; i++) {
                QueryWrapper<Question> wrapper = new QueryWrapper<>();
                wrapper.eq("difficulty", i);
                Long count = questionMapper.selectCount(wrapper);
                
                Map<String, Object> difficultyStat = new HashMap<>();
                difficultyStat.put("name", difficulties[i-1]);
                difficultyStat.put("value", count);
                difficultyStats.add(difficultyStat);
            }
            
            stats.put("difficultyDistribution", difficultyStats);
            
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取题目统计失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getExamAnalytics(String dateRange) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 考试统计（最近7天的答题情况）
            List<Map<String, Object>> examData = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            
            for (int i = 6; i >= 0; i--) {
                LocalDateTime date = now.minusDays(i);
                LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
                LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);
                
                QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
                wrapper.between("create_time", startOfDay, endOfDay);
                Long count = answerRecordMapper.selectCount(wrapper);
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.format(formatter));
                dayData.put("count", count);
                examData.add(dayData);
            }
            
            stats.put("examData", examData);
            
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取考试统计失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getWrongQuestionStats(String dateRange) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 错题统计
            Long totalWrongQuestions = wrongQuestionMapper.selectCount(null);
            stats.put("totalWrongQuestions", totalWrongQuestions);
            
            // 已掌握的错题数
            QueryWrapper<WrongQuestion> masteredWrapper = new QueryWrapper<>();
            masteredWrapper.eq("is_mastered", 1);
            Long masteredCount = wrongQuestionMapper.selectCount(masteredWrapper);
            stats.put("masteredCount", masteredCount);
            
            // 未掌握的错题数
            Long notMasteredCount = totalWrongQuestions - masteredCount;
            stats.put("notMasteredCount", notMasteredCount);
            
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取错题统计失败：" + e.getMessage());
        }
    }
}