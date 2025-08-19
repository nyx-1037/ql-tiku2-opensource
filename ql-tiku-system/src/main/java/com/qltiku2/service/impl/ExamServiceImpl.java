package com.qltiku2.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AnswerSubmitRequest;
import com.qltiku2.dto.ExamQueryRequest;
import com.qltiku2.dto.ExamSaveRequest;
import com.qltiku2.dto.SimulationExamRequest;
import com.qltiku2.entity.*;
import com.qltiku2.mapper.*;
import com.qltiku2.service.ExamService;
import com.qltiku2.service.QuestionService;
import com.qltiku2.service.WrongBookService;
import com.qltiku2.vo.ExamVO;
import com.qltiku2.vo.QuestionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试服务实现类
 * 
 * @author qltiku2
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {
    
    @Resource
    private SubjectMapper subjectMapper;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private QuestionService questionService;
    
    @Resource
    private QuestionMapper questionMapper;
    
    @Resource
    private ExamQuestionMapper examQuestionMapper;
    
    @Resource
    private AnswerRecordMapper answerRecordMapper;
    
    @Resource
    private WrongBookService wrongBookService;
    
    @Override
    public IPage<ExamVO> pageExams(ExamQueryRequest queryRequest) {
        // 构建查询条件
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(queryRequest.getKeyword())) {
            queryWrapper.like(Exam::getTitle, queryRequest.getKeyword());
        }
        
        if (queryRequest.getSubjectId() != null) {
            queryWrapper.eq(Exam::getSubjectId, queryRequest.getSubjectId());
        }
        
        if (queryRequest.getStatus() != null) {
            queryWrapper.eq(Exam::getStatus, queryRequest.getStatus());
        }
        
        if (queryRequest.getStartTimeBegin() != null) {
            queryWrapper.ge(Exam::getStartTime, queryRequest.getStartTimeBegin());
        }
        
        if (queryRequest.getStartTimeEnd() != null) {
            queryWrapper.le(Exam::getStartTime, queryRequest.getStartTimeEnd());
        }
        
        if (queryRequest.getCreateBy() != null) {
            queryWrapper.eq(Exam::getCreateBy, queryRequest.getCreateBy());
        }
        
        queryWrapper.orderByDesc(Exam::getCreateTime);
        
        // 分页查询
        Page<Exam> page = new Page<>(queryRequest.getCurrent(), queryRequest.getSize());
        IPage<Exam> examPage = this.page(page, queryWrapper);
        
        // 转换为VO
        return convertToExamVOPage(examPage);
    }
    
    @Override
    public ExamVO getExamById(Long id) {
        Exam exam = this.getById(id);
        if (exam == null) {
            return null;
        }
        return convertToExamVO(exam);
    }
    
    @Override
    public Long createExam(ExamSaveRequest saveRequest, Long createBy) {
        Exam exam = new Exam();
        BeanUtils.copyProperties(saveRequest, exam);
        exam.setCreateBy(createBy);
        
        // 根据时间自动设置状态
        exam.setStatus(determineExamStatus(saveRequest.getStartTime(), saveRequest.getEndTime()));
        
        // 处理题目关联
        List<Long> questionIds = new ArrayList<>();
        
        if (saveRequest.getExamType() == 1) {
            // 自选题目模式
            questionIds = saveRequest.getSelectedQuestionIds();
        } else {
            // 随机题目模式
            if (saveRequest.getQuestionConfigs() != null && !saveRequest.getQuestionConfigs().isEmpty()) {
                for (ExamSaveRequest.QuestionConfig config : saveRequest.getQuestionConfigs()) {
                    // 根据配置随机选择题目
                    LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Question::getSubjectId, saveRequest.getSubjectId())
                               .eq(Question::getQuestionType, config.getQuestionType())
                               .eq(Question::getDifficulty, config.getDifficulty())
                               .eq(Question::getDeleted, 0)
                               .last("ORDER BY RAND() LIMIT " + config.getCount());
                    
                    List<Question> questions = questionMapper.selectList(queryWrapper);
                    questionIds.addAll(questions.stream().map(Question::getId).collect(Collectors.toList()));
                }
            }
        }
        
        // 设置实际题目数量
        exam.setQuestionCount(questionIds.size());
        
        // 保存考试
        this.save(exam);
        
        // 保存题目关联关系
        if (!questionIds.isEmpty()) {
            List<ExamQuestion> examQuestions = new ArrayList<>();
            for (int i = 0; i < questionIds.size(); i++) {
                ExamQuestion examQuestion = new ExamQuestion();
                examQuestion.setExamId(exam.getId());
                examQuestion.setQuestionId(questionIds.get(i));
                examQuestion.setSortOrder(i + 1);
                
                // 设置分值
                if (saveRequest.getExamType() == 1) {
                    // 自选题目模式，平均分配分值
                    examQuestion.setScore(saveRequest.getTotalScore() / questionIds.size());
                } else {
                    // 随机题目模式，根据配置设置分值
                    Question question = questionMapper.selectById(questionIds.get(i));
                    for (ExamSaveRequest.QuestionConfig config : saveRequest.getQuestionConfigs()) {
                        if (config.getQuestionType().equals(question.getQuestionType()) && 
                            config.getDifficulty().equals(question.getDifficulty())) {
                            examQuestion.setScore(config.getScore());
                            break;
                        }
                    }
                }
                
                examQuestion.setCreateTime(LocalDateTime.now());
                examQuestions.add(examQuestion);
            }
            
            // 批量插入题目关联
            for (ExamQuestion examQuestion : examQuestions) {
                examQuestionMapper.insert(examQuestion);
            }
        }
        
        return exam.getId();
    }
    
    @Override
    public boolean updateExam(ExamSaveRequest saveRequest) {
        if (saveRequest.getId() == null) {
            throw new IllegalArgumentException("考试ID不能为空");
        }
        
        Exam exam = this.getById(saveRequest.getId());
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        
        BeanUtils.copyProperties(saveRequest, exam);
        
        // 根据时间自动设置状态
        if (saveRequest.getStartTime() != null && saveRequest.getEndTime() != null) {
            exam.setStatus(determineExamStatus(saveRequest.getStartTime(), saveRequest.getEndTime()));
        }
        
        // 更新题目关联关系
        if (saveRequest.getExamType() != null) {
            // 先删除原有的题目关联
            LambdaQueryWrapper<ExamQuestion> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(ExamQuestion::getExamId, exam.getId());
            examQuestionMapper.delete(deleteWrapper);
            
            // 重新添加题目关联
            List<Long> questionIds = new ArrayList<>();
            
            if (saveRequest.getExamType() == 0) {
                // 随机题目模式
                for (ExamSaveRequest.QuestionConfig config : saveRequest.getQuestionConfigs()) {
                    LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Question::getSubjectId, saveRequest.getSubjectId())
                            .eq(Question::getQuestionType, config.getQuestionType())
                            .eq(Question::getDifficulty, config.getDifficulty())
                            .eq(Question::getDeleted, 0)
                            .last("ORDER BY RAND() LIMIT " + config.getCount());
                    
                    List<Question> questions = questionMapper.selectList(queryWrapper);
                    questionIds.addAll(questions.stream().map(Question::getId).collect(Collectors.toList()));
                }
            } else {
                // 自选题目模式
                questionIds = saveRequest.getSelectedQuestionIds();
            }
            
            // 设置题目数量
            exam.setQuestionCount(questionIds.size());
            
            // 保存题目关联
            List<ExamQuestion> examQuestions = new ArrayList<>();
            for (int i = 0; i < questionIds.size(); i++) {
                ExamQuestion examQuestion = new ExamQuestion();
                examQuestion.setExamId(exam.getId());
                examQuestion.setQuestionId(questionIds.get(i));
                examQuestion.setSortOrder(i + 1);
                examQuestion.setCreateTime(LocalDateTime.now());
                
                if (saveRequest.getExamType() == 1) {
                    // 自选题目模式，使用统一分值
                    examQuestion.setScore(saveRequest.getTotalScore() / questionIds.size());
                } else {
                    // 随机题目模式，根据配置设置分值
                    Question question = questionMapper.selectById(questionIds.get(i));
                    for (ExamSaveRequest.QuestionConfig config : saveRequest.getQuestionConfigs()) {
                        if (config.getQuestionType().equals(question.getQuestionType()) && 
                            config.getDifficulty().equals(question.getDifficulty())) {
                            examQuestion.setScore(config.getScore());
                            break;
                        }
                    }
                }
                
                examQuestions.add(examQuestion);
            }
            
            // 批量插入题目关联
            for (ExamQuestion examQuestion : examQuestions) {
                examQuestionMapper.insert(examQuestion);
            }
        }
        
        return this.updateById(exam);
    }
    
    @Override
    public boolean deleteExam(Long id) {
        return this.removeById(id);
    }
    
    @Override
    public boolean batchDeleteExams(List<Long> ids) {
        return this.removeByIds(ids);
    }
    
    @Override
    public boolean updateExamStatus(Long id, Integer status) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setStatus(status);
        return this.updateById(exam);
    }
    
    @Override
    public List<ExamVO> listExams(Long subjectId) {
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        
        if (subjectId != null) {
            queryWrapper.eq(Exam::getSubjectId, subjectId);
        }
        
        queryWrapper.orderByDesc(Exam::getCreateTime);
        
        List<Exam> exams = this.list(queryWrapper);
        return exams.stream().map(this::convertToExamVO).collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(Long id) {
        return this.getById(id) != null;
    }
    
    @Override
    public Exam getExamEntityById(Long id) {
        return this.getById(id);
    }
    
    /**
     * 转换为ExamVO分页对象
     */
    private IPage<ExamVO> convertToExamVOPage(IPage<Exam> examPage) {
        Page<ExamVO> voPage = new Page<>(examPage.getCurrent(), examPage.getSize(), examPage.getTotal());
        
        List<ExamVO> voList = examPage.getRecords().stream()
                .map(this::convertToExamVO)
                .collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    /**
     * 转换为ExamVO对象
     */
    private ExamVO convertToExamVO(Exam exam) {
        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        
        // 设置科目名称
        if (exam.getSubjectId() != null) {
            Subject subject = subjectMapper.selectById(exam.getSubjectId());
            if (subject != null) {
                vo.setSubjectName(subject.getName());
            }
        }
        
        // 设置创建人姓名
        if (exam.getCreateBy() != null) {
            SysUser user = sysUserMapper.selectById(exam.getCreateBy());
            if (user != null) {
                vo.setCreateByName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        
        // 设置状态描述
        vo.setStatus(exam.getStatus());
        
        // 设置考试类型
        vo.setExamType(exam.getExamType());
        
        // 设置题目数量（查询 exam_question 表）
        if (exam.getId() != null) {
            LambdaQueryWrapper<ExamQuestion> questionWrapper = new LambdaQueryWrapper<>();
            questionWrapper.eq(ExamQuestion::getExamId, exam.getId());
            Long questionCount = examQuestionMapper.selectCount(questionWrapper);
            vo.setQuestionCount(questionCount != null ? questionCount.intValue() : 0);
        } else {
            vo.setQuestionCount(0);
        }
        
        // TODO: 设置参与人数和平均分（需要统计答题记录）
        vo.setParticipantCount(0);
        vo.setAverageScore(0.0);
        
        return vo;
    }
    
    /**
     * 根据开始时间和结束时间确定考试状态
     */
    private Integer determineExamStatus(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        if (now.isBefore(startTime)) {
            return 0; // 未开始
        } else if (now.isAfter(endTime)) {
            return 2; // 已结束
        } else {
            return 1; // 进行中
        }
    }
    
    @Override
    public List<ExamVO> listAvailableExams(Long subjectId) {
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        
        // 只查询状态为启用的考试
        queryWrapper.eq(Exam::getStatus, 1);
        queryWrapper.eq(Exam::getDeleted, 0);
        
        if (subjectId != null) {
            queryWrapper.eq(Exam::getSubjectId, subjectId);
        }
        
        // 只显示当前时间可以参加的考试
        LocalDateTime now = LocalDateTime.now();
        queryWrapper.le(Exam::getStartTime, now)
                   .ge(Exam::getEndTime, now);
        
        queryWrapper.orderByDesc(Exam::getCreateTime);
        
        List<Exam> exams = this.list(queryWrapper);
        return exams.stream().map(this::convertToExamVO).collect(Collectors.toList());
    }
    
    @Override
    public List<ExamVO> listFixedPapers(Long subjectId) {
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查询固定试卷和真题试卷（exam_type=0或1）
        queryWrapper.in(Exam::getExamType, 0, 1); // 0表示模拟试卷，1表示真题试卷
        queryWrapper.eq(Exam::getStatus, 1);
        queryWrapper.eq(Exam::getDeleted, 0);
        
        if (subjectId != null) {
            queryWrapper.eq(Exam::getSubjectId, subjectId);
        }
        
        queryWrapper.orderByDesc(Exam::getCreateTime);
        
        List<Exam> exams = this.list(queryWrapper);
        return exams.stream().map(this::convertToExamVO).collect(Collectors.toList());
    }
    
    @Override
    public IPage<ExamVO> pageFixedPapers(Integer current, Integer size, Long subjectId, String keyword) {
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查询固定试卷和真题试卷（exam_type=0或1）
        queryWrapper.in(Exam::getExamType, 0, 1); // 0表示模拟试卷，1表示真题试卷
        queryWrapper.eq(Exam::getStatus, 1);
        queryWrapper.eq(Exam::getDeleted, 0);
        
        // 科目筛选
        if (subjectId != null) {
            queryWrapper.eq(Exam::getSubjectId, subjectId);
        }
        
        // 关键词模糊查询（标题和描述）
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> 
                wrapper.like(Exam::getTitle, keyword)
                       .or()
                       .like(Exam::getDescription, keyword)
            );
        }
        
        queryWrapper.orderByDesc(Exam::getCreateTime);
        
        // 分页查询
        Page<Exam> page = new Page<>(current, size);
        IPage<Exam> examPage = this.page(page, queryWrapper);
        
        // 转换为ExamVO分页对象
        return convertToExamVOPage(examPage);
    }
    
    @Override
    public Map<String, Object> startFixedPaperExam(Long examId, Long userId) {
        // 检查固定试卷是否存在
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new RuntimeException("固定试卷不存在");
        }
        
        // 检查是否为固定试卷类型（支持模拟试卷和真题试卷）
        if (exam.getExamType() == null || (exam.getExamType() != 0 && exam.getExamType() != 1)) {
            throw new RuntimeException("该考试不是固定试卷");
        }
        
        // 检查试卷状态
        if (exam.getStatus() != 1) {
            throw new RuntimeException("固定试卷未启用");
        }
        
        // 获取考试题目
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, examId)
                          .orderByAsc(ExamQuestion::getSortOrder);
        
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        if (examQuestions.isEmpty()) {
            throw new RuntimeException("固定试卷暂无题目");
        }
        
        // 获取题目详情
        List<Long> questionIds = examQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toList());
        
        List<QuestionVO> questions = new ArrayList<>();
        for (Long questionId : questionIds) {
            try {
                // 直接从数据库获取题目，然后转换为VO
                Question question = questionMapper.selectById(questionId);
                if (question != null) {
                    QuestionVO questionVO = convertToQuestionVO(question);
                    questions.add(questionVO);
                } else {
                    System.err.println("获取题目详情失败，题目ID: " + questionId + ", 题目不存在");
                }
            } catch (Exception e) {
                System.err.println("获取题目详情失败，题目ID: " + questionId + ", 错误: " + e.getMessage());
                e.printStackTrace();
                // 跳过有问题的题目，继续处理其他题目
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examTitle", exam.getTitle());
        result.put("duration", exam.getDuration());
        result.put("totalScore", exam.getTotalScore());
        result.put("questionCount", questions.size());
        result.put("questions", questions);
        result.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    @Override
    public Map<String, Object> submitFixedPaperExam(Long examId, Long userId, List<AnswerSubmitRequest> answers) {
        // 检查固定试卷是否存在
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new RuntimeException("固定试卷不存在");
        }
        
        // 检查是否为固定试卷类型（支持模拟试卷和真题试卷）
        if (exam.getExamType() == null || (exam.getExamType() != 0 && exam.getExamType() != 1)) {
            throw new RuntimeException("该考试不是固定试卷");
        }
        
        // 获取考试的所有题目
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, examId)
                          .orderByAsc(ExamQuestion::getSortOrder);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        
        if (examQuestions.isEmpty()) {
            throw new RuntimeException("固定试卷暂无题目");
        }
        
        // 创建用户答案映射
        Map<Long, AnswerSubmitRequest> answerMap = new HashMap<>();
        if (answers != null) {
            answerMap = answers.stream()
                    .collect(Collectors.toMap(AnswerSubmitRequest::getQuestionId, answer -> answer));
        }
        
        int totalScore = 0;
        int correctCount = 0;
        
        // 为所有题目创建答题记录
        for (ExamQuestion examQuestion : examQuestions) {
            AnswerRecord record = new AnswerRecord();
            record.setUserId(userId);
            record.setQuestionId(examQuestion.getQuestionId());
            record.setExamId(examId);
            record.setPracticeType(4); // 4表示固定试卷考试模式
            
            // 获取用户答案
            AnswerSubmitRequest userAnswer = answerMap.get(examQuestion.getQuestionId());
            if (userAnswer != null) {
                record.setUserAnswer(userAnswer.getUserAnswer());
                record.setAnswerTime(userAnswer.getAnswerTime());
            } else {
                // 未答题目
                record.setUserAnswer("");
                record.setAnswerTime(0);
            }
            
            // 获取题目信息进行判分
            try {
                // 直接从数据库获取题目信息，避免类型转换问题
                Question question = questionMapper.selectById(examQuestion.getQuestionId());
                if (question != null) {
                    boolean isCorrect = false;
                    if (userAnswer != null && userAnswer.getUserAnswer() != null && !userAnswer.getUserAnswer().trim().isEmpty()) {
                        isCorrect = checkAnswerByQuestion(question, userAnswer.getUserAnswer());
                    }
                    
                    record.setIsCorrect(isCorrect ? 1 : 0);
                    record.setScore(isCorrect ? examQuestion.getScore() : 0);
                    
                    totalScore += record.getScore();
                    if (isCorrect) {
                        correctCount++;
                    } else {
                        // 添加到错题本（包括未答题目）
                        try {
                            String wrongAnswer = userAnswer != null ? userAnswer.getUserAnswer() : "";
                            wrongBookService.addWrongQuestion(userId, examQuestion.getQuestionId(), wrongAnswer, "EXAM_FIXED");
                        } catch (Exception e) {
                            // 记录日志但不影响主流程
                            System.err.println("添加错题失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 如果无法获取题目信息，标记为错误
                    record.setIsCorrect(0);
                    record.setScore(0);
                    System.err.println("获取题目信息失败，题目ID: " + examQuestion.getQuestionId() + ", 题目不存在");
                }
            } catch (Exception e) {
                System.err.println("获取题目信息失败，题目ID: " + examQuestion.getQuestionId() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                // 如果无法获取题目信息，标记为错误
                record.setIsCorrect(0);
                record.setScore(0);
            }
            
            record.setCreateTime(LocalDateTime.now());
            answerRecordMapper.insert(record);
        }
        
        // 计算准确率和是否通过（基于总题目数）
        int totalQuestions = examQuestions.size();
        double accuracy = totalQuestions > 0 ? (double) correctCount / totalQuestions * 100 : 0;
        boolean passed = totalScore >= exam.getPassScore();
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examTitle", exam.getTitle());
        result.put("totalScore", totalScore);
        result.put("maxScore", exam.getTotalScore());
        result.put("correctCount", correctCount);
        result.put("totalCount", totalQuestions);
        result.put("accuracy", Math.round(accuracy * 100.0) / 100.0);
        result.put("passed", passed);
        result.put("passScore", exam.getPassScore());
        result.put("submitTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    @Override
    public Map<String, Object> startExam(Long examId, Long userId) {
        // 检查考试是否存在
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        
        // 检查考试状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }
        
        // 检查用户是否已经参加过此考试
        LambdaQueryWrapper<AnswerRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getExamId, examId)
                    .eq(AnswerRecord::getPracticeType, 2); // 2表示考试模式
        
        List<AnswerRecord> existingRecords = answerRecordMapper.selectList(recordWrapper);
        if (!existingRecords.isEmpty()) {
            throw new RuntimeException("您已经参加过此考试");
        }
        
        // 获取考试题目
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, examId)
                          .orderByAsc(ExamQuestion::getSortOrder);
        
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        if (examQuestions.isEmpty()) {
            throw new RuntimeException("考试暂无题目");
        }
        
        // 获取题目详情
        List<Long> questionIds = examQuestions.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toList());
        
        List<QuestionVO> questions = new ArrayList<>();
        for (Long questionId : questionIds) {
            try {
                com.qltiku2.common.Result<QuestionVO> result = questionService.getQuestionById(questionId);
                if (result != null && result.getData() != null) {
                    questions.add(result.getData());
                }
            } catch (Exception e) {
                System.err.println("获取题目详情失败，题目ID: " + questionId + ", 错误: " + e.getMessage());
                // 跳过有问题的题目，继续处理其他题目
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examTitle", exam.getTitle());
        result.put("duration", exam.getDuration());
        result.put("totalScore", exam.getTotalScore());
        result.put("questionCount", questions.size());
        result.put("questions", questions);
        result.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    @Override
    public Map<String, Object> submitExam(Long examId, Long userId, List<AnswerSubmitRequest> answers) {
        // 检查考试是否存在
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        
        // 允许重复考试，不检查是否已经提交过
        // 如果需要记录每次考试记录，可以保留之前的记录，但不再阻止提交
        
        // 获取考试的所有题目
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, examId)
                          .orderByAsc(ExamQuestion::getSortOrder);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        
        if (examQuestions.isEmpty()) {
            throw new RuntimeException("考试暂无题目");
        }
        
        // 创建用户答案映射
        Map<Long, AnswerSubmitRequest> answerMap = new HashMap<>();
        if (answers != null) {
            answerMap = answers.stream()
                    .collect(Collectors.toMap(AnswerSubmitRequest::getQuestionId, answer -> answer));
        }
        
        int totalScore = 0;
        int correctCount = 0;
        int totalQuestions = examQuestions.size();
        
        // 为所有题目创建答题记录
        for (ExamQuestion examQuestion : examQuestions) {
            AnswerRecord record = new AnswerRecord();
            record.setUserId(userId);
            record.setQuestionId(examQuestion.getQuestionId());
            record.setExamId(examId);
            record.setPracticeType(2); // 考试模式
            
            // 获取用户答案
            AnswerSubmitRequest userAnswer = answerMap.get(examQuestion.getQuestionId());
            if (userAnswer != null) {
                record.setUserAnswer(userAnswer.getUserAnswer());
                record.setAnswerTime(userAnswer.getAnswerTime());
            } else {
                // 未答题目
                record.setUserAnswer("");
                record.setAnswerTime(0);
            }
            
            // 获取题目信息进行判分
            // 获取题目信息进行判分
            try {
                // 直接从数据库获取题目信息，避免类型转换问题
                Question question = questionMapper.selectById(examQuestion.getQuestionId());
                if (question != null) {
                    boolean isCorrect = false;
                    if (userAnswer != null && userAnswer.getUserAnswer() != null && !userAnswer.getUserAnswer().trim().isEmpty()) {
                        isCorrect = checkAnswerByQuestion(question, userAnswer.getUserAnswer());
                    }
                    
                    record.setIsCorrect(isCorrect ? 1 : 0);
                    
                    if (isCorrect) {
                        correctCount++;
                        // 简单计分：每题分值 = 总分 / 题目数
                        record.setScore(exam.getTotalScore() / totalQuestions);
                        totalScore += record.getScore();
                    } else {
                        record.setScore(0);
                        // 添加到错题本（包括未答题目）
                        try {
                            String wrongAnswer = userAnswer != null ? userAnswer.getUserAnswer() : "";
                            wrongBookService.addWrongQuestion(userId, examQuestion.getQuestionId(), wrongAnswer, "EXAM_CUSTOM");
                        } catch (Exception e) {
                            // 记录日志但不影响主流程
                            System.err.println("添加错题失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 如果无法获取题目信息，标记为错误
                    record.setIsCorrect(0);
                    record.setScore(0);
                    System.err.println("获取题目信息失败，题目ID: " + examQuestion.getQuestionId() + ", 题目不存在");
                }
            } catch (Exception e) {
                System.err.println("获取题目信息失败，题目ID: " + examQuestion.getQuestionId() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                record.setIsCorrect(0);
                record.setScore(0);
            }
            
            answerRecordMapper.insert(record);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("totalScore", totalScore);
        result.put("correctCount", correctCount);
        result.put("totalCount", totalQuestions);
        result.put("accuracy", totalQuestions > 0 ? (double) correctCount / totalQuestions * 100 : 0);
        result.put("passed", totalScore >= exam.getPassScore());
        result.put("submitTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    @Override
    public Map<String, Object> getExamResult(Long examId, Long userId) {
        // 查询答题记录
        LambdaQueryWrapper<AnswerRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getExamId, examId)
                    .eq(AnswerRecord::getPracticeType, 2);
        
        List<AnswerRecord> records = answerRecordMapper.selectList(recordWrapper);
        if (records.isEmpty()) {
            throw new RuntimeException("未找到考试记录");
        }
        
        Exam exam = this.getById(examId);
        
        int totalScore = records.stream().mapToInt(AnswerRecord::getScore).sum();
        int correctCount = (int) records.stream().filter(r -> r.getIsCorrect() == 1).count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examTitle", exam.getTitle());
        result.put("totalScore", totalScore);
        result.put("correctCount", correctCount);
        result.put("totalCount", records.size());
        result.put("accuracy", records.size() > 0 ? (double) correctCount / records.size() * 100 : 0);
        result.put("passed", totalScore >= exam.getPassScore());
        result.put("records", records);
        
        return result;
    }
    
    @Override
    public IPage<Map<String, Object>> getUserExamRecords(Long userId, Integer current, Integer size, String keyword, Long subjectId, Integer status) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        // 1. 查询正式考试记录
        LambdaQueryWrapper<AnswerRecord> examRecordWrapper = new LambdaQueryWrapper<>();
        examRecordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getPracticeType, 2)
                    .isNotNull(AnswerRecord::getExamId)
                    .orderByDesc(AnswerRecord::getCreateTime);
        
        List<AnswerRecord> examRecords = answerRecordMapper.selectList(examRecordWrapper);
        
        // 按考试分组统计正式考试
        Map<Long, List<AnswerRecord>> examRecordsMap = examRecords.stream()
                .collect(Collectors.groupingBy(AnswerRecord::getExamId));
        
        for (Map.Entry<Long, List<AnswerRecord>> entry : examRecordsMap.entrySet()) {
            Long examId = entry.getKey();
            List<AnswerRecord> records = entry.getValue();
            
            Exam exam = this.getById(examId);
            if (exam != null) {
                // 应用搜索过滤
                boolean matchesKeyword = keyword == null || keyword.trim().isEmpty() || 
                    exam.getTitle().toLowerCase().contains(keyword.toLowerCase());
                boolean matchesSubject = subjectId == null || exam.getSubjectId().equals(subjectId);
                
                int totalScore = records.stream().mapToInt(AnswerRecord::getScore).sum();
                int correctCount = (int) records.stream().filter(r -> r.getIsCorrect() == 1).count();
                boolean passed = totalScore >= exam.getPassScore();
                
                // 应用状态过滤 (0: 未通过, 1: 已通过)
                boolean matchesStatus = status == null || 
                    (status == 1 && passed) || (status == 0 && !passed);
                
                if (matchesKeyword && matchesSubject && matchesStatus) {
                    Map<String, Object> examResult = new HashMap<>();
                    examResult.put("examId", examId);
                    examResult.put("examTitle", exam.getTitle());
                    examResult.put("subjectId", exam.getSubjectId());
                    examResult.put("totalScore", totalScore);
                    examResult.put("correctCount", correctCount);
                    examResult.put("totalCount", records.size());
                    examResult.put("accuracy", records.size() > 0 ? (double) correctCount / records.size() * 100 : 0);
                    examResult.put("passed", passed);
                    examResult.put("status", passed ? 1 : 0);
                    examResult.put("submitTime", records.get(0).getCreateTime());
                    examResult.put("examType", "formal"); // 标记为正式考试
                    
                    // 添加考试类型文本信息
                    String examTypeText = "固定试卷";
                    if (exam.getExamType() != null) {
                        if (exam.getExamType() == 0) {
                            examTypeText = "模拟试卷";
                        } else if (exam.getExamType() == 1) {
                            examTypeText = "真题试卷";
                        }
                    }
                    examResult.put("examTypeText", examTypeText);
                    
                    // 获取科目名称
                    Subject subject = subjectMapper.selectById(exam.getSubjectId());
                    examResult.put("subjectName", subject != null ? subject.getName() : "未知科目");
                    
                    resultList.add(examResult);
                }
            }
        }
        
        // 2. 查询模拟考试记录
        LambdaQueryWrapper<AnswerRecord> simulationRecordWrapper = new LambdaQueryWrapper<>();
        simulationRecordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getPracticeType, 3) // 3表示模拟考试
                    .eq(AnswerRecord::getExamId, -1L) // 模拟考试使用-1作为examId
                    .isNotNull(AnswerRecord::getRemark) // remark存储模拟考试ID
                    .orderByDesc(AnswerRecord::getCreateTime);
        
        List<AnswerRecord> simulationRecords = answerRecordMapper.selectList(simulationRecordWrapper);
        
        // 按模拟考试ID分组统计
        Map<String, List<AnswerRecord>> simulationRecordsMap = simulationRecords.stream()
                .collect(Collectors.groupingBy(AnswerRecord::getRemark));
        
        for (Map.Entry<String, List<AnswerRecord>> entry : simulationRecordsMap.entrySet()) {
            String simulationExamId = entry.getKey();
            List<AnswerRecord> records = entry.getValue();
            
            if (!records.isEmpty()) {
                // 获取科目信息（从第一个题目获取）
                String subjectName = "模拟考试";
                Long recordSubjectId = null;
                try {
                    com.qltiku2.common.Result<QuestionVO> questionResult = questionService.getQuestionById(records.get(0).getQuestionId());
                    if (questionResult != null && questionResult.getCode() == 200 && questionResult.getData() != null) {
                        QuestionVO firstQuestion = questionResult.getData();
                        if (firstQuestion != null) {
                            Subject subject = subjectMapper.selectById(firstQuestion.getSubjectId());
                            if (subject != null) {
                                subjectName = subject.getName() + " - 模拟考试";
                                recordSubjectId = subject.getId();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("获取模拟考试科目信息失败: " + e.getMessage());
                }
                
                // 应用搜索过滤
                boolean matchesKeyword = keyword == null || keyword.trim().isEmpty() || 
                    subjectName.toLowerCase().contains(keyword.toLowerCase());
                boolean matchesSubject = subjectId == null || (recordSubjectId != null && recordSubjectId.equals(subjectId));
                
                int totalScore = records.stream().mapToInt(AnswerRecord::getScore).sum();
                int correctCount = (int) records.stream().filter(r -> r.getIsCorrect() == 1).count();
                int passScore = records.size() * 6; // 60%及格
                boolean passed = totalScore >= passScore;
                
                // 应用状态过滤 (0: 未通过, 1: 已通过)
                boolean matchesStatus = status == null || 
                    (status == 1 && passed) || (status == 0 && !passed);
                
                if (matchesKeyword && matchesSubject && matchesStatus) {
                    Map<String, Object> examResult = new HashMap<>();
                    examResult.put("examId", "simulation_" + simulationExamId); // 添加前缀区分模拟考试
                    examResult.put("recordId", "simulation_" + simulationExamId); // 添加recordId字段，与examId保持一致
                    examResult.put("examTitle", subjectName);
                    examResult.put("subjectId", recordSubjectId);
                    examResult.put("totalScore", totalScore);
                    examResult.put("correctCount", correctCount);
                    examResult.put("totalCount", records.size());
                    examResult.put("accuracy", records.size() > 0 ? (double) correctCount / records.size() * 100 : 0);
                    examResult.put("passed", passed);
                    examResult.put("status", passed ? 1 : 0);
                    examResult.put("submitTime", records.get(0).getCreateTime());
                    examResult.put("examType", "simulation"); // 标记为模拟考试
                    examResult.put("subjectName", subjectName);
                    
                    resultList.add(examResult);
                }
            }
        }
        
        // 按提交时间排序（最新的在前）
        resultList.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("submitTime");
            LocalDateTime timeB = (LocalDateTime) b.get("submitTime");
            return timeB.compareTo(timeA);
        });
        
        // 分页处理
        int total = resultList.size();
        int start = (current - 1) * size;
        int end = Math.min(start + size, total);
        
        List<Map<String, Object>> pagedList = new ArrayList<>();
        if (start < total) {
            pagedList = resultList.subList(start, end);
        }
        
        Page<Map<String, Object>> resultPage = new Page<>(current, size, total);
        resultPage.setRecords(pagedList);
        
        return resultPage;
    }
    
    @Override
    public List<Map<String, Object>> getUserExamRecords(Long examId, Long userId) {
        LambdaQueryWrapper<AnswerRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getExamId, examId)
                    .eq(AnswerRecord::getPracticeType, 2)
                    .orderByDesc(AnswerRecord::getCreateTime);
        
        List<AnswerRecord> allRecords = answerRecordMapper.selectList(recordWrapper);
        if (allRecords.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按创建时间分组
        Map<LocalDateTime, List<AnswerRecord>> groupedRecords = allRecords.stream()
                .collect(Collectors.groupingBy(AnswerRecord::getCreateTime));
        
        // 返回所有考试记录批次
        List<Map<String, Object>> result = new ArrayList<>();
        List<LocalDateTime> sortedTimes = groupedRecords.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        
        for (int i = 0; i < sortedTimes.size(); i++) {
            LocalDateTime recordTime = sortedTimes.get(i);
            List<AnswerRecord> records = groupedRecords.get(recordTime);
            
            if (!records.isEmpty()) {
                Map<String, Object> recordInfo = new HashMap<>();
                recordInfo.put("recordTime", recordTime);
                recordInfo.put("recordIndex", i + 1);
                recordInfo.put("totalQuestions", records.size());
                recordInfo.put("correctCount", records.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                        .count());
                recordInfo.put("score", records.stream()
                        .mapToInt(AnswerRecord::getScore)
                        .sum());
                recordInfo.put("negativeRecordId", -(i + 1)); // 用于获取特定记录的负数ID
                result.add(recordInfo);
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getExamRecordDetail(Long recordId, Long userId) {
        // 查询所有考试记录（不按examId过滤，因为recordId为负数时是批次索引）
        LambdaQueryWrapper<AnswerRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getPracticeType, 2)
                    .orderByDesc(AnswerRecord::getCreateTime)
                    .orderByAsc(AnswerRecord::getQuestionId);
        
        List<AnswerRecord> allRecords = answerRecordMapper.selectList(recordWrapper);
        if (allRecords.isEmpty()) {
            throw new RuntimeException("未找到考试记录");
        }
        
        // 按考试ID分组，然后按创建时间分组
        Map<Long, Map<LocalDateTime, List<AnswerRecord>>> examGroupedRecords = allRecords.stream()
                .collect(Collectors.groupingBy(AnswerRecord::getExamId, 
                        Collectors.groupingBy(AnswerRecord::getCreateTime)));
        
        List<AnswerRecord> records;
        Long actualExamId;
        
        if (recordId < 0) {
            // 当recordId为负数时，先找到对应的考试ID和批次
            // 获取所有考试ID
            List<Long> examIds = new ArrayList<>(examGroupedRecords.keySet());
            
            // 对每个考试ID按创建时间排序
            Map<Long, List<LocalDateTime>> examTimesMap = new HashMap<>();
            for (Long examId : examIds) {
                List<LocalDateTime> times = examGroupedRecords.get(examId).keySet().stream()
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());
                examTimesMap.put(examId, times);
            }
            
            // 创建扁平化的记录列表
            List<Map.Entry<Long, LocalDateTime>> allExamRecords = new ArrayList<>();
            for (Map.Entry<Long, List<LocalDateTime>> entry : examTimesMap.entrySet()) {
                Long examId = entry.getKey();
                for (LocalDateTime time : entry.getValue()) {
                    allExamRecords.add(new AbstractMap.SimpleEntry<>(examId, time));
                }
            }
            
            // 按时间降序排序
            allExamRecords.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            // 使用负数索引获取特定批次的记录
            int index = Math.abs(recordId.intValue()) - 1;
            if (index >= 0 && index < allExamRecords.size()) {
                Map.Entry<Long, LocalDateTime> targetRecord = allExamRecords.get(index);
                actualExamId = targetRecord.getKey();
                LocalDateTime targetTime = targetRecord.getValue();
                records = examGroupedRecords.get(actualExamId).get(targetTime);
            } else {
                // 如果索引超出范围，使用最新的记录
                Map.Entry<Long, LocalDateTime> latestRecord = allExamRecords.get(0);
                actualExamId = latestRecord.getKey();
                LocalDateTime latestTime = latestRecord.getValue();
                records = examGroupedRecords.get(actualExamId).get(latestTime);
            }
        } else {
            // 当recordId为正数时，直接使用它作为考试ID
            actualExamId = recordId;
            
            // 获取该考试ID的所有记录
            if (!examGroupedRecords.containsKey(actualExamId)) {
                throw new RuntimeException("未找到指定考试的记录");
            }
            
            Map<LocalDateTime, List<AnswerRecord>> timeGroupedRecords = examGroupedRecords.get(actualExamId);
            
            // 获取最新的记录批次
            LocalDateTime latestTime = timeGroupedRecords.keySet().stream()
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            
            if (latestTime == null) {
                throw new RuntimeException("未找到有效的考试记录");
            }
            
            records = timeGroupedRecords.get(latestTime);
        }
        
        // 确保记录不为空
        if (records == null || records.isEmpty()) {
            throw new RuntimeException("未找到有效的考试记录");
        }
        
        // 获取考试信息
        Exam exam = this.getById(actualExamId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        
        // 获取科目信息
        Subject subject = subjectMapper.selectById(exam.getSubjectId());
        
        // 获取考试的实际题目总数（从 exam_question 表查询）
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, actualExamId);
        Long actualTotalCount = examQuestionMapper.selectCount(examQuestionWrapper);
        int totalCount = actualTotalCount != null ? actualTotalCount.intValue() : records.size();
        
        // 统计考试结果
        int totalScore = records.stream().mapToInt(AnswerRecord::getScore).sum();
        int correctCount = (int) records.stream().filter(r -> r.getIsCorrect() == 1).count();
        double accuracy = totalCount > 0 ? (double) correctCount / totalCount * 100 : 0;
        boolean passed = totalScore >= exam.getPassScore();
        
        // 获取考试的所有题目（包括未答题目）
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        
        // 创建答题记录映射，便于查找
        Map<Long, AnswerRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AnswerRecord::getQuestionId, record -> record));
        
        // 获取详细答题记录（包含所有题目）
        // 获取详细答题记录（包含所有题目）
        List<Map<String, Object>> answerDetails = new ArrayList<>();
        for (ExamQuestion examQuestion : examQuestions) {
            try {
                // 直接从数据库获取题目信息，避免类型转换问题
                Question question = questionMapper.selectById(examQuestion.getQuestionId());
                if (question != null) {
                    QuestionVO questionVO = convertToQuestionVO(question);
                    AnswerRecord record = recordMap.get(examQuestion.getQuestionId());
                
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", examQuestion.getQuestionId());
                    detail.put("questionContent", questionVO.getContent());
                    detail.put("questionType", questionVO.getQuestionType());
                    detail.put("options", questionVO.getOptionList()); // 使用解析后的选项列表
                    detail.put("correctAnswer", questionVO.getCorrectAnswer());
                    detail.put("explanation", questionVO.getAnalysis());
                    detail.put("questionImages", question.getImages() != null ? question.getImages() : "");
                    
                    if (record != null) {
                        // 有答题记录
                        detail.put("userAnswer", record.getUserAnswer());
                        detail.put("isCorrect", record.getIsCorrect() == 1);
                        detail.put("score", record.getScore());
                        detail.put("answerTime", record.getAnswerTime());
                    } else {
                        // 未答题目，标记为错误
                        detail.put("userAnswer", "");
                        detail.put("isCorrect", false);
                        detail.put("score", 0);
                        detail.put("answerTime", 0);
                    }
                    
                    answerDetails.add(detail);
                } else {
                    System.err.println("获取考试记录详情题目失败，题目ID: " + examQuestion.getQuestionId() + ", 题目不存在");
                }
            } catch (Exception e) {
                System.err.println("获取考试记录详情题目失败，题目ID: " + examQuestion.getQuestionId() + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", actualExamId);
        result.put("examTitle", exam.getTitle());
        result.put("examDescription", exam.getDescription());
        result.put("subjectId", exam.getSubjectId());
        result.put("subjectName", subject != null ? subject.getName() : "未知科目");
        result.put("duration", exam.getDuration());
        result.put("totalScore", totalScore);
        result.put("passScore", exam.getPassScore());
        result.put("correctCount", correctCount);
        result.put("totalCount", totalCount);
        result.put("accuracy", accuracy);
        result.put("passed", passed);
        result.put("status", passed ? 1 : 0);
        result.put("submitTime", records.get(0).getCreateTime());
        result.put("answerDetails", answerDetails);
        
        return result;
    }
    
/**
     * 获取模拟考试记录详情
     */
    public Map<String, Object> getSimulationExamRecordDetail(String examId, Long userId) {
        // 查询模拟考试记录，直接使用examId查询remark字段
        LambdaQueryWrapper<AnswerRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getExamId, -1L) // 模拟考试使用-1作为examId
                    .eq(AnswerRecord::getPracticeType, 3) // 3表示模拟考试模式
                    .eq(AnswerRecord::getRemark, examId) // 直接使用examId查询remark字段
                     .orderByAsc(AnswerRecord::getQuestionId);
        
        List<AnswerRecord> records = answerRecordMapper.selectList(recordWrapper);
        if (records.isEmpty()) {
            throw new RuntimeException("未找到模拟考试记录");
        }
        
        // 获取有效答题记录（过滤掉题目不存在的记录）
        List<Map<String, Object>> answerDetails = new ArrayList<>();
        List<AnswerRecord> validRecords = new ArrayList<>();
        
        for (AnswerRecord record : records) {
            try {
                // 使用QuestionService获取题目信息，避免类型转换问题
                Result<QuestionVO> questionResult = questionService.getQuestionById(record.getQuestionId());
                if (questionResult != null && questionResult.isSuccess() && questionResult.getData() != null) {
                    QuestionVO questionVO = questionResult.getData();
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", record.getQuestionId());
                    detail.put("questionContent", questionVO.getContent() != null ? questionVO.getContent() : "");
                    detail.put("questionType", questionVO.getQuestionType() != null ? questionVO.getQuestionType() : 0);
                    detail.put("options", questionVO.getOptionList() != null ? questionVO.getOptionList() : new ArrayList<>());
                    detail.put("userAnswer", record.getUserAnswer() != null ? record.getUserAnswer() : "");
                    detail.put("correctAnswer", questionVO.getCorrectAnswer() != null ? questionVO.getCorrectAnswer() : "");
                    detail.put("isCorrect", record.getIsCorrect() != null && record.getIsCorrect() == 1);
                    detail.put("score", record.getScore() != null ? record.getScore() : 0);
                    detail.put("answerTime", record.getAnswerTime() != null ? record.getAnswerTime() : 0);
                    detail.put("explanation", questionVO.getAnalysis() != null ? questionVO.getAnalysis() : "");
                    detail.put("questionImages", questionVO.getImages() != null ? questionVO.getImages() : "");
                    
                    answerDetails.add(detail);
                    validRecords.add(record);
                } else {
                    System.err.println("跳过不存在的题目，题目ID: " + record.getQuestionId());
                }
            } catch (Exception e) {
                System.err.println("获取题目信息失败，题目ID: " + record.getQuestionId() + ", 错误: " + e.getMessage());
                // 跳过出错的题目记录
                continue;
            }
        }
        
        // 如果没有有效记录，抛出异常
        if (validRecords.isEmpty()) {
            throw new RuntimeException("模拟考试记录中的题目已不存在");
        }
        
        // 重新计算统计数据（基于有效记录）
        int totalScore = validRecords.stream().mapToInt(AnswerRecord::getScore).sum();
        int correctCount = (int) validRecords.stream().filter(r -> r.getIsCorrect() == 1).count();
        int totalCount = validRecords.size();
        double accuracy = totalCount > 0 ? (double) correctCount / totalCount * 100 : 0;
        int passScore = totalCount * 6; // 60%及格
        boolean passed = totalScore >= passScore;
        
        // 获取科目信息（从第一个有效题目获取）
        String subjectName = "模拟考试";
        LocalDateTime submitTime = LocalDateTime.now();
        
        if (!validRecords.isEmpty()) {
            try {
                AnswerRecord firstRecord = validRecords.get(0);
                Result<QuestionVO> questionResult = questionService.getQuestionById(firstRecord.getQuestionId());
                if (questionResult != null && questionResult.isSuccess() && questionResult.getData() != null) {
                    QuestionVO questionVO = questionResult.getData();
                    if (questionVO.getSubjectId() != null) {
                        Subject subject = subjectMapper.selectById(questionVO.getSubjectId());
                        if (subject != null && subject.getName() != null) {
                            subjectName = subject.getName() + " - 模拟考试";
                        }
                    }
                }
                
                if (firstRecord.getCreateTime() != null) {
                    submitTime = firstRecord.getCreateTime();
                }
            } catch (Exception e) {
                System.err.println("获取模拟考试科目信息失败: " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examTitle", subjectName);
        result.put("examDescription", "模拟考试 - 随机生成题目");
        result.put("subjectName", subjectName);
        result.put("duration", 60); // 默认60分钟
        result.put("totalScore", totalScore);
        result.put("passScore", passScore);
        result.put("correctCount", correctCount);
        result.put("totalCount", totalCount);
        result.put("accuracy", accuracy);
        result.put("passed", passed);
        result.put("status", passed ? 1 : 0);
        result.put("submitTime", submitTime);
        result.put("answerDetails", answerDetails);
        
        return result;
    }
    
    /**
     * 检查答案是否正确 - 使用QuestionVO
     */
    private boolean checkAnswer(QuestionVO question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null) {
            return false;
        }
        
        // 根据题目类型进行不同的判分逻辑
        switch (question.getQuestionType()) {
            case 0: // 单选题
            case 2: // 判断题
                return correctAnswer.equals(userAnswer.trim());
            case 1: // 多选题
                // 多选题答案格式：A,B,C
                String[] correctOptions = correctAnswer.split(",");
                String[] userOptions = userAnswer.split(",");
                Arrays.sort(correctOptions);
                Arrays.sort(userOptions);
                return Arrays.equals(correctOptions, userOptions);
            case 3: // 填空题
                // 填空题进行简单的字符串匹配（忽略大小写和前后空格）
                return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
            case 4: // 简答题
                // 简答题暂时不自动判分，返回false表示需要人工判分
                return false;
            default:
                return false;
        }
    }
    
    /**
     * 检查答案是否正确 - 使用Question实体
     */
    private boolean checkAnswerByQuestion(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null) {
            return false;
        }
        
        // 根据题目类型进行不同的判分逻辑
        switch (question.getQuestionType()) {
            case 0: // 单选题
            case 2: // 判断题
                return correctAnswer.equals(userAnswer.trim());
            case 1: // 多选题
                // 多选题答案格式：A,B,C
                String[] correctOptions = correctAnswer.split(",");
                String[] userOptions = userAnswer.split(",");
                Arrays.sort(correctOptions);
                Arrays.sort(userOptions);
                return Arrays.equals(correctOptions, userOptions);
            case 3: // 填空题
                // 填空题进行简单的字符串匹配（忽略大小写和前后空格）
                return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
            case 4: // 简答题
                // 简答题暂时不自动判分，返回false表示需要人工判分
                return false;
            default:
                return false;
        }
    }
    
    @Override
    public Map<String, Object> createSimulationExam(SimulationExamRequest request, Long userId) {
        try {
            // 验证科目是否存在
            Subject subject = subjectMapper.selectById(request.getSubjectId());
            if (subject == null) {
                throw new RuntimeException("科目不存在");
            }
            
            // 限制题目数量最多50道
            int questionCount = Math.min(request.getQuestionCount(), 50);
            
            // 将字符串难度转换为数字
            List<String> numericDifficulties = new ArrayList<>();
            if (request.getDifficulties() != null) {
                for (String difficulty : request.getDifficulties()) {
                    switch (difficulty.toUpperCase()) {
                        case "EASY":
                            numericDifficulties.add("1");
                            break;
                        case "MEDIUM":
                            numericDifficulties.add("2");
                            break;
                        case "HARD":
                            numericDifficulties.add("3");
                            break;
                        default:
                            // 如果已经是数字，直接添加
                            numericDifficulties.add(difficulty);
                            break;
                    }
                }
            }
            
            // 根据条件随机获取题目，确保一次性生成所有题目
            List<QuestionVO> questions = questionService.getRandomQuestions(
                request.getSubjectId(), 
                questionCount, 
                numericDifficulties
            );
            
            if (questions.isEmpty()) {
                throw new RuntimeException("没有找到符合条件的题目");
            }
            
            if (questions.size() < questionCount) {
                throw new RuntimeException("题目数量不足，只找到 " + questions.size() + " 道题目");
            }
            
            // 创建临时考试会话信息，包含所有题目用于全卷预览
            Map<String, Object> result = new HashMap<>();
            result.put("examId", "simulation_" + System.currentTimeMillis()); // 使用时间戳作为临时考试ID
            result.put("subjectName", subject.getName());
            result.put("duration", request.getDuration());
            result.put("questionCount", questions.size());
            result.put("questions", questions); // 一次性返回所有题目
            result.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("创建模拟考试失败：" + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> submitSimulationExam(String examId, Long userId, List<AnswerSubmitRequest> answers) {
        // 模拟考试不需要检查考试是否存在，因为它是临时的
        // 检查是否已经提交过（使用examId作为字符串）
        LambdaQueryWrapper<AnswerRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(AnswerRecord::getUserId, userId)
                    .eq(AnswerRecord::getExamId, -1L) // 模拟考试使用-1作为examId
                    .eq(AnswerRecord::getPracticeType, 3) // 3表示模拟考试模式
                    .eq(AnswerRecord::getRemark, examId); // 使用remark字段存储模拟考试ID
        
        List<AnswerRecord> existingRecords = answerRecordMapper.selectList(recordWrapper);
        if (!existingRecords.isEmpty()) {
            throw new RuntimeException("您已经提交过此模拟考试");
        }
        
        // 从examId中提取题目数量信息（examId格式：simulation_timestamp_questionCount）
        // 如果无法提取，则使用默认值或从答案中推断
        int totalQuestions = 20; // 默认题目数量
        try {
            // 尝试从examId中提取题目数量（如果前端传递了这个信息）
            // 这里暂时使用默认值，后续可以优化
            if (answers != null && !answers.isEmpty()) {
                // 如果有答案，可以从答案中获取题目ID的最大值来推断题目数量
                // 但这种方法不够准确，建议前端在创建模拟考试时传递题目数量
                totalQuestions = Math.max(totalQuestions, answers.size());
            }
        } catch (Exception e) {
            // 使用默认值
            totalQuestions = 20;
        }
        
        // 创建用户答案映射
        Map<Long, AnswerSubmitRequest> answerMap = new HashMap<>();
        if (answers != null) {
            answerMap = answers.stream()
                    .collect(Collectors.toMap(AnswerSubmitRequest::getQuestionId, answer -> answer));
            // 如果有答案，尝试从题目ID推断总题目数
            if (!answers.isEmpty()) {
                Long maxQuestionId = answers.stream()
                        .map(AnswerSubmitRequest::getQuestionId)
                        .max(Long::compareTo)
                        .orElse(0L);
                // 假设题目ID是连续的，从1开始
                totalQuestions = Math.max(totalQuestions, maxQuestionId.intValue());
            }
        }
        
        int totalScore = 0;
        int correctCount = 0;
        
        // 为所有题目创建答题记录（基于推断的题目数量）
        for (int i = 1; i <= totalQuestions; i++) {
            Long questionId = (long) i;
            AnswerSubmitRequest userAnswer = answerMap.get(questionId);
            
            // 如果没有这个题目的答案，但有其他题目的答案，说明这个题目确实存在但未作答
            // 如果完全没有答案，我们只为已知的题目创建记录
            if (userAnswer == null && answers != null && !answers.isEmpty()) {
                // 检查这个questionId是否在答案列表中出现过
                boolean questionExists = answers.stream()
                        .anyMatch(a -> a.getQuestionId().equals(questionId));
                if (!questionExists && i > answers.size()) {
                    // 如果这个题目ID没有在答案中出现，且超出了答案数量，跳过
                    continue;
                }
            }
            
            AnswerRecord record = new AnswerRecord();
            record.setUserId(userId);
            record.setQuestionId(questionId);
            record.setExamId(-1L); // 模拟考试使用-1作为examId
            record.setPracticeType(3); // 模拟考试模式
            record.setRemark(examId); // 使用remark字段存储模拟考试ID
            
            if (userAnswer != null) {
                record.setUserAnswer(userAnswer.getUserAnswer());
                record.setAnswerTime(userAnswer.getAnswerTime());
            } else {
                // 未答题目
                record.setUserAnswer("");
                record.setAnswerTime(0);
            }
            
            // 获取题目信息进行判分
            try {
                // 使用QuestionService获取题目信息，避免类型转换问题
                Result<QuestionVO> questionResult = questionService.getQuestionById(questionId);
                if (questionResult != null && questionResult.isSuccess() && questionResult.getData() != null) {
                    QuestionVO questionVO = questionResult.getData();
                    boolean isCorrect = false;
                    if (userAnswer != null && userAnswer.getUserAnswer() != null && !userAnswer.getUserAnswer().trim().isEmpty()) {
                        isCorrect = checkAnswer(questionVO, userAnswer.getUserAnswer());
                    }
                    
                    record.setIsCorrect(isCorrect ? 1 : 0);
                    
                    if (isCorrect) {
                        correctCount++;
                        // 简单计分：每题10分
                        record.setScore(10);
                        totalScore += record.getScore();
                    } else {
                        record.setScore(0);
                        // 添加到错题本（包括未答题目）
                        try {
                            String wrongAnswer = userAnswer != null ? userAnswer.getUserAnswer() : "";
                            wrongBookService.addWrongQuestion(userId, questionId, wrongAnswer, "EXAM_CUSTOM");
                        } catch (Exception e) {
                            // 记录日志但不影响主流程
                            System.err.println("添加错题失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 如果题目不存在，跳过这条记录
                    System.err.println("获取模拟考试题目失败，题目ID: " + questionId + ", 题目不存在或服务调用失败");
                    continue;
                }
            } catch (Exception e) {
                System.err.println("获取模拟考试题目失败，题目ID: " + questionId + ", 错误: " + e.getMessage());
                e.printStackTrace();
                continue;
            }
            
            answerRecordMapper.insert(record);
        }
        
        // 重新计算实际的题目数量（基于成功插入的记录）
        LambdaQueryWrapper<AnswerRecord> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(AnswerRecord::getUserId, userId)
                   .eq(AnswerRecord::getExamId, -1L)
                   .eq(AnswerRecord::getPracticeType, 3)
                   .eq(AnswerRecord::getRemark, examId);
        totalQuestions = Math.toIntExact(answerRecordMapper.selectCount(countWrapper));
        
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("totalScore", totalScore);
        result.put("correctCount", correctCount);
        result.put("totalCount", totalQuestions);
        result.put("accuracy", totalQuestions > 0 ? (double) correctCount / totalQuestions * 100 : 0);
        result.put("passed", totalScore >= (totalQuestions * 6)); // 60%及格
        result.put("submitTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    @Override
    public void exportExamData(Long examId, HttpServletResponse response) throws Exception {
        // 获取考试信息
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        
        // 获取科目信息
        Subject subject = subjectMapper.selectById(exam.getSubjectId());
        String subjectName = subject != null ? subject.getName() : "未知科目";
        
        // 获取考试题目
        LambdaQueryWrapper<ExamQuestion> examQuestionWrapper = new LambdaQueryWrapper<>();
        examQuestionWrapper.eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSortOrder);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionWrapper);
        
        // 获取题目详情
        List<Question> questions = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (question != null) {
                questions.add(question);
            }
        }
        
        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        
        // 创建试卷题目工作表（参考题库导出格式）
        Sheet questionSheet = workbook.createSheet("试卷题目");
        createExamQuestionSheet(questionSheet, questions, subjectName);
        
        // 设置响应头
        String fileName = URLEncoder.encode(exam.getTitle() + "_试卷题目.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        // 写入响应
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    private void createExamQuestionSheet(Sheet sheet, List<Question> questions, String subjectName) {
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "科目名称", "题目类型", "难度", "题目标题", "题目内容",
            "选项A", "选项B", "选项C", "选项D", "选项E", "选项F",
            "正确答案", "题目解析", "知识点"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // 填充题目数据
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            Row row = sheet.createRow(i + 1);
            
            // 科目名称
            row.createCell(0).setCellValue(subjectName);
            
            // 题目类型
            row.createCell(1).setCellValue(getQuestionTypeText(question.getQuestionType()));
            
            // 难度
            row.createCell(2).setCellValue(getDifficultyText(question.getDifficulty()));
            
            // 题目标题
            row.createCell(3).setCellValue(question.getTitle() != null ? question.getTitle() : question.getContent());
            
            // 题目内容
            row.createCell(4).setCellValue(question.getContent());
            
            // 解析选项（如果有的话）
            String[] options = new String[6];
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                String[] optionArray = question.getOptions().split(";");
                for (int j = 0; j < Math.min(optionArray.length, 6); j++) {
                    String option = optionArray[j].trim();
                    // 移除选项前缀（如A.、B.等）
                    if (option.length() > 2 && option.charAt(1) == '.') {
                        option = option.substring(2).trim();
                    }
                    options[j] = option;
                }
            }
            
            // 选项A-F
            for (int j = 0; j < 6; j++) {
                row.createCell(5 + j).setCellValue(options[j] != null ? options[j] : "");
            }
            
            // 正确答案
            String correctAnswer = question.getCorrectAnswer();
            if (question.getQuestionType() == 2) { // 判断题
                correctAnswer = "true".equals(correctAnswer) ? "正确" : "错误";
            }
            row.createCell(11).setCellValue(correctAnswer != null ? correctAnswer : "");
            
            // 题目解析
            row.createCell(12).setCellValue(question.getAnalysis() != null ? question.getAnalysis() : "");
            
            // 知识点
            row.createCell(13).setCellValue(question.getKnowledgePoints() != null ? question.getKnowledgePoints() : "");
        }
        
        // 设置列宽
        int[] colWidths = {12, 10, 8, 30, 40, 20, 20, 20, 20, 20, 20, 15, 30, 20};
        for (int i = 0; i < colWidths.length; i++) {
            sheet.setColumnWidth(i, colWidths[i] * 256);
        }
    }
    
    private void createExamInfoSheet(Sheet sheet, Exam exam, String subjectName) {
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("考试ID");
        headerRow.createCell(1).setCellValue("考试名称");
        headerRow.createCell(2).setCellValue("科目");
        headerRow.createCell(3).setCellValue("题目数量");
        headerRow.createCell(4).setCellValue("考试时长(分钟)");
        headerRow.createCell(5).setCellValue("总分");
        headerRow.createCell(6).setCellValue("及格分");
        headerRow.createCell(7).setCellValue("考试类型");
        headerRow.createCell(8).setCellValue("状态");
        headerRow.createCell(9).setCellValue("开始时间");
        headerRow.createCell(10).setCellValue("结束时间");
        headerRow.createCell(11).setCellValue("创建时间");
        
        // 创建数据行
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue(exam.getId());
        dataRow.createCell(1).setCellValue(exam.getTitle());
        dataRow.createCell(2).setCellValue(subjectName);
        dataRow.createCell(3).setCellValue(exam.getQuestionCount());
        dataRow.createCell(4).setCellValue(exam.getDuration());
        dataRow.createCell(5).setCellValue(exam.getTotalScore());
        dataRow.createCell(6).setCellValue(exam.getPassScore());
        dataRow.createCell(7).setCellValue(exam.getExamType() == 0 ? "随机题目" : "自选题目");
        dataRow.createCell(8).setCellValue(getStatusText(exam.getStatus()));
        dataRow.createCell(9).setCellValue(exam.getStartTime() != null ? exam.getStartTime().toString() : "");
        dataRow.createCell(10).setCellValue(exam.getEndTime() != null ? exam.getEndTime().toString() : "");
        dataRow.createCell(11).setCellValue(exam.getCreateTime() != null ? exam.getCreateTime().toString() : "");
        
        // 自动调整列宽
        for (int i = 0; i < 12; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createQuestionSheet(Sheet sheet, List<Question> questions, List<ExamQuestion> examQuestions) {
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("序号");
        headerRow.createCell(1).setCellValue("题目ID");
        headerRow.createCell(2).setCellValue("题目类型");
        headerRow.createCell(3).setCellValue("题目内容");
        headerRow.createCell(4).setCellValue("选项");
        headerRow.createCell(5).setCellValue("正确答案");
        headerRow.createCell(6).setCellValue("解析");
        headerRow.createCell(7).setCellValue("难度");
        headerRow.createCell(8).setCellValue("知识点");
        headerRow.createCell(9).setCellValue("分值");
        
        // 创建数据行
        Map<Long, Integer> questionScoreMap = examQuestions.stream()
                .collect(Collectors.toMap(ExamQuestion::getQuestionId, ExamQuestion::getScore));
        
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            Row dataRow = sheet.createRow(i + 1);
            dataRow.createCell(0).setCellValue(i + 1);
            dataRow.createCell(1).setCellValue(question.getId());
            dataRow.createCell(2).setCellValue(getQuestionTypeText(question.getQuestionType()));
            dataRow.createCell(3).setCellValue(question.getContent());
            dataRow.createCell(4).setCellValue(question.getOptions() != null ? question.getOptions() : "");
            dataRow.createCell(5).setCellValue(question.getCorrectAnswer());
            dataRow.createCell(6).setCellValue(question.getAnalysis() != null ? question.getAnalysis() : "");
            dataRow.createCell(7).setCellValue(getDifficultyText(question.getDifficulty()));
            dataRow.createCell(8).setCellValue(question.getKnowledgePoints() != null ? question.getKnowledgePoints() : "");
            dataRow.createCell(9).setCellValue(questionScoreMap.getOrDefault(question.getId(), 0));
        }
        
        // 自动调整列宽
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createAnswerSheet(Sheet sheet, Map<Long, List<AnswerRecord>> userAnswerMap, List<Question> questions) {
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("用户ID");
        headerRow.createCell(1).setCellValue("用户名");
        headerRow.createCell(2).setCellValue("题目ID");
        headerRow.createCell(3).setCellValue("题目内容");
        headerRow.createCell(4).setCellValue("用户答案");
        headerRow.createCell(5).setCellValue("正确答案");
        headerRow.createCell(6).setCellValue("是否正确");
        headerRow.createCell(7).setCellValue("得分");
        headerRow.createCell(8).setCellValue("答题时间");
        
        int rowIndex = 1;
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        
        for (Map.Entry<Long, List<AnswerRecord>> entry : userAnswerMap.entrySet()) {
            Long userId = entry.getKey();
            List<AnswerRecord> userAnswers = entry.getValue();
            
            // 获取用户信息
            SysUser user = sysUserMapper.selectById(userId);
            String username = user != null ? user.getUsername() : "未知用户";
            
            for (AnswerRecord answer : userAnswers) {
                Question question = questionMap.get(answer.getQuestionId());
                if (question != null) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(userId);
                    dataRow.createCell(1).setCellValue(username);
                    dataRow.createCell(2).setCellValue(answer.getQuestionId());
                    dataRow.createCell(3).setCellValue(question.getContent());
                    dataRow.createCell(4).setCellValue(answer.getUserAnswer() != null ? answer.getUserAnswer() : "");
                    dataRow.createCell(5).setCellValue(question.getCorrectAnswer());
                    dataRow.createCell(6).setCellValue(answer.getIsCorrect() == 1 ? "正确" : "错误");
                    dataRow.createCell(7).setCellValue(answer.getScore() != null ? answer.getScore() : 0);
                    dataRow.createCell(8).setCellValue(answer.getCreateTime() != null ? answer.getCreateTime().toString() : "");
                }
            }
        }
        
        // 自动调整列宽
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已发布";
            case 2: return "已结束";
            default: return "未知";
        }
    }
    
    private String getQuestionTypeText(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 0: return "单选题";
            case 1: return "多选题";
            case 2: return "判断题";
            case 3: return "简答题";
            default: return "未知";
        }
    }
    
    private String getDifficultyText(Integer difficulty) {
        if (difficulty == null) return "未知";
        switch (difficulty) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }
    
    /**
     * 回退到传统格式解析
     */
    private void fallbackToTraditionalParsing(String optionsStr, List<com.qltiku2.dto.QuestionSaveRequest.QuestionOption> optionList) {
        // 传统分号分隔格式：A.选项1;B.选项2;C.选项3
        String[] optionArray = optionsStr.split(";");
        for (String option : optionArray) {
            String trimmedOption = option.trim();
            if (!trimmedOption.isEmpty()) {
                com.qltiku2.dto.QuestionSaveRequest.QuestionOption questionOption = 
                    new com.qltiku2.dto.QuestionSaveRequest.QuestionOption();
                
                // 提取选项键值对，格式如 "A.选项内容"
                if (trimmedOption.length() > 2 && trimmedOption.charAt(1) == '.') {
                    questionOption.setKey(String.valueOf(trimmedOption.charAt(0)));
                    questionOption.setValue(trimmedOption.substring(2).trim());
                } else {
                    // 如果格式不标准，使用序号作为key
                    questionOption.setKey(String.valueOf((char)('A' + optionList.size())));
                    questionOption.setValue(trimmedOption);
                }
                optionList.add(questionOption);
            }
        }
    }

    /**
     * 将Question实体转换为QuestionVO
     */
    private QuestionVO convertToQuestionVO(Question question) {
        if (question == null) {
            return null;
        }
        
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setSubjectId(question.getSubjectId());
        vo.setQuestionType(question.getQuestionType());
        vo.setDifficulty(question.getDifficulty());
        vo.setTitle(question.getTitle());
        vo.setContent(question.getContent());
        vo.setOptions(question.getOptions());
        vo.setCorrectAnswer(question.getCorrectAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setKnowledgePoints(question.getKnowledgePoints());
        vo.setCreateTime(question.getCreateTime());
        vo.setUpdateTime(question.getUpdateTime());
        
        // 解析选项为列表格式
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<com.qltiku2.dto.QuestionSaveRequest.QuestionOption> optionList = new ArrayList<>();
            
            try {
                String optionsStr = question.getOptions().trim();
                ObjectMapper objectMapper = new ObjectMapper();
                
                // 检查是否为JSON数组格式
                if (optionsStr.startsWith("[") && optionsStr.endsWith("]")) {
                    try {
                        // 尝试解析为QuestionOption对象数组
                        List<Map<String, Object>> optionArray = objectMapper.readValue(optionsStr, new TypeReference<List<Map<String, Object>>>(){});
                        
                        for (Map<String, Object> option : optionArray) {
                            com.qltiku2.dto.QuestionSaveRequest.QuestionOption questionOption = 
                                new com.qltiku2.dto.QuestionSaveRequest.QuestionOption();
                            
                            // 安全获取key和value，避免空指针
                            Object keyObj = option.get("key");
                            Object valueObj = option.get("value");
                            
                            questionOption.setKey(keyObj != null ? keyObj.toString() : "");
                            questionOption.setValue(valueObj != null ? valueObj.toString() : "");
                            
                            optionList.add(questionOption);
                        }
                    } catch (Exception e1) {
                        // 如果解析为对象数组失败，尝试解析为字符串数组
                        try {
                            List<String> stringArray = objectMapper.readValue(optionsStr, new TypeReference<List<String>>(){});
                            for (int i = 0; i < stringArray.size(); i++) {
                                com.qltiku2.dto.QuestionSaveRequest.QuestionOption questionOption = 
                                    new com.qltiku2.dto.QuestionSaveRequest.QuestionOption();
                                questionOption.setKey(String.valueOf((char)('A' + i)));
                                questionOption.setValue(stringArray.get(i));
                                optionList.add(questionOption);
                            }
                        } catch (Exception e2) {
                            // 解析失败，回退到传统格式解析
                            fallbackToTraditionalParsing(optionsStr, optionList);
                        }
                    }
                } else {
                    // 传统分号分隔格式：A.选项1;B.选项2;C.选项3
                    fallbackToTraditionalParsing(optionsStr, optionList);
                }
                
                vo.setOptionList(optionList);
            } catch (Exception e) {
                System.err.println("解析题目选项失败，题目ID: " + question.getId() + ", 选项内容: " + question.getOptions() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                // 解析失败时设置空列表
                vo.setOptionList(new ArrayList<>());
            }
        }
        
        // 处理图片信息
        vo.setImages(question.getImages());
        if (question.getImages() != null && !question.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<String> imageList = objectMapper.readValue(question.getImages(), new TypeReference<List<String>>(){});
                vo.setImageList(imageList);
            } catch (Exception e) {
                System.err.println("解析题目图片失败，题目ID: " + question.getId() + ", 图片内容: " + question.getImages() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                vo.setImageList(new ArrayList<>());
            }
        } else {
            vo.setImageList(new ArrayList<>());
        }
        
        return vo;
    }
}
