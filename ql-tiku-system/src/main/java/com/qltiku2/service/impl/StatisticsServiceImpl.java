package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qltiku2.common.Result;
import com.qltiku2.entity.AnswerRecord;
import com.qltiku2.entity.Question;
import com.qltiku2.entity.Subject;
import com.qltiku2.entity.WrongQuestion;
import com.qltiku2.mapper.AnswerRecordMapper;
import com.qltiku2.mapper.QuestionMapper;
import com.qltiku2.mapper.SubjectMapper;
import com.qltiku2.mapper.WrongQuestionMapper;
import com.qltiku2.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统计服务实现类
 * 
 * @author qltiku2
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
    
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    
    @Override
    public Result<Map<String, Object>> getPersonalStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 今日练习统计
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
            
            QueryWrapper<AnswerRecord> todayWrapper = new QueryWrapper<>();
            todayWrapper.eq("user_id", userId)
                       .between("create_time", startOfDay, endOfDay);
            
            List<AnswerRecord> todayRecords = answerRecordMapper.selectList(todayWrapper);
            
            int todayPracticeCount = todayRecords.size();
            double todayAccuracy = 0.0;
            
            if (!todayRecords.isEmpty()) {
                // 计算正确率（假设AnswerRecord有isCorrect字段）
                long correctCount = todayRecords.stream()
                    .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                    .sum();
                todayAccuracy = (double) correctCount / todayRecords.size() * 100;
            }
            
            // 总练习统计
            QueryWrapper<AnswerRecord> totalWrapper = new QueryWrapper<>();
            totalWrapper.eq("user_id", userId);
            int totalPracticeCount = Math.toIntExact(answerRecordMapper.selectCount(totalWrapper));
            
            // 构建返回数据
            Map<String, Object> todayStats = new HashMap<>();
            todayStats.put("practiceCount", todayPracticeCount);
            todayStats.put("accuracy", Math.round(todayAccuracy * 100) / 100.0);
            
            Map<String, Object> totalStats = new HashMap<>();
            totalStats.put("totalCount", totalPracticeCount);
            
            stats.put("today", todayStats);
            stats.put("total", totalStats);
            
            return Result.success(stats);
            
        } catch (Exception e) {
            return Result.error("获取个人统计数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getLearningReport(Long userId, String dateRange, Long subjectId) {
        Map<String, Object> report = new HashMap<>();
        
        try {
            // 解析日期范围
            LocalDateTime startDate = null;
            LocalDateTime endDate = LocalDateTime.now();
            
            if (dateRange != null && !dateRange.isEmpty()) {
                String[] dates = dateRange.split(",");
                if (dates.length == 2) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    startDate = LocalDate.parse(dates[0], formatter).atStartOfDay();
                    endDate = LocalDate.parse(dates[1], formatter).plusDays(1).atStartOfDay();
                }
            } else {
                // 默认最近30天
                startDate = endDate.minusDays(30);
            }
            
            // 构建查询条件
            QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            if (startDate != null) {
                wrapper.between("create_time", startDate, endDate);
            }
            // 注意：AnswerRecord没有subject_id字段，需要通过question关联
            
            List<AnswerRecord> records = answerRecordMapper.selectList(wrapper);
            
            // 概览统计
            Map<String, Object> overviewStats = new HashMap<>();
            overviewStats.put("totalQuestions", records.size());
            
            if (!records.isEmpty()) {
                // 计算正确率
                long correctCount = records.stream()
                    .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                    .sum();
                double correctRate = (double) correctCount / records.size() * 100;
                overviewStats.put("correctRate", Math.round(correctRate * 100) / 100.0);
                
                // 学习天数（去重日期）
                Set<LocalDate> studyDates = new HashSet<>();
                records.forEach(record -> {
                    studyDates.add(record.getCreateTime().toLocalDate());
                });
                overviewStats.put("studyDays", studyDates.size());
                
                // 学习时长（假设每题1分钟）
                overviewStats.put("studyTime", records.size());
            } else {
                overviewStats.put("correctRate", 0.0);
                overviewStats.put("studyDays", 0);
                overviewStats.put("studyTime", 0);
            }
            
            // 学习趋势数据（按日期分组）
            Map<String, Integer> trendData = new LinkedHashMap<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd");
            
            // 初始化最近7天的数据
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                trendData.put(date.format(dateFormatter), 0);
            }
            
            // 填充实际数据
            records.forEach(record -> {
                String dateKey = record.getCreateTime().format(dateFormatter);
                if (trendData.containsKey(dateKey)) {
                    trendData.put(dateKey, trendData.get(dateKey) + 1);
                }
            });
            
            // 正确率分析
            Map<String, Integer> accuracyData = new HashMap<>();
            accuracyData.put("正确", 0);
            accuracyData.put("错误", 0);
            
            records.forEach(record -> {
                // 基于isCorrect字段进行分类
                if (record.getIsCorrect() == 1) {
                    accuracyData.put("正确", accuracyData.get("正确") != null ? accuracyData.get("正确") + 1 : 1);
                } else {
                    accuracyData.put("错误", accuracyData.get("错误") != null ? accuracyData.get("错误") + 1 : 1);
                }
            });
            
            // 科目分布
            Map<String, Integer> subjectData = new HashMap<>();
            Map<Long, String> subjectNames = new HashMap<>();
            
            // 获取科目名称
            List<Subject> subjects = subjectMapper.selectList(null);
            subjects.forEach(subject -> {
                subjectNames.put(subject.getId(), subject.getName());
                subjectData.put(subject.getName(), 0);
            });
            
            // 由于AnswerRecord没有直接的subjectId，暂时使用模拟数据
            // 实际应用中需要通过questionId关联Question表获取subjectId
            subjectData.put("数学", records.size() / 3);
            subjectData.put("语文", records.size() / 3);
            subjectData.put("英语", records.size() - records.size() / 3 * 2);
            
            // 难度分析（需要关联题目表）
            Map<String, Integer> difficultyData = new HashMap<>();
            difficultyData.put("简单", 0);
            difficultyData.put("中等", 0);
            difficultyData.put("困难", 0);
            
            // 错题分析
            QueryWrapper<WrongQuestion> wrongWrapper = new QueryWrapper<>();
            wrongWrapper.eq("user_id", userId);
            if (startDate != null) {
                wrongWrapper.between("create_time", startDate, endDate);
            }
            int wrongCount = Math.toIntExact(wrongQuestionMapper.selectCount(wrongWrapper));
            
            Map<String, Object> wrongData = new HashMap<>();
            wrongData.put("总错题数", wrongCount);
            wrongData.put("待复习", Math.max(0, wrongCount - 10)); // 假设已复习10题
            wrongData.put("已掌握", Math.min(wrongCount, 10));
            
            // 学习建议
            List<String> suggestions = generateSuggestions(overviewStats, wrongCount);
            
            // 构建返回数据
            report.put("overviewStats", overviewStats);
            report.put("trendData", trendData);
            report.put("accuracyData", accuracyData);
            report.put("subjectData", subjectData);
            report.put("difficultyData", difficultyData);
            report.put("wrongData", wrongData);
            report.put("suggestions", suggestions);
            
            return Result.success(report);
            
        } catch (Exception e) {
            return Result.error("获取学习报告失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getOverviewStats(Long userId, String dateRange, Long subjectId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 解析日期范围
            LocalDateTime[] dateRangeArray = parseDateRange(dateRange);
            LocalDateTime startDate = dateRangeArray[0];
            LocalDateTime endDate = dateRangeArray[1];
            
            // 构建查询条件
            QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            if (startDate != null) {
                wrapper.between("create_time", startDate, endDate);
            }
            
            List<AnswerRecord> records = answerRecordMapper.selectList(wrapper);
            
            // 如果指定了科目，需要通过题目关联过滤
            if (subjectId != null) {
                records = records.stream()
                    .filter(record -> {
                        Question question = questionMapper.selectById(record.getQuestionId());
                        return question != null && question.getSubjectId().equals(subjectId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 总练习题数
            stats.put("totalQuestions", records.size());
            
            // 平均正确率
            if (!records.isEmpty()) {
                long correctCount = records.stream()
                    .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                    .sum();
                double correctRate = (double) correctCount / records.size() * 100;
                stats.put("correctRate", Math.round(correctRate * 100) / 100.0);
            } else {
                stats.put("correctRate", 0.0);
            }
            
            // 学习天数
            Set<LocalDate> studyDates = new HashSet<>();
            records.forEach(record -> {
                studyDates.add(record.getCreateTime().toLocalDate());
            });
            stats.put("studyDays", studyDates.size());
            
            // 学习时长（假设每题平均1分钟）
            stats.put("studyTime", Math.round(records.size() / 60.0 * 100) / 100.0);
            
            return Result.success(stats);
            
        } catch (Exception e) {
            return Result.error("获取概览统计数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getTrendData(Long userId, String dateRange, String trendType, Long subjectId) {
        Map<String, Object> trendData = new HashMap<>();
        
        try {
            // 解析日期范围
            LocalDateTime[] dateRangeArray = parseDateRange(dateRange);
            LocalDateTime startDate = dateRangeArray[0];
            LocalDateTime endDate = dateRangeArray[1];
            
            // 如果没有指定日期范围，根据趋势类型设置默认范围
            if (startDate == null) {
                if ("monthly".equals(trendType)) {
                    startDate = endDate.minusMonths(6);
                } else if ("weekly".equals(trendType)) {
                    startDate = endDate.minusWeeks(8);
                } else {
                    startDate = endDate.minusDays(7);
                }
            }
            
            // 构建查询条件
            QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .between("create_time", startDate, endDate);
            
            List<AnswerRecord> records = answerRecordMapper.selectList(wrapper);
            
            // 如果指定了科目，过滤记录
            if (subjectId != null) {
                records = records.stream()
                    .filter(record -> {
                        Question question = questionMapper.selectById(record.getQuestionId());
                        return question != null && question.getSubjectId().equals(subjectId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 根据趋势类型生成数据
            Map<String, Object> chartData = generateTrendChartData(records, startDate, endDate, trendType);
            trendData.putAll(chartData);
            
            return Result.success(trendData);
            
        } catch (Exception e) {
            return Result.error("获取学习趋势数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getAccuracyAnalysis(Long userId, String dateRange, Long subjectId) {
        Map<String, Object> accuracyData = new HashMap<>();
        
        try {
            // 解析日期范围
            LocalDateTime[] dateRangeArray = parseDateRange(dateRange);
            LocalDateTime startDate = dateRangeArray[0];
            LocalDateTime endDate = dateRangeArray[1];
            
            // 构建查询条件
            QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            if (startDate != null) {
                wrapper.between("create_time", startDate, endDate);
            }
            
            List<AnswerRecord> records = answerRecordMapper.selectList(wrapper);
            
            // 如果指定了科目，过滤记录
            if (subjectId != null) {
                records = records.stream()
                    .filter(record -> {
                        Question question = questionMapper.selectById(record.getQuestionId());
                        return question != null && question.getSubjectId().equals(subjectId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 按正确率区间分组
            Map<String, Integer> accuracyDistribution = new LinkedHashMap<>();
            accuracyDistribution.put("90-100%", 0);
            accuracyDistribution.put("80-90%", 0);
            accuracyDistribution.put("70-80%", 0);
            accuracyDistribution.put("60-70%", 0);
            accuracyDistribution.put("60%以下", 0);
            
            // 按日期分组计算每日正确率
            Map<LocalDate, List<AnswerRecord>> dailyRecords = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    record -> record.getCreateTime().toLocalDate()
                ));
            
            dailyRecords.forEach((date, dayRecords) -> {
                if (!dayRecords.isEmpty()) {
                    long correctCount = dayRecords.stream()
                        .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                        .sum();
                    double dailyAccuracy = (double) correctCount / dayRecords.size() * 100;
                    
                    if (dailyAccuracy >= 90) {
                        accuracyDistribution.put("90-100%", accuracyDistribution.get("90-100%") + 1);
                    } else if (dailyAccuracy >= 80) {
                        accuracyDistribution.put("80-90%", accuracyDistribution.get("80-90%") + 1);
                    } else if (dailyAccuracy >= 70) {
                        accuracyDistribution.put("70-80%", accuracyDistribution.get("70-80%") + 1);
                    } else if (dailyAccuracy >= 60) {
                        accuracyDistribution.put("60-70%", accuracyDistribution.get("60-70%") + 1);
                    } else {
                        accuracyDistribution.put("60%以下", accuracyDistribution.get("60%以下") + 1);
                    }
                }
            });
            
            accuracyData.put("distribution", accuracyDistribution);
            
            // 总体正确率
            if (!records.isEmpty()) {
                long totalCorrect = records.stream()
                    .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                    .sum();
                double overallAccuracy = (double) totalCorrect / records.size() * 100;
                accuracyData.put("overallAccuracy", Math.round(overallAccuracy * 100) / 100.0);
            } else {
                accuracyData.put("overallAccuracy", 0.0);
            }
            
            return Result.success(accuracyData);
            
        } catch (Exception e) {
            return Result.error("获取正确率分析数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析日期范围（健壮版）
     * 入参示例："2025-08-01,2025-08-08"
     * 规则：
     * - 非法或为空：回退为最近30天
     * - 结束日期大于今天：截断为今天
     * - 开始日期晚于结束日期：重置为 [结束, 结束]
     * - 返回的 endDate 为次日零点，便于 between 查询包含结束日
     */
    private LocalDateTime[] parseDateRange(String dateRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate;

        try {
            if (dateRange != null && !dateRange.isEmpty()) {
                String[] dates = dateRange.split(",");
                if (dates.length == 2) {
                    String s = dates[0] == null ? "" : dates[0].trim();
                    String e = dates[1] == null ? "" : dates[1].trim();
                    // 仅接受 yyyy-MM-dd 格式，其它一律视为非法
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    // 正则粗校验
                    if (!s.matches("\\d{4}-\\d{2}-\\d{2}") || !e.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        throw new IllegalArgumentException("非法日期格式");
                    }
                    LocalDate sDate = LocalDate.parse(s, formatter);
                    LocalDate eDate = LocalDate.parse(e, formatter);
                    // 截断结束日期到今天（不超过当前）
                    LocalDate today = LocalDate.now();
                    if (eDate.isAfter(today)) {
                        eDate = today;
                    }
                    // 校正开始>结束
                    if (sDate.isAfter(eDate)) {
                        sDate = eDate; // 置为同一天
                    }
                    startDate = sDate.atStartOfDay();
                    endDate = eDate.plusDays(1).atStartOfDay();
                } else {
                    // 结构非法，回退
                    startDate = now.minusDays(30);
                    endDate = now;
                }
            } else {
                // 默认最近30天
                startDate = now.minusDays(30);
                endDate = now;
            }
        } catch (Exception ex) {
            // 任何异常回退到最近30天，避免500
            startDate = now.minusDays(30);
            endDate = now;
        }

        return new LocalDateTime[]{startDate, endDate};
    }
    
    /**
     * 生成趋势图表数据
     */
    private Map<String, Object> generateTrendChartData(List<AnswerRecord> records, 
                                                       LocalDateTime startDate, 
                                                       LocalDateTime endDate, 
                                                       String trendType) {
        Map<String, Object> chartData = new HashMap<>();
        
        if ("monthly".equals(trendType)) {
            // 按月统计
            Map<String, Integer> monthlyData = new LinkedHashMap<>();
            Map<String, Double> monthlyAccuracy = new LinkedHashMap<>();
            
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            
            // 初始化月份数据
            LocalDateTime current = startDate;
            while (current.isBefore(endDate)) {
                String monthKey = current.format(monthFormatter);
                monthlyData.put(monthKey, 0);
                monthlyAccuracy.put(monthKey, 0.0);
                current = current.plusMonths(1);
            }
            
            // 填充实际数据
            Map<String, List<AnswerRecord>> monthlyRecords = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    record -> record.getCreateTime().format(monthFormatter)
                ));
            
            monthlyRecords.forEach((month, monthRecords) -> {
                if (monthlyData.containsKey(month)) {
                    monthlyData.put(month, monthRecords.size());
                    
                    long correctCount = monthRecords.stream()
                        .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                        .sum();
                    double accuracy = monthRecords.isEmpty() ? 0.0 : 
                        (double) correctCount / monthRecords.size() * 100;
                    monthlyAccuracy.put(month, Math.round(accuracy * 100) / 100.0);
                }
            });
            
            chartData.put("categories", new ArrayList<>(monthlyData.keySet()));
            chartData.put("practiceData", new ArrayList<>(monthlyData.values()));
            chartData.put("accuracyData", new ArrayList<>(monthlyAccuracy.values()));
            
        } else if ("weekly".equals(trendType)) {
            // 按周统计
            Map<String, Integer> weeklyData = new LinkedHashMap<>();
            Map<String, Double> weeklyAccuracy = new LinkedHashMap<>();
            
            // 初始化周数据
            LocalDateTime current = startDate;
            int weekNum = 1;
            while (current.isBefore(endDate)) {
                String weekKey = "第" + weekNum + "周";
                weeklyData.put(weekKey, 0);
                weeklyAccuracy.put(weekKey, 0.0);
                current = current.plusWeeks(1);
                weekNum++;
            }
            
            // 填充实际数据（简化处理，按周分组）
            Map<Integer, List<AnswerRecord>> weeklyRecords = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    record -> record.getCreateTime().getDayOfYear() / 7
                ));
            
            int index = 1;
            for (List<AnswerRecord> weekRecords : weeklyRecords.values()) {
                String weekKey = "第" + index + "周";
                if (weeklyData.containsKey(weekKey)) {
                    weeklyData.put(weekKey, weekRecords.size());
                    
                    long correctCount = weekRecords.stream()
                        .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                        .sum();
                    double accuracy = weekRecords.isEmpty() ? 0.0 : 
                        (double) correctCount / weekRecords.size() * 100;
                    weeklyAccuracy.put(weekKey, Math.round(accuracy * 100) / 100.0);
                }
                index++;
            }
            
            chartData.put("categories", new ArrayList<>(weeklyData.keySet()));
            chartData.put("practiceData", new ArrayList<>(weeklyData.values()));
            chartData.put("accuracyData", new ArrayList<>(weeklyAccuracy.values()));
            
        } else {
            // 按天统计（默认）
            Map<String, Integer> dailyData = new LinkedHashMap<>();
            Map<String, Double> dailyAccuracy = new LinkedHashMap<>();
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd");
            
            // 初始化日期数据
            LocalDateTime current = startDate;
            while (current.isBefore(endDate)) {
                String dateKey = current.format(dateFormatter);
                dailyData.put(dateKey, 0);
                dailyAccuracy.put(dateKey, 0.0);
                current = current.plusDays(1);
            }
            
            // 填充实际数据
            Map<String, List<AnswerRecord>> dailyRecords = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    record -> record.getCreateTime().format(dateFormatter)
                ));
            
            dailyRecords.forEach((date, dayRecords) -> {
                if (dailyData.containsKey(date)) {
                    dailyData.put(date, dayRecords.size());
                    
                    long correctCount = dayRecords.stream()
                        .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                        .sum();
                    double accuracy = dayRecords.isEmpty() ? 0.0 : 
                        (double) correctCount / dayRecords.size() * 100;
                    dailyAccuracy.put(date, Math.round(accuracy * 100) / 100.0);
                }
            });
            
            chartData.put("categories", new ArrayList<>(dailyData.keySet()));
            chartData.put("practiceData", new ArrayList<>(dailyData.values()));
            chartData.put("accuracyData", new ArrayList<>(dailyAccuracy.values()));
        }
        
        return chartData;
    }
    
    @Override
    public Result<Map<String, Object>> getSubjectDistribution(Long userId, String dateRange, Long subjectId) {
        Map<String, Object> subjectData = new HashMap<>();

        try {
            // 解析日期范围
            LocalDateTime[] dateRangeArray = parseDateRange(dateRange);
            LocalDateTime startDate = dateRangeArray[0];
            LocalDateTime endDate = dateRangeArray[1];

            // 构建查询条件
            QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            if (startDate != null) {
                wrapper.between("create_time", startDate, endDate);
            }

            List<AnswerRecord> records = answerRecordMapper.selectList(wrapper);

            // 如果指定了科目，过滤记录
            if (subjectId != null) {
                records = records.stream()
                    .filter(record -> {
                        Question question = questionMapper.selectById(record.getQuestionId());
                        return question != null && subjectId.equals(question.getSubjectId());
                    })
                    .collect(java.util.stream.Collectors.toList());
            }

            // 按科目分组统计
            Map<String, Integer> subjectCount = new HashMap<>();
            Map<String, Double> subjectAccuracy = new HashMap<>();

            // 通过题目关联获取科目信息
            Map<Long, List<AnswerRecord>> subjectRecords = new HashMap<>();

            for (AnswerRecord record : records) {
                Question question = questionMapper.selectById(record.getQuestionId());
                if (question != null) {
                    Long qSubjectId = question.getSubjectId();
                    subjectRecords.computeIfAbsent(qSubjectId, k -> new ArrayList<>()).add(record);
                }
            }

            // 获取科目名称并统计
            for (Map.Entry<Long, List<AnswerRecord>> entry : subjectRecords.entrySet()) {
                Long sid = entry.getKey();
                List<AnswerRecord> subjectAnswers = entry.getValue();

                // 获取科目名称
                Subject subject = subjectMapper.selectById(sid);
                String subjectName = subject != null ? subject.getName() : "未知科目";

                // 统计数量
                subjectCount.put(subjectName, subjectAnswers.size());

                // 计算正确率
                long correctCount = subjectAnswers.stream()
                    .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                    .sum();
                double accuracy = subjectAnswers.isEmpty() ? 0.0 :
                    (double) correctCount / subjectAnswers.size() * 100;
                subjectAccuracy.put(subjectName, Math.round(accuracy * 100) / 100.0);
            }

            subjectData.put("distribution", subjectCount);
            subjectData.put("accuracy", subjectAccuracy);

            return Result.success(subjectData);

        } catch (Exception e) {
            return Result.error("获取科目分布数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getDifficultyAnalysis(Long userId, String dateRange, Long subjectId) {
        Map<String, Object> difficultyData = new HashMap<>();
        
        try {
            // 解析日期范围
            LocalDateTime[] dateRangeArray = parseDateRange(dateRange);
            LocalDateTime startDate = dateRangeArray[0];
            LocalDateTime endDate = dateRangeArray[1];
            
            // 构建查询条件
            QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            if (startDate != null) {
                wrapper.between("create_time", startDate, endDate);
            }
            
            List<AnswerRecord> records = answerRecordMapper.selectList(wrapper);
            
            // 如果指定了科目，过滤记录
            if (subjectId != null) {
                records = records.stream()
                    .filter(record -> {
                        Question question = questionMapper.selectById(record.getQuestionId());
                        return question != null && question.getSubjectId().equals(subjectId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 按难度分组统计
            Map<String, Integer> difficultyCount = new LinkedHashMap<>();
            Map<String, Double> difficultyAccuracy = new LinkedHashMap<>();
            
            // 初始化难度数据
            difficultyCount.put("简单", 0);
            difficultyCount.put("中等", 0);
            difficultyCount.put("困难", 0);
            difficultyAccuracy.put("简单", 0.0);
            difficultyAccuracy.put("中等", 0.0);
            difficultyAccuracy.put("困难", 0.0);
            
            // 按难度分组
            Map<String, List<AnswerRecord>> difficultyRecords = new HashMap<>();
            difficultyRecords.put("简单", new ArrayList<>());
            difficultyRecords.put("中等", new ArrayList<>());
            difficultyRecords.put("困难", new ArrayList<>());
            
            for (AnswerRecord record : records) {
                Question question = questionMapper.selectById(record.getQuestionId());
                if (question != null) {
                    String difficulty;
                    Integer difficultyLevel = question.getDifficulty();
                    if (difficultyLevel == null) {
                        difficulty = "中等"; // 默认中等
                    } else if (difficultyLevel <= 1) {
                        difficulty = "简单";
                    } else if (difficultyLevel <= 2) {
                        difficulty = "中等";
                    } else {
                        difficulty = "困难";
                    }
                    
                    difficultyRecords.get(difficulty).add(record);
                }
            }
            
            // 统计各难度的数量和正确率
            for (Map.Entry<String, List<AnswerRecord>> entry : difficultyRecords.entrySet()) {
                String difficulty = entry.getKey();
                List<AnswerRecord> diffRecords = entry.getValue();
                
                difficultyCount.put(difficulty, diffRecords.size());
                
                if (!diffRecords.isEmpty()) {
                    long correctCount = diffRecords.stream()
                        .mapToLong(record -> record.getIsCorrect() == 1 ? 1 : 0)
                        .sum();
                    double accuracy = (double) correctCount / diffRecords.size() * 100;
                    difficultyAccuracy.put(difficulty, Math.round(accuracy * 100) / 100.0);
                }
            }
            
            difficultyData.put("distribution", difficultyCount);
            difficultyData.put("accuracy", difficultyAccuracy);
            
            return Result.success(difficultyData);
            
        } catch (Exception e) {
            return Result.error("获取难度分析数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getWrongQuestionAnalysis(Long userId, String dateRange, Long subjectId) {
        Map<String, Object> wrongData = new HashMap<>();
        
        try {
            // 构建错题查询条件
            QueryWrapper<WrongQuestion> wrongWrapper = new QueryWrapper<>();
            wrongWrapper.eq("user_id", userId);
            
            // 如果指定了科目，需要通过题目关联过滤
            List<WrongQuestion> wrongQuestions = wrongQuestionMapper.selectList(wrongWrapper);
            
            if (subjectId != null) {
                wrongQuestions = wrongQuestions.stream()
                    .filter(wrongQuestion -> {
                        Question question = questionMapper.selectById(wrongQuestion.getQuestionId());
                        return question != null && question.getSubjectId().equals(subjectId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 总错题数
            int totalWrongCount = wrongQuestions.size();
            
            // 解析日期范围，统计时间段内的错题趋势
            LocalDateTime[] dateRangeArray = parseDateRange(dateRange);
            LocalDateTime startDate = dateRangeArray[0];
            LocalDateTime endDate = dateRangeArray[1];

            // 按日期分组统计错题趋势
            Map<String, Integer> wrongTrend = new LinkedHashMap<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd");

            // 初始化日期数据 - 添加空值检查
            if (startDate != null && endDate != null) {
                LocalDateTime current = startDate;
                while (current != null && current.isBefore(endDate)) {
                    String dateKey = current.format(dateFormatter);
                    wrongTrend.put(dateKey, 0);
                    current = current.plusDays(1);
                }
            }
            
            // 统计指定时间范围内的错题 - 添加空值检查
            if (startDate != null && endDate != null) {
                wrongQuestions.stream()
                    .filter(wq -> wq.getCreateTime() != null &&
                                  wq.getCreateTime().isAfter(startDate) &&
                                  wq.getCreateTime().isBefore(endDate))
                    .forEach(wq -> {
                        String dateKey = wq.getCreateTime().format(dateFormatter);
                        if (wrongTrend.containsKey(dateKey)) {
                            wrongTrend.put(dateKey, wrongTrend.get(dateKey) + 1);
                        }
                    });
            }
            
            // 错题状态分析（简化处理）
            Map<String, Integer> wrongStatus = new HashMap<>();
            wrongStatus.put("待复习", (int) (totalWrongCount * 0.7)); // 假设70%待复习
            wrongStatus.put("已掌握", (int) (totalWrongCount * 0.3)); // 假设30%已掌握
            
            wrongData.put("totalCount", totalWrongCount);
            wrongData.put("trend", wrongTrend);
            wrongData.put("status", wrongStatus);
            
            return Result.success(wrongData);
            
        } catch (Exception e) {
            return Result.error("获取错题分析数据失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getLearningSuggestions(Long userId, String dateRange, Long subjectId) {
        try {
            // 获取概览统计数据
            Result<Map<String, Object>> overviewResult = getOverviewStats(userId, dateRange, subjectId);
            if (overviewResult.getCode() != 200 || overviewResult.getData() == null) {
                return Result.error("获取统计数据失败");
            }
            
            Map<String, Object> overviewStats = overviewResult.getData();
            
            // 获取错题数量
            Result<Map<String, Object>> wrongResult = getWrongQuestionAnalysis(userId, dateRange, subjectId);
            int wrongCount = 0;
            if (wrongResult.getCode() == 200 && wrongResult.getData() != null) {
                Object totalCountObj = wrongResult.getData().get("totalCount");
                if (totalCountObj != null) {
                    wrongCount = (Integer) totalCountObj;
                }
            }
            
            // 生成学习建议
            List<String> suggestions = generateSuggestions(overviewStats, wrongCount);
            
            // 包装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("suggestions", suggestions);
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("获取学习建议失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成学习建议
     */
    private List<String> generateSuggestions(Map<String, Object> overviewStats, int wrongCount) {
        List<String> suggestions = new ArrayList<>();
        
        // 安全获取统计数据，设置默认值
        double correctRate = 0.0;
        int studyDays = 0;
        int totalQuestions = 0;
        
        if (overviewStats != null) {
            Object correctRateObj = overviewStats.get("correctRate");
            Object studyDaysObj = overviewStats.get("studyDays");
            Object totalQuestionsObj = overviewStats.get("totalQuestions");
            
            if (correctRateObj != null) {
                correctRate = (Double) correctRateObj;
            }
            if (studyDaysObj != null) {
                studyDays = (Integer) studyDaysObj;
            }
            if (totalQuestionsObj != null) {
                totalQuestions = (Integer) totalQuestionsObj;
            }
        }
        
        // 基于正确率的建议
        if (correctRate < 60) {
            suggestions.add("正确率偏低，建议加强基础知识学习，多做简单题目巩固基础");
        } else if (correctRate < 80) {
            suggestions.add("正确率良好，继续保持并提高难题解答能力");
        } else {
            suggestions.add("正确率优秀，可以挑战更高难度的题目");
        }
        
        // 基于学习天数的建议
        if (studyDays < 7) {
            suggestions.add("建议保持每日练习的习惯，提高学习连续性");
        } else if (studyDays >= 20) {
            suggestions.add("学习习惯很好，坚持每日练习，效果显著");
        }
        
        // 基于练习量的建议
        if (totalQuestions < 50) {
            suggestions.add("练习量较少，建议增加每日练习题目数量");
        } else if (totalQuestions > 200) {
            suggestions.add("练习量充足，注意劳逸结合，保证学习质量");
        }
        
        // 基于错题数量的建议
        if (wrongCount > 20) {
            suggestions.add("错题较多，建议重点复习错题本中的题目，总结错误原因");
        } else if (wrongCount > 0) {
            suggestions.add("有少量错题，建议定期回顾，避免重复犯错");
        } else {
            suggestions.add("暂无错题，学习状态良好，继续保持");
        }
        
        return suggestions;
    }
}