package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.dto.SubjectQueryRequest;
import com.qltiku2.dto.SubjectSaveRequest;
import com.qltiku2.entity.Subject;
import com.qltiku2.mapper.SubjectMapper;
import com.qltiku2.service.SubjectService;
import com.qltiku2.vo.SubjectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 科目服务实现类
 * 
 * @author qltiku2
 */
@Service
public class SubjectServiceImpl implements SubjectService {
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private com.qltiku2.mapper.QuestionMapper questionMapper;
    
    @Override
    public Result<IPage<SubjectVO>> getSubjectPage(SubjectQueryRequest request) {
        try {
            // 创建分页对象
            Page<Subject> page = new Page<>(request.getCurrent(), request.getSize());
            
            // 构建查询条件
            QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
            
            if (StringUtils.hasText(request.getKeyword())) {
                queryWrapper.like("name", request.getKeyword());
            }
            
            if (request.getStatus() != null) {
                queryWrapper.eq("status", request.getStatus());
            }
            
            // 排序
            String sortField = request.getSortField();
            // 将Java字段名转换为数据库字段名
            if ("sortOrder".equals(sortField)) {
                sortField = "sort_order";
            }
            
            if ("asc".equals(request.getSortOrder())) {
                queryWrapper.orderByAsc(sortField);
            } else {
                queryWrapper.orderByDesc(sortField);
            }
            
            // 执行查询
            IPage<Subject> subjectPage = subjectMapper.selectPage(page, queryWrapper);
            
            // 转换为VO
            IPage<SubjectVO> voPage = subjectPage.convert(this::convertToVO);
            
            return Result.success(voPage);
            
        } catch (Exception e) {
            return Result.error("查询科目列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<List<SubjectVO>> getEnabledSubjects() {
        try {
            QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1);
            queryWrapper.orderByAsc("sort_order");
            
            List<Subject> subjects = subjectMapper.selectList(queryWrapper);
            List<SubjectVO> subjectVOs = subjects.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return Result.success(subjectVOs);
            
        } catch (Exception e) {
            return Result.error("获取启用科目列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<SubjectVO> getSubjectById(Long id) {
        try {
            Subject subject = subjectMapper.selectById(id);
            if (subject == null) {
                return Result.error("科目不存在");
            }
            
            SubjectVO subjectVO = convertToVO(subject);
            return Result.success(subjectVO);
            
        } catch (Exception e) {
            return Result.error("获取科目详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> createSubject(SubjectSaveRequest request) {
        try {
            // 检查科目名称是否已存在
            QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", request.getName());
            Subject existingSubject = subjectMapper.selectOne(queryWrapper);
            if (existingSubject != null) {
                return Result.error("科目名称已存在");
            }
            
            // 创建科目实体
            Subject subject = new Subject();
            BeanUtils.copyProperties(request, subject);
            subject.setCreateTime(LocalDateTime.now());
            subject.setUpdateTime(LocalDateTime.now());
            
            // 保存科目
            int result = subjectMapper.insert(subject);
            if (result > 0) {
                return Result.success("科目创建成功");
            } else {
                return Result.error("科目创建失败");
            }
            
        } catch (Exception e) {
            return Result.error("创建科目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateSubject(Long id, SubjectSaveRequest request) {
        try {
            // 检查科目是否存在
            Subject existingSubject = subjectMapper.selectById(id);
            if (existingSubject == null) {
                return Result.error("科目不存在");
            }
            
            // 检查科目名称是否已被其他科目使用
            QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", request.getName());
            queryWrapper.ne("id", id);
            Subject duplicateSubject = subjectMapper.selectOne(queryWrapper);
            if (duplicateSubject != null) {
                return Result.error("科目名称已存在");
            }
            
            // 更新科目信息
            Subject subject = new Subject();
            BeanUtils.copyProperties(request, subject);
            subject.setId(id);
            subject.setUpdateTime(LocalDateTime.now());
            
            // 更新科目
            int result = subjectMapper.updateById(subject);
            if (result > 0) {
                return Result.success("科目更新成功");
            } else {
                return Result.error("科目更新失败");
            }
            
        } catch (Exception e) {
            return Result.error("更新科目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> deleteSubject(Long id) {
        try {
            // 检查科目是否存在
            Subject existingSubject = subjectMapper.selectById(id);
            if (existingSubject == null) {
                return Result.error("科目不存在");
            }
            
            // 检查是否有关联的题目
            // 这里可以添加检查逻辑，如果有关联题目则不允许删除
            
            // 删除科目（逻辑删除）
            int result = subjectMapper.deleteById(id);
            if (result > 0) {
                return Result.success("科目删除成功");
            } else {
                return Result.error("科目删除失败");
            }
            
        } catch (Exception e) {
            return Result.error("删除科目失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> batchDeleteSubjects(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.badRequest("请选择要删除的科目");
            }
            
            // 批量删除
            int result = subjectMapper.deleteBatchIds(ids);
            if (result > 0) {
                return Result.success("批量删除成功，共删除" + result + "个科目");
            } else {
                return Result.error("批量删除失败");
            }
            
        } catch (Exception e) {
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateSubjectStatus(Long id, Integer status) {
        try {
            // 检查科目是否存在
            Subject existingSubject = subjectMapper.selectById(id);
            if (existingSubject == null) {
                return Result.error("科目不存在");
            }
            
            // 更新状态
            Subject subject = new Subject();
            subject.setId(id);
            subject.setStatus(status);
            subject.setUpdateTime(LocalDateTime.now());
            
            int result = subjectMapper.updateById(subject);
            if (result > 0) {
                String statusName = status == 1 ? "启用" : "禁用";
                return Result.success("科目" + statusName + "成功");
            } else {
                return Result.error("更新科目状态失败");
            }
            
        } catch (Exception e) {
            return Result.error("更新科目状态失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateSubjectSort(Long id, Integer sortOrder) {
        try {
            // 检查科目是否存在
            Subject existingSubject = subjectMapper.selectById(id);
            if (existingSubject == null) {
                return Result.error("科目不存在");
            }
            
            // 更新排序
            Subject subject = new Subject();
            subject.setId(id);
            subject.setSortOrder(sortOrder);
            subject.setUpdateTime(LocalDateTime.now());
            
            int result = subjectMapper.updateById(subject);
            if (result > 0) {
                return Result.success("科目排序更新成功");
            } else {
                return Result.error("更新科目排序失败");
            }
            
        } catch (Exception e) {
            return Result.error("更新科目排序失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Object> getSubjectStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 总科目数
            Long totalCount = subjectMapper.selectCount(null);
            statistics.put("totalCount", totalCount);
            
            // 启用科目数
            QueryWrapper<Subject> enabledWrapper = new QueryWrapper<>();
            enabledWrapper.eq("status", 1);
            Long enabledCount = subjectMapper.selectCount(enabledWrapper);
            statistics.put("enabledCount", enabledCount);
            
            // 禁用科目数
            QueryWrapper<Subject> disabledWrapper = new QueryWrapper<>();
            disabledWrapper.eq("status", 0);
            Long disabledCount = subjectMapper.selectCount(disabledWrapper);
            statistics.put("disabledCount", disabledCount);
            
            // 科目题目统计（这里需要关联查询，暂时返回空列表）
            statistics.put("subjectQuestionStats", new ArrayList<>());
            
            return Result.success(statistics);
            
        } catch (Exception e) {
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 转换为VO对象
     */
    private SubjectVO convertToVO(Subject subject) {
        SubjectVO vo = new SubjectVO();
        BeanUtils.copyProperties(subject, vo);
        
        // 设置题目数量（通过关联查询获取）
        Integer questionCount = questionMapper.countQuestionsBySubjectId(subject.getId());
        vo.setQuestionCount(questionCount != null ? questionCount : 0);
        
        return vo;
    }
}