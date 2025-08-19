package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.SubjectQueryRequest;
import com.qltiku2.dto.SubjectSaveRequest;
import com.qltiku2.vo.SubjectVO;

import java.util.List;

/**
 * 科目服务接口
 * 
 * @author qltiku2
 */
public interface SubjectService {
    
    /**
     * 分页查询科目列表
     */
    Result<IPage<SubjectVO>> getSubjectPage(SubjectQueryRequest request);
    
    /**
     * 获取所有启用的科目列表
     */
    Result<List<SubjectVO>> getEnabledSubjects();
    
    /**
     * 根据ID获取科目详情
     */
    Result<SubjectVO> getSubjectById(Long id);
    
    /**
     * 创建科目
     */
    Result<String> createSubject(SubjectSaveRequest request);
    
    /**
     * 更新科目
     */
    Result<String> updateSubject(Long id, SubjectSaveRequest request);
    
    /**
     * 删除科目
     */
    Result<String> deleteSubject(Long id);
    
    /**
     * 批量删除科目
     */
    Result<String> batchDeleteSubjects(List<Long> ids);
    
    /**
     * 更新科目状态
     */
    Result<String> updateSubjectStatus(Long id, Integer status);
    
    /**
     * 更新科目排序
     */
    Result<String> updateSubjectSort(Long id, Integer sortOrder);
    
    /**
     * 获取科目统计信息
     */
    Result<Object> getSubjectStatistics();
}