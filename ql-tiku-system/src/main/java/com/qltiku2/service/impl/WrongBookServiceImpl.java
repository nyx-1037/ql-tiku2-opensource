package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.common.Result;
import com.qltiku2.entity.Question;
import com.qltiku2.entity.WrongQuestion;
import com.qltiku2.mapper.QuestionMapper;
import com.qltiku2.mapper.WrongQuestionMapper;
import com.qltiku2.service.WrongBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 错题本服务实现类
 */
@Service
public class WrongBookServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion> implements WrongBookService {

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public Result getWrongQuestions(Long userId, Integer page, Integer size, Long subjectId, String type, String difficulty, String wrongType) {
        try {
            // 构建查询条件
            QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            
            // 添加错题类型过滤
            if (StringUtils.hasText(wrongType)) {
                wrapper.eq("wrong_type", wrongType);
            }
            
            wrapper.orderByDesc("create_time");

            // 分页查询
            Page<WrongQuestion> pageObj = new Page<>(page, size);
            IPage<WrongQuestion> pageResult = wrongQuestionMapper.selectPage(pageObj, wrapper);

            // 获取题目详细信息
            List<WrongQuestion> wrongQuestions = pageResult.getRecords();
            List<Map<String, Object>> questionList = wrongQuestions.stream().map(wq -> {
                Map<String, Object> questionMap = new HashMap<>();
                
                // 获取题目详情
                Question question = questionMapper.selectById(wq.getQuestionId());
                if (question != null) {
                    questionMap.put("id", wq.getId());
                    questionMap.put("questionId", question.getId());
                    questionMap.put("content", question.getContent());
                    questionMap.put("type", question.getQuestionType());
                    questionMap.put("difficulty", question.getDifficulty());
                    questionMap.put("options", parseOptions(question.getOptions()));
                    questionMap.put("correctAnswer", question.getCorrectAnswer());
                    questionMap.put("explanation", question.getAnalysis());
                    questionMap.put("subjectName", question.getSubjectName());
                    questionMap.put("userAnswer", ""); // WrongQuestion没有userAnswer字段
                    questionMap.put("wrongTime", wq.getCreateTime());
                    questionMap.put("wrongType", wq.getWrongType());
                    questionMap.put("wrongCount", wq.getWrongCount());
                }
                
                return questionMap;
            }).collect(Collectors.toList());

            // 统计信息
            QueryWrapper<WrongQuestion> totalWrapper = new QueryWrapper<>();
            totalWrapper.eq("user_id", userId);
            long totalWrongCount = wrongQuestionMapper.selectCount(totalWrapper);
            
            // 这里简化处理，假设复习过的题目数量为0
            long reviewedCount = 0;

            Map<String, Object> result = new HashMap<>();
            result.put("content", questionList);
            result.put("total", pageResult.getTotal());
            result.put("totalWrongCount", totalWrongCount);
            result.put("reviewedCount", reviewedCount);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("totalPages", pageResult.getPages());

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取错题列表失败");
        }
    }

    @Override
    public Result removeWrongQuestion(Long userId, Long questionId) {
        try {
            QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("id", questionId);
            
            int result = wrongQuestionMapper.delete(wrapper);
            if (result > 0) {
                return Result.success("移除成功");
            } else {
                return Result.error("移除失败，题目不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("移除错题失败");
        }
    }

    @Override
    public Result addWrongQuestion(Long userId, Long questionId, String userAnswer, String wrongType) {
        try {
            // 检查是否已存在相同用户ID和题目ID的记录
            QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("question_id", questionId);
            
            WrongQuestion existing = wrongQuestionMapper.selectOne(wrapper);
            if (existing != null) {
                // 更新错误次数、时间和错题类型
                existing.setWrongCount(existing.getWrongCount() + 1);
                existing.setLastWrongTime(LocalDateTime.now());
                // 如果传入了新的错题类型，则更新
                if (StringUtils.hasText(wrongType)) {
                    existing.setWrongType(wrongType);
                }
                existing.setUpdateTime(LocalDateTime.now());
                wrongQuestionMapper.updateById(existing);
            } else {
                // 获取题目信息
                Question question = questionMapper.selectById(questionId);
                if (question == null) {
                    return Result.error("题目不存在");
                }
                
                // 创建新的错题记录
                WrongQuestion wrongQuestion = new WrongQuestion();
                wrongQuestion.setUserId(userId);
                wrongQuestion.setQuestionId(questionId);
                wrongQuestion.setWrongType(StringUtils.hasText(wrongType) ? wrongType : "PRACTICE");
                wrongQuestion.setWrongCount(1);
                wrongQuestion.setIsMastered(0);
                wrongQuestion.setLastWrongTime(LocalDateTime.now());
                wrongQuestion.setCreateTime(LocalDateTime.now());
                wrongQuestion.setUpdateTime(LocalDateTime.now());
                
                wrongQuestionMapper.insert(wrongQuestion);
            }
            
            return Result.success("添加错题成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加错题失败");
        }
    }

    @Override
    public Result clearWrongQuestions(Long userId) {
        try {
            QueryWrapper<WrongQuestion> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            
            wrongQuestionMapper.delete(wrapper);
            return Result.success("清空错题本成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("清空错题本失败");
        }
    }

    /**
     * 解析选项字符串为选项列表
     */
    private Object parseOptions(String optionsStr) {
        if (!StringUtils.hasText(optionsStr)) {
            return null;
        }
        
        try {
            // 简单解析选项，假设格式为 "A.选项1;B.选项2;C.选项3;D.选项4"
            String[] options = optionsStr.split(";");
            return java.util.Arrays.stream(options)
                .map(option -> {
                    String[] parts = option.split("\\.", 2);
                    if (parts.length == 2) {
                        Map<String, String> optionMap = new HashMap<>();
                        optionMap.put("key", parts[0].trim());
                        optionMap.put("value", parts[1].trim());
                        return optionMap;
                    }
                    return null;
                })
                .filter(option -> option != null)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return optionsStr;
        }
    }
}