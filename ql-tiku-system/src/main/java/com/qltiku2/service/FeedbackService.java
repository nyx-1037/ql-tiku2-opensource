package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.FeedbackQueryRequest;
import com.qltiku2.dto.FeedbackSaveRequest;
import com.qltiku2.vo.FeedbackVO;

import java.util.List;
import java.util.Map;

/**
 * 反馈服务接口
 * 
 * @author qltiku2
 */
public interface FeedbackService {
    
    /**
     * 分页查询反馈列表
     */
    Result<IPage<FeedbackVO>> getFeedbackPage(FeedbackQueryRequest request);
    
    /**
     * 根据ID获取反馈详情
     */
    Result<FeedbackVO> getFeedbackById(Long id);
    
    /**
     * 创建反馈
     */
    Result<String> createFeedback(FeedbackSaveRequest request, Long userId);
    
    /**
     * 更新反馈
     */
    Result<String> updateFeedback(Long id, FeedbackSaveRequest request, Long userId);
    
    /**
     * 删除反馈
     */
    Result<String> deleteFeedback(Long id, Long userId);
    
    /**
     * 批量删除反馈（管理端）
     */
    Result<String> batchDeleteFeedbacks(List<Long> ids);
    
    /**
     * 更新反馈状态（管理端）
     */
    Result<String> updateFeedbackStatus(Long id, Integer status, String adminReply);
    
    /**
     * 获取用户的反馈列表
     */
    Result<List<FeedbackVO>> getUserFeedbacks(Long userId);
    
    /**
     * 获取反馈统计信息
     */
    Result<Map<String, Object>> getFeedbackStatistics();
    
    /**
     * 获取最近的反馈列表
     */
    Result<List<FeedbackVO>> getRecentFeedbacks(Integer limit);
    
    /**
     * 上传反馈图片到OSS
     */
    Result<String> uploadFeedbackImage(org.springframework.web.multipart.MultipartFile file);
}