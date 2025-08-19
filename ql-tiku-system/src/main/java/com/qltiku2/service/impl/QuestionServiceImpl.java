package com.qltiku2.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AnswerSubmitRequest;
import com.qltiku2.dto.QuestionQueryRequest;
import com.qltiku2.dto.QuestionSaveRequest;
import com.qltiku2.entity.AnswerRecord;
import com.qltiku2.entity.Question;
import com.qltiku2.entity.Subject;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.AnswerRecordMapper;
import com.qltiku2.mapper.QuestionMapper;
import com.qltiku2.mapper.SubjectMapper;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.mapper.PracticeRecordMapper;
import com.qltiku2.mapper.PracticeSessionQuestionMapper;
import com.qltiku2.entity.PracticeRecord;
import com.qltiku2.service.AiChatService;
import com.qltiku2.service.PracticeCacheService;
import com.qltiku2.service.QuestionService;
import com.qltiku2.service.SysConfigService;
import com.qltiku2.service.AiGradingRecordService;
import com.qltiku2.entity.AiGradingRecord;
import com.qltiku2.utils.QuestionCacheUtils;
import com.qltiku2.vo.AnswerSubmitResponse;
import com.qltiku2.vo.QuestionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * 题目服务实现类
 * 
 * @author qltiku2
 */
@Service
public class QuestionServiceImpl implements QuestionService {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionServiceImpl.class);
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private AiChatService aiChatService;
    
    @Autowired
    private SysConfigService sysConfigService;
    
    @Autowired
    private AiGradingRecordService aiGradingRecordService;
    
    @Autowired
    private PracticeRecordMapper practiceRecordMapper;
    
    @Autowired
    private PracticeSessionQuestionMapper practiceSessionQuestionMapper;
    
    @Autowired
    private QuestionCacheUtils questionCache;

    @Autowired
    private PracticeCacheService practiceCacheService;

    /**
     * 生成题目ID缓存
     * 
     * @param userId 用户ID
     * @param subjectId 科目ID
     * @param questionType 题目类型
     * @param difficulty 难度
     * @param mode 模式（sequential/random）
     */
    private void generateQuestionIdCache(Long userId, Long subjectId, Integer questionType, 
                                       Integer difficulty, String mode) {
        try {
            // 构建查询条件
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("deleted", 0);
            
            if (subjectId != null) {
                queryWrapper.eq("subject_id", subjectId);
            }
            
            if (questionType != null) {
                queryWrapper.eq("question_type", questionType);
            }
            
            if (difficulty != null) {
                queryWrapper.eq("difficulty", difficulty);
            }
            
            // 获取当前用户最新的进行中的练习记录
            QueryWrapper<PracticeRecord> practiceWrapper = new QueryWrapper<>();
            practiceWrapper.eq("user_id", userId)
                          .eq("status", 1) // 进行中状态
                          .orderByDesc("create_time")
                          .last("LIMIT 1");
            PracticeRecord currentPractice = practiceRecordMapper.selectOne(practiceWrapper);
            
            // 获取已做过的题目ID
            Set<Long> answeredQuestionIds = new HashSet<>();
            if (currentPractice != null) {
                List<Long> answeredIds = questionMapper.selectAnsweredQuestionIds(currentPractice.getId());
                answeredQuestionIds.addAll(answeredIds);
            }
            
            // 查询所有符合条件的题目
            List<Question> questions = questionMapper.selectList(queryWrapper);
            
            if (questions.isEmpty()) {
                return;
            }
            
            // 过滤已做过的题目
            List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .filter(id -> !answeredQuestionIds.contains(id))
                .collect(Collectors.toList());
            
            if (questionIds.isEmpty()) {
                return;
            }
            
            // 根据模式处理题目ID顺序
            if ("random".equals(mode)) {
                // 随机模式：使用Set去重后随机排序
                Collections.shuffle(questionIds, new Random());
            } else {
                // 顺序模式：按ID升序排列
                Collections.sort(questionIds);
            }
            
            // 缓存题目ID数组
            practiceCacheService.cacheQuestionIds(userId, subjectId, questionType, difficulty, mode, questionIds);
            
        } catch (Exception e) {
            log.error("生成题目ID缓存失败", e);
        }
    }

    @Override
    public Result<IPage<QuestionVO>> getQuestionPage(QuestionQueryRequest request) {
        try {
            // 构建一个明确、稳定的缓存键，包含所有筛选参数
            String cacheKey = String.format("page:current=%d&size=%d&subject=%s&type=%s&difficulty=%s&keyword=%s",
                request.getCurrent(),
                request.getSize(),
                request.getSubjectId() != null ? request.getSubjectId() : "all",
                request.getQuestionType() != null ? request.getQuestionType() : "all",
                request.getDifficulty() != null ? request.getDifficulty() : "all",
                StringUtils.hasText(request.getKeyword()) ? request.getKeyword() : ""
            );
            
            // 尝试从缓存获取
            Result<IPage<QuestionVO>> cachedResult = questionCache.get(cacheKey, Result.class);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // 缓存未命中，执行查询
            // 创建分页对象
            Page<Question> page = new Page<>(request.getCurrent(), request.getSize());
            
            // 构建查询条件
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            
            // 添加删除标记条件（使用表别名）
            queryWrapper.eq("q.deleted", 0);
            
            if (request.getSubjectId() != null) {
                queryWrapper.eq("q.subject_id", request.getSubjectId());
            }
            
            if (request.getQuestionType() != null) {
                System.out.println("添加题目类型筛选条件: q.question_type = " + request.getQuestionType());
                queryWrapper.eq("q.question_type", request.getQuestionType());
            }
            
            if (request.getDifficulty() != null) {
                queryWrapper.eq("q.difficulty", request.getDifficulty());
            }
            
            if (StringUtils.hasText(request.getKeyword())) {
                queryWrapper.and(wrapper -> wrapper
                    .like("q.title", request.getKeyword())
                    .or()
                    .like("q.content", request.getKeyword())
                );
            }
            
            if (StringUtils.hasText(request.getKnowledgePoints())) {
                queryWrapper.like("q.knowledge_points", request.getKnowledgePoints());
            }
            
            if (request.getCreateUserId() != null) {
                queryWrapper.eq("q.create_user_id", request.getCreateUserId());
            }
            
            // 排序
            String sortField = request.getSortField();
            // 将Java字段名转换为数据库字段名
            if ("sortOrder".equals(sortField)) {
                sortField = "sort_order";
            } else if ("createTime".equals(sortField)) {
                sortField = "create_time";
            } else if ("updateTime".equals(sortField)) {
                sortField = "update_time";
            }
            
            if ("asc".equals(request.getSortOrder())) {
                queryWrapper.orderByAsc(sortField);
            } else {
                queryWrapper.orderByDesc(sortField);
            }
            
            // 执行查询（使用带科目名称的查询）
            IPage<Question> questionPage = questionMapper.selectQuestionPage(page, queryWrapper);
            
            // 添加查询结果调试信息
            System.out.println("查询结果数量: " + questionPage.getRecords().size());
            for (Question q : questionPage.getRecords()) {
                System.out.println("题目ID: " + q.getId() + ", 题目类型: " + q.getQuestionType() + ", 内容: " + (q.getContent() != null ? q.getContent().substring(0, Math.min(20, q.getContent().length())) : "null"));
            }
            
            // 转换为VO
            IPage<QuestionVO> voPage = questionPage.convert(this::convertToVO);
            
            Result<IPage<QuestionVO>> result = Result.success(voPage);
            
            // 将结果存入缓存
            questionCache.set(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("查询题目列表失败", e);
            return Result.error("查询题目列表失败：" + e.getMessage());
        }
    }
    

    @Override
    public Result<Map<String, Object>> getSubjectQuestionStats() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 按科目统计题目数量
            List<Map<String, Object>> subjectStats = questionMapper.countQuestionsBySubject();
            result.put("subjectStats", subjectStats);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取科目题目数量统计失败", e);
            return Result.error("获取科目题目数量统计失败");
        }
    }
    
    @Override
    public Result<Integer> getQuestionCountBySubject(Long subjectId) {
        try {
            if (subjectId == null) {
                return Result.badRequest("科目ID不能为空");
            }
            
            Integer count = questionMapper.countQuestionsBySubjectId(subjectId);
            return Result.success(count != null ? count : 0);
        } catch (Exception e) {
            log.error("获取科目题目数量失败", e);
            return Result.error("获取科目题目数量失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<QuestionVO> getQuestionById(Long id) {
        try {
            // 构建缓存键
            String cacheKey = "id:" + id;
            
            // 尝试从缓存获取 - 直接获取Object避免类型转换问题
            Object cachedObj = questionCache.get(cacheKey);
            if (cachedObj != null) {
                if (cachedObj instanceof Result) {
                    Result<?> result = (Result<?>) cachedObj;
                    if (result.getData() instanceof QuestionVO) {
                        return (Result<QuestionVO>) cachedObj;
                    } else if (result.getData() instanceof java.util.LinkedHashMap) {
                        // 处理LinkedHashMap转换为QuestionVO的情况
                        java.util.LinkedHashMap<?, ?> map = (java.util.LinkedHashMap<?, ?>) result.getData();
                        String json = new ObjectMapper().writeValueAsString(map);
                        QuestionVO questionVO = new ObjectMapper().readValue(json, QuestionVO.class);
                        return Result.success(questionVO);
                    }
                }
                
                // 如果缓存数据格式不正确，删除缓存并重新获取
                questionCache.delete(cacheKey);
            }
            
            // 缓存未命中，执行查询
            Question question = questionMapper.selectById(id);
            if (question == null) {
                return Result.error("题目不存在");
            }
            
            QuestionVO questionVO = convertToVO(question);
            Result<QuestionVO> result = Result.success(questionVO);
            
            // 将结果存入缓存
            questionCache.set(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("获取题目详情失败", e);
            return Result.error("获取题目详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> createQuestion(QuestionSaveRequest request) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Result.unauthorized("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 创建题目实体
            Question question = new Question();
            BeanUtils.copyProperties(request, question);
            
            // 处理选项
            if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                question.setOptions(objectMapper.writeValueAsString(request.getOptions()));
            }
            
            // 处理图片信息
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                question.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
            
            question.setCreateUserId(user.getId());
            question.setCreateTime(LocalDateTime.now());
            question.setUpdateTime(LocalDateTime.now());
            
            // 保存题目
            int result = questionMapper.insert(question);
            if (result > 0) {
                // 清除相关缓存
                questionCache.deleteByPrefix("page:");
                questionCache.deleteByPrefix("random:");
                questionCache.deleteByPrefix("practice:");
                questionCache.deleteByPrefix("search:");
                questionCache.deleteByPrefix("statistics");
                
                return Result.success("题目创建成功");
            } else {
                return Result.error("题目创建失败");
            }
            
        } catch (Exception e) {
            log.error("创建题目失败", e);
            return Result.error("创建题目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateQuestion(Long id, QuestionSaveRequest request) {
        try {
            // 检查题目是否存在
            Question existingQuestion = questionMapper.selectById(id);
            if (existingQuestion == null) {
                return Result.error("题目不存在");
            }
            
            // 检查权限（只有创建者或管理员可以修改）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String username = authentication.getName();
                SysUser user = sysUserMapper.selectByUsername(username);
                if (user != null && user.getUserType() != 2 && 
                    !existingQuestion.getCreateUserId().equals(user.getId())) {
                    return Result.forbidden("无权限修改此题目");
                }
            }
            
            // 更新题目信息
            Question question = new Question();
            BeanUtils.copyProperties(request, question);
            question.setId(id);
            
            // 处理选项
            if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                question.setOptions(objectMapper.writeValueAsString(request.getOptions()));
            }
            
            // 处理图片信息
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                question.setImages(objectMapper.writeValueAsString(request.getImages()));
            } else if (request.getImages() != null && request.getImages().isEmpty()) {
                // 如果图片列表为空，清空图片信息
                question.setImages(null);
            }
            
            question.setUpdateTime(LocalDateTime.now());
            
            // 更新题目
            int result = questionMapper.updateById(question);
            if (result > 0) {
                // 清除特定ID的缓存
                questionCache.delete("id:" + id);
                
                // 清除可能包含此题目的列表缓存
                questionCache.deleteByPrefix("page:");
                questionCache.deleteByPrefix("random:");
                questionCache.deleteByPrefix("practice:");
                questionCache.deleteByPrefix("search:");
                questionCache.deleteByPrefix("statistics");
                
                return Result.success("题目更新成功");
            } else {
                return Result.error("题目更新失败");
            }
            
        } catch (Exception e) {
            log.error("更新题目失败", e);
            return Result.error("更新题目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> deleteQuestion(Long id) {
        try {
            // 检查题目是否存在
            Question existingQuestion = questionMapper.selectById(id);
            if (existingQuestion == null) {
                return Result.error("题目不存在");
            }
            
            // 检查权限
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String username = authentication.getName();
                SysUser user = sysUserMapper.selectByUsername(username);
                if (user != null && user.getUserType() != 2 && 
                    !existingQuestion.getCreateUserId().equals(user.getId())) {
                    return Result.forbidden("无权限删除此题目");
                }
            }
            
            // 删除题目（逻辑删除）
            int result = questionMapper.deleteById(id);
            if (result > 0) {
                // 清除特定ID的缓存
                questionCache.delete("id:" + id);
                
                // 清除可能包含此题目的列表缓存
                questionCache.deleteByPrefix("page:");
                questionCache.deleteByPrefix("random:");
                questionCache.deleteByPrefix("practice:");
                questionCache.deleteByPrefix("search:");
                questionCache.deleteByPrefix("statistics");
                
                return Result.success("题目删除成功");
            } else {
                return Result.error("题目删除失败");
            }
            
        } catch (Exception e) {
            log.error("删除题目失败", e);
            return Result.error("删除题目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> batchDeleteQuestions(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.badRequest("请选择要删除的题目");
            }
            
            // 检查权限（简化处理，实际应该逐个检查）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String username = authentication.getName();
                SysUser user = sysUserMapper.selectByUsername(username);
                if (user != null && user.getUserType() != 2) {
                    return Result.forbidden("只有管理员可以批量删除题目");
                }
            }
            
            // 批量删除
            int result = questionMapper.deleteBatchIds(ids);
            if (result > 0) {
                // 清除每个ID的缓存
                for (Long id : ids) {
                    questionCache.delete("id:" + id);
                }
                
                // 清除可能包含这些题目的列表缓存
                questionCache.deleteByPrefix("page:");
                questionCache.deleteByPrefix("random:");
                questionCache.deleteByPrefix("practice:");
                questionCache.deleteByPrefix("search:");
                questionCache.deleteByPrefix("statistics");
                
                return Result.success("批量删除成功，共删除" + result + "道题目");
            } else {
                return Result.error("批量删除失败");
            }
            
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<List<QuestionVO>> getRandomQuestions(Long subjectId, Integer questionType, 
                                                     Integer difficulty, Integer count) {
        try {
            // 构建缓存键
            String cacheKey = "random:" + (subjectId != null ? subjectId : "all") + "_" 
                            + (questionType != null ? questionType : "all") + "_" 
                            + (difficulty != null ? difficulty : "all") + "_" 
                            + count;
            
            // 尝试从缓存获取
            Result<List<QuestionVO>> cachedResult = questionCache.get(cacheKey, Result.class);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // 缓存未命中，执行查询
            // 构建查询条件
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            if (subjectId != null) {
                queryWrapper.eq("subject_id", subjectId);
            }
            if (questionType != null) {
                queryWrapper.eq("question_type", questionType);
            }
            if (difficulty != null) {
                queryWrapper.eq("difficulty", difficulty);
            }
            
            // 随机排序并限制数量
            queryWrapper.orderByAsc("RAND()").last("LIMIT " + count);
            
            List<Question> questions = questionMapper.selectList(queryWrapper);
            List<QuestionVO> questionVOs = questions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            Result<List<QuestionVO>> result = Result.success(questionVOs);
            
            // 将结果存入缓存
            questionCache.set(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("获取随机题目失败", e);
            return Result.error("获取随机题目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<IPage<QuestionVO>> searchQuestions(String keyword, Integer current, Integer size) {
        try {
            if (!StringUtils.hasText(keyword)) {
                return Result.badRequest("搜索关键词不能为空");
            }
            
            // 构建缓存键
            String cacheKey = "search:" + keyword + ":" + current + ":" + size;
            
            // 尝试从缓存获取
            Result<IPage<QuestionVO>> cachedResult = questionCache.get(cacheKey, Result.class);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // 缓存未命中，执行查询
            Page<Question> page = new Page<>(current, size);
            
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            queryWrapper.and(wrapper -> wrapper
                .like("title", keyword)
                .or()
                .like("content", keyword)
                .or()
                .like("knowledge_points", keyword)
            );
            
            queryWrapper.orderByDesc("create_time");
            
            IPage<Question> questionPage = questionMapper.selectPage(page, queryWrapper);
            IPage<QuestionVO> voPage = questionPage.convert(this::convertToVO);
            
            Result<IPage<QuestionVO>> result = Result.success(voPage);
            
            // 将结果存入缓存
            questionCache.set(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索题目失败", e);
            return Result.error("搜索题目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Object> getQuestionStatistics() {
        try {
            // 构建缓存键
            String cacheKey = "statistics";
            
            // 尝试从缓存获取
            Result<Object> cachedResult = questionCache.get(cacheKey, Result.class);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // 缓存未命中，执行查询
            Map<String, Object> statistics = new HashMap<>();
            
            // 总题目数
            Long totalCount = questionMapper.selectCount(null);
            statistics.put("totalCount", totalCount);
            
            // 按科目统计
            // 这里需要调用自定义的统计方法，暂时用简单实现
            statistics.put("subjectStats", new ArrayList<>());
            
            // 按类型统计
            statistics.put("typeStats", new ArrayList<>());
            
            // 按难度统计
            statistics.put("difficultyStats", new ArrayList<>());
            
            Result<Object> result = Result.success(statistics);
            
            // 将结果存入缓存
            questionCache.set(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> importQuestions(List<QuestionSaveRequest> questions) {
        try {
            // 简单的批量导入实现
            for (QuestionSaveRequest request : questions) {
                createQuestion(request);
            }
            return Result.success("批量导入成功，共导入 " + questions.size() + " 道题目");
        } catch (Exception e) {
            log.error("批量导入失败", e);
            return Result.error("批量导入失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<List<QuestionVO>> exportQuestions(QuestionQueryRequest request) {
        try {
            // 构建缓存键
            String cacheKey = "export:" + request.hashCode();
            
            // 尝试从缓存获取
            Result<List<QuestionVO>> cachedResult = questionCache.get(cacheKey, Result.class);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // 缓存未命中，执行查询
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            
            if (request.getSubjectId() != null) {
                queryWrapper.eq("subject_id", request.getSubjectId());
            }
            
            if (request.getQuestionType() != null) {
                queryWrapper.eq("question_type", request.getQuestionType());
            }
            
            if (request.getDifficulty() != null) {
                queryWrapper.eq("difficulty", request.getDifficulty());
            }
            
            queryWrapper.orderByDesc("create_time");
            
            List<Question> questions = questionMapper.selectList(queryWrapper);
            List<QuestionVO> questionVOs = questions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            Result<List<QuestionVO>> result = Result.success(questionVOs);
            
            // 将结果存入缓存
            questionCache.set(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("导出题目失败", e);
            return Result.error("导出题目失败：" + e.getMessage());
        }
    }
    
    /**
     * 转换为VO对象
     */
    private QuestionVO convertToVO(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO vo = new QuestionVO();
        
        // 手动复制所有字段，以确保数据完整性，避免BeanUtils的潜在问题
        vo.setId(question.getId());
        vo.setSubjectId(question.getSubjectId());
        vo.setTitle(question.getTitle());
        vo.setContent(question.getContent());
        vo.setOptions(question.getOptions());
        vo.setCorrectAnswer(question.getCorrectAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setKnowledgePoints(question.getKnowledgePoints());
        vo.setCreateUserId(question.getCreateUserId());
        vo.setCreateTime(question.getCreateTime());
        vo.setUpdateTime(question.getUpdateTime());
        
        // 关键字段：确保类型和难度被设置
        // vo的setter会自动设置对应的Name
        if (question.getQuestionType() != null) {
            vo.setQuestionType(question.getQuestionType());
        }
        if (question.getDifficulty() != null) {
            vo.setDifficulty(question.getDifficulty());
        }

        // 关联字段
        // subjectName 是在 selectQuestionPage 查询中直接获取的，不需要再次查询数据库
        vo.setSubjectName(question.getSubjectName());

        // 解析JSON和字符串字段
        if (StringUtils.hasText(question.getOptions())) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<QuestionSaveRequest.QuestionOption> optionList = 
                    objectMapper.readValue(question.getOptions(), new TypeReference<List<QuestionSaveRequest.QuestionOption>>(){});
                vo.setOptionList(optionList);
            } catch (Exception e) {
                vo.setOptionList(new ArrayList<>());
            }
        } else {
            vo.setOptionList(new ArrayList<>());
        }
        
        if (StringUtils.hasText(question.getKnowledgePoints())) {
            List<String> knowledgePointList = Arrays.asList(
                question.getKnowledgePoints().split(",")
            );
            vo.setKnowledgePointList(knowledgePointList);
        }
        
        // 处理图片信息
        vo.setImages(question.getImages());
        if (StringUtils.hasText(question.getImages())) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<String> imageList = objectMapper.readValue(question.getImages(), new TypeReference<List<String>>(){});
                vo.setImageList(imageList);
            } catch (Exception e) {
                vo.setImageList(new ArrayList<>());
            }
        } else {
            vo.setImageList(new ArrayList<>());
        }
        
        return vo;
    }
    
    @Override
    public Result<AnswerSubmitResponse> submitAnswer(AnswerSubmitRequest request) {
        try {
            // 获取题目信息
            Question question = questionMapper.selectById(request.getQuestionId());
            if (question == null) {
                return Result.error("题目不存在");
            }
            
            // 判断答案是否正确
            boolean isCorrect = checkAnswer(question, request.getUserAnswer());
            
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String username = authentication.getName();
                SysUser user = sysUserMapper.selectByUsername(username);
                if (user != null) {
                    // 保存答题记录
                    AnswerRecord record = new AnswerRecord();
                    record.setUserId(user.getId());
                    record.setQuestionId(request.getQuestionId());
                    record.setUserAnswer(request.getUserAnswer());
                    record.setIsCorrect(isCorrect ? 1 : 0);
                    record.setPracticeType(1); // 1表示练习模式
                    record.setAnswerTime(request.getAnswerTime());
                    record.setScore(isCorrect ? 1 : 0); // 练习模式简单计分：对1分，错0分
                    record.setCreateTime(LocalDateTime.now());
                    
                    answerRecordMapper.insert(record);
                    
                    // 将题目ID记录到练习会话中（用于随机不重复）
                    // 获取当前用户最新的进行中的练习记录
                    QueryWrapper<PracticeRecord> practiceWrapper = new QueryWrapper<>();
                    practiceWrapper.eq("user_id", user.getId())
                                  .eq("status", 1) // 进行中状态
                                  .orderByDesc("create_time")
                                  .last("LIMIT 1");
                    PracticeRecord currentPractice = practiceRecordMapper.selectOne(practiceWrapper);
                    
                    if (currentPractice != null) {
                        // 检查题目是否已存在，避免重复插入
                        int existCount = practiceSessionQuestionMapper.countByPracticeRecordIdAndQuestionId(
                            currentPractice.getId(), request.getQuestionId());
                        if (existCount == 0) {
                            practiceSessionQuestionMapper.insertPracticeSessionQuestion(
                                currentPractice.getId(), request.getQuestionId());
                        }
                    }
                }
            }
            
            // 构建响应
            AnswerSubmitResponse response = new AnswerSubmitResponse(
                isCorrect,
                question.getCorrectAnswer(),
                request.getUserAnswer(),
                question.getAnalysis()
            );
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("提交答案失败", e);
            return Result.error("提交答案失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查答案是否正确
     */
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || question.getCorrectAnswer() == null) {
            return false;
        }
        
        // 添加调试日志
        System.out.println("=== 答案验证调试信息 ===");
        System.out.println("题目ID: " + question.getId());
        System.out.println("题目类型: " + question.getQuestionType());
        System.out.println("用户答案: [" + userAnswer + "]");
        System.out.println("正确答案: [" + question.getCorrectAnswer() + "]");
        
        // 对于多选题，需要处理逗号分隔的答案
        if (question.getQuestionType() == 1) { // 多选题
            // 将答案按逗号分割并排序
            String[] userAnswers = userAnswer.split(",");
            String[] correctAnswers = question.getCorrectAnswer().split(",");
            
            // 去除空格
            for (int i = 0; i < userAnswers.length; i++) {
                userAnswers[i] = userAnswers[i].trim();
            }
            for (int i = 0; i < correctAnswers.length; i++) {
                correctAnswers[i] = correctAnswers[i].trim();
            }
            
            Arrays.sort(userAnswers);
            Arrays.sort(correctAnswers);
            
            System.out.println("用户答案数组: " + Arrays.toString(userAnswers));
            System.out.println("正确答案数组: " + Arrays.toString(correctAnswers));
            
            boolean result = Arrays.equals(userAnswers, correctAnswers);
            System.out.println("比较结果: " + result);
            System.out.println("========================");
            
            return result;
        } else if (question.getQuestionType() == 2) { // 判断题
            // 判断题需要特殊处理：前端提交A/B，数据库可能存储正确/错误或true/false
            String normalizedUserAnswer = userAnswer.trim();
            String normalizedCorrectAnswer = question.getCorrectAnswer().trim();
            
            // 将前端的A/B转换为对应格式进行比较
            String convertedUserAnswer;
            if ("A".equals(normalizedUserAnswer)) {
                // A表示正确，根据数据库格式进行转换
                if ("true".equals(normalizedCorrectAnswer) || "false".equals(normalizedCorrectAnswer)) {
                    convertedUserAnswer = "true";
                } else {
                    convertedUserAnswer = "正确";
                }
            } else if ("B".equals(normalizedUserAnswer)) {
                // B表示错误，根据数据库格式进行转换
                if ("true".equals(normalizedCorrectAnswer) || "false".equals(normalizedCorrectAnswer)) {
                    convertedUserAnswer = "false";
                } else {
                    convertedUserAnswer = "错误";
                }
            } else {
                convertedUserAnswer = normalizedUserAnswer;
            }
            
            System.out.println("转换后的用户答案: [" + convertedUserAnswer + "]");
            
            boolean result = convertedUserAnswer.equals(normalizedCorrectAnswer);
            System.out.println("比较结果: " + result);
            System.out.println("========================");
            
            return result;
        } else {
            // 单选题直接比较
            boolean result = userAnswer.trim().equals(question.getCorrectAnswer().trim());
            System.out.println("比较结果: " + result);
            System.out.println("========================");
            return result;
        }
    }
    
    @Override
    public Result<Map<String, Object>> aiGrading(Map<String, Object> request) {
        try {
            // 获取请求参数
            Long questionId = Long.valueOf(request.get("questionId").toString());
            String questionContent = (String) request.get("questionContent");
            String userAnswer = (String) request.get("userAnswer");
            String correctAnswer = (String) request.get("correctAnswer");
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Result.unauthorized("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 检查是否有历史记录
            AiGradingRecord latestRecord = aiGradingRecordService.getLatestRecord(user.getId(), questionId);
            
            // 如果是重新判题，软删除旧记录
            if (latestRecord != null) {
                aiGradingRecordService.softDeleteOldRecords(user.getId(), questionId);
            }
            
            // 获取AI判题前缀模板
            String prefixTemplate = sysConfigService.getConfigValue("ai.grading.prefix");
            if (prefixTemplate == null || prefixTemplate.trim().isEmpty()) {
                prefixTemplate = "请对以下简答题进行判题评分：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请给出评分和详细的评价，并在最后一行明确标注：判断结果：正确 或 判断结果：错误";
            }
            
            // 获取AI正确性判断前缀模板
            String correctnessTemplate = sysConfigService.getConfigValue("ai.grading.correctness.prefix");
            if (correctnessTemplate == null || correctnessTemplate.trim().isEmpty()) {
                correctnessTemplate = "请判断以下简答题答案是否正确：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请只回答：正确 或 错误";
            }
            
            // 替换模板中的占位符
            String prompt = prefixTemplate
                .replace("{question}", questionContent)
                .replace("{userAnswer}", userAnswer)
                .replace("{correctAnswer}", correctAnswer != null ? correctAnswer : "无参考答案");
                
            String correctnessPrompt = correctnessTemplate
                .replace("{question}", questionContent)
                .replace("{userAnswer}", userAnswer)
                .replace("{correctAnswer}", correctAnswer != null ? correctAnswer : "无参考答案");
            
            // 生成会话ID
            String sessionId = "grading_" + questionId + "_" + user.getId() + "_" + System.currentTimeMillis();
            String correctnessSessionId = "correctness_" + questionId + "_" + user.getId() + "_" + System.currentTimeMillis();
            
            // 先调用AI判断正确性
            StringBuilder correctnessResponse = new StringBuilder();
            aiChatService.sendMessage(user.getId(), correctnessSessionId, correctnessPrompt, questionId, null)
                .doOnNext(correctnessResponse::append)
                .blockLast(java.time.Duration.ofSeconds(30)); // 增加超时时间
            
            // 解析正确性判断结果
            String correctnessResult = correctnessResponse.toString().trim();
            boolean isCorrect = correctnessResult.contains("正确") && !correctnessResult.contains("错误");
            
            // 如果AI判断正确，更新答题记录
            if (isCorrect) {
                // 查找现有的答题记录并更新
                QueryWrapper<AnswerRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", user.getId())
                           .eq("question_id", questionId)
                           .orderByDesc("create_time")
                           .last("LIMIT 1");
                
                AnswerRecord existingRecord = answerRecordMapper.selectOne(queryWrapper);
                 if (existingRecord != null) {
                     existingRecord.setIsCorrect(1);
                     existingRecord.setScore(1);
                     answerRecordMapper.updateById(existingRecord);
                 }
            }
            
            // 调用AI服务进行详细判题（异步，不阻塞）
            StringBuilder aiResponse = new StringBuilder();
            try {
                aiChatService.sendMessage(user.getId(), sessionId, prompt, questionId, null)
                    .doOnNext(aiResponse::append)
                    .blockLast(java.time.Duration.ofSeconds(30)); // 增加超时时间
            } catch (Exception e) {
                // 如果详细判题失败，使用简单的判断结果
                aiResponse.append(isCorrect ? "AI判断：答案正确" : "AI判断：答案需要改进");
            }
            
            // 保存AI判题记录
            AiGradingRecord newRecord = aiGradingRecordService.saveGradingRecord(
                user.getId(), 
                questionId, 
                userAnswer, 
                aiResponse.toString(), 
                isCorrect
            );
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("gradingResult", aiResponse.toString());
            result.put("isCorrect", isCorrect);
            result.put("questionId", questionId);
            result.put("sessionId", sessionId);
            result.put("recordId", newRecord.getId());
            result.put("hasHistory", latestRecord != null);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("AI判题失败", e);
            return Result.error("AI判题失败：" + e.getMessage());
        }
    }
    
    @Override
    public void aiGradingStream(Map<String, Object> request, Long userId, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                // 获取请求参数
                Long questionId = Long.valueOf(request.get("questionId").toString());
                String questionContent = (String) request.get("questionContent");
                String userAnswer = (String) request.get("userAnswer");
                String correctAnswer = (String) request.get("correctAnswer");
                
                // 获取用户信息
                SysUser user = sysUserMapper.selectById(userId);
                if (user == null) {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("用户不存在"));
                    emitter.complete();
                    return;
                }
                
                // 检查是否有历史记录
                AiGradingRecord latestRecord = aiGradingRecordService.getLatestRecord(userId, questionId);
                
                // 如果是重新判题，软删除旧记录
                if (latestRecord != null) {
                    aiGradingRecordService.softDeleteOldRecords(userId, questionId);
                }
                
                // 获取AI判题前缀模板
                String prefixTemplate = sysConfigService.getConfigValue("ai.grading.prefix");
                if (prefixTemplate == null || prefixTemplate.trim().isEmpty()) {
                    prefixTemplate = "请对以下简答题进行判题评分：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请给出评分和详细的评价，并在最后一行明确标注：判断结果：正确 或 判断结果：错误";
                }
                
                // 替换模板中的占位符
                String prompt = prefixTemplate
                    .replace("{question}", questionContent)
                    .replace("{userAnswer}", userAnswer)
                    .replace("{correctAnswer}", correctAnswer != null ? correctAnswer : "无参考答案");
                
                // 生成会话ID
                String sessionId = "grading_" + questionId + "_" + userId + "_" + System.currentTimeMillis();
                
                // 调用AI服务进行流式判题
                StringBuilder aiResponse = new StringBuilder();
                boolean[] isCorrect = {false};
                
                aiChatService.sendMessage(userId, sessionId, prompt, questionId, null)
                    .doOnNext(chunk -> {
                        try {
                            aiResponse.append(chunk);
                            
                            // 发送流式数据到前端
                            Map<String, Object> data = new HashMap<>();
                            data.put("content", chunk);
                            data.put("type", "content");
                            
                            emitter.send(SseEmitter.event()
                                .name("data")
                                .data(new ObjectMapper().writeValueAsString(data)));
                                
                        } catch (Exception e) {
                            System.err.println("发送流式数据失败: " + e.getMessage());
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            // 判断答案是否正确
                            String fullResponse = aiResponse.toString();
                            isCorrect[0] = fullResponse.contains("判断结果：正确") || 
                                          (fullResponse.contains("正确") && !fullResponse.contains("错误"));
                            
                            // 如果AI判断正确，更新答题记录
                            if (isCorrect[0]) {
                                QueryWrapper<AnswerRecord> queryWrapper = new QueryWrapper<>();
                                queryWrapper.eq("user_id", userId)
                                           .eq("question_id", questionId)
                                           .orderByDesc("create_time")
                                           .last("LIMIT 1");
                                
                                AnswerRecord existingRecord = answerRecordMapper.selectOne(queryWrapper);
                                if (existingRecord != null) {
                                    existingRecord.setIsCorrect(1);
                                    existingRecord.setScore(1);
                                    answerRecordMapper.updateById(existingRecord);
                                }
                            }
                            
                            // 保存AI判题记录
                            AiGradingRecord newRecord = aiGradingRecordService.saveGradingRecord(
                                userId, 
                                questionId, 
                                userAnswer, 
                                fullResponse, 
                                isCorrect[0]
                            );
                            
                            // 发送最终结果
                            Map<String, Object> resultData = new HashMap<>();
                            resultData.put("isCorrect", isCorrect[0]);
                            resultData.put("score", isCorrect[0] ? 1 : 0);
                            resultData.put("recordId", newRecord.getId());
                            resultData.put("type", "result");
                            
                            emitter.send(SseEmitter.event()
                                .name("data")
                                .data(new ObjectMapper().writeValueAsString(resultData)));
                            
                            // 发送完成标记
                            emitter.send(SseEmitter.event()
                                .name("data")
                                .data("[DONE]"));
                            
                            emitter.complete();
                            
                        } catch (Exception e) {
                            System.err.println("完成AI判题处理失败: " + e.getMessage());
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnError(error -> {
                        System.err.println("AI判题流式处理失败: " + error.getMessage());
                        emitter.completeWithError(error);
                    })
                    .subscribe();
                    
            } catch (Exception e) {
                System.err.println("AI判题流式处理异常: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }
    
    @Override
    public Result<Map<String, Object>> getAiGradingHistory(Long questionId) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Result.unauthorized("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 获取历史记录
            List<AiGradingRecord> historyRecords = aiGradingRecordService.getHistoryRecords(user.getId(), questionId);
            AiGradingRecord latestRecord = aiGradingRecordService.getLatestRecord(user.getId(), questionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("historyRecords", historyRecords);
            result.put("latestRecord", latestRecord);
            result.put("hasHistory", !historyRecords.isEmpty());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取AI判题历史记录失败", e);
            return Result.error("获取AI判题历史记录失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<List<QuestionVO>> getPracticeQuestions(Long userId, Long subjectId, Integer questionType, 
                                                        Integer difficulty, Integer count, String mode, String excludeIds) {
        try {
            if (count == null || count <= 0) {
                count = 1; // 默认1道题
            }
            
            // 检查是否需要重新生成题目ID缓存
            boolean needGenerateCache = !practiceCacheService.hasCache(userId, subjectId, questionType, difficulty, mode);
            
            if (needGenerateCache) {
                // 生成题目ID缓存
                generateQuestionIdCache(userId, subjectId, questionType, difficulty, mode);
            }
            
            // 获取下一个题目ID
            Long nextQuestionId = practiceCacheService.getNextQuestionId(userId, subjectId, questionType, difficulty, mode);
            
            if (nextQuestionId == null) {
                // 所有题目已完成
                return Result.success(new ArrayList<>());
            }
            
            // 获取题目详情
            Question question = questionMapper.selectById(nextQuestionId);
            if (question == null) {
                return Result.success(new ArrayList<>());
            }
            
            // 转换为VO对象
            QuestionVO questionVO = convertToVO(question);
            List<QuestionVO> resultList = new ArrayList<>();
            resultList.add(questionVO);
            
            return Result.success(resultList);
            
        } catch (Exception e) {
            log.error("获取练习题目失败", e);
            return Result.error("获取练习题目失败：" + e.getMessage());
        }
    }
    
    @Override
    public List<QuestionVO> getRandomQuestions(Long subjectId, Integer count, List<String> difficulties) {
        try {
            // 构建查询条件
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            
            if (subjectId != null) {
                queryWrapper.eq("subject_id", subjectId);
            }
            
            if (difficulties != null && !difficulties.isEmpty()) {
                queryWrapper.in("difficulty", difficulties);
            }
            
            // 随机排序并限制数量
            queryWrapper.last("ORDER BY RAND() LIMIT " + count);
            
            List<Question> questions = questionMapper.selectList(queryWrapper);
            return questions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取随机题目失败", e);
            throw new RuntimeException("获取随机题目失败：" + e.getMessage());
        }
    }
}
