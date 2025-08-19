package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.dto.FeedbackQueryRequest;
import com.qltiku2.dto.FeedbackSaveRequest;
import com.qltiku2.entity.Feedback;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.FeedbackMapper;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.service.FeedbackService;
import com.qltiku2.service.OssService;
import com.qltiku2.vo.FeedbackVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 反馈服务实现类
 * 
 * @author qltiku2
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {
    
    @Autowired
    private FeedbackMapper feedbackMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private OssService ossService;
    
    @Override
    public Result<IPage<FeedbackVO>> getFeedbackPage(FeedbackQueryRequest request) {
        try {
            // 创建分页对象
            Page<Feedback> page = new Page<>(request.getCurrent(), request.getSize());
            
            // 构建查询条件
            QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("f.deleted", 0);
            
            if (request.getUserId() != null) {
                queryWrapper.eq("f.user_id", request.getUserId());
            }
            
            if (request.getFeedbackType() != null) {
                queryWrapper.eq("f.feedback_type", request.getFeedbackType());
            }
            
            if (request.getStatus() != null) {
                queryWrapper.eq("f.status", request.getStatus());
            }
            
            if (StringUtils.hasText(request.getKeyword())) {
                queryWrapper.and(wrapper -> wrapper
                    .like("f.title", request.getKeyword())
                    .or()
                    .like("f.content", request.getKeyword())
                );
            }
            
            if (StringUtils.hasText(request.getUsername())) {
                queryWrapper.like("u.username", request.getUsername());
            }
            
            if (StringUtils.hasText(request.getStartTime())) {
                queryWrapper.ge("f.create_time", request.getStartTime());
            }
            
            if (StringUtils.hasText(request.getEndTime())) {
                queryWrapper.le("f.create_time", request.getEndTime());
            }
            
            // 排序
            String sortField = request.getSortField();
            if ("createTime".equals(sortField)) {
                sortField = "f.create_time";
            } else if ("updateTime".equals(sortField)) {
                sortField = "f.update_time";
            } else {
                sortField = "f." + sortField;
            }
            
            if ("asc".equals(request.getSortOrder())) {
                queryWrapper.orderByAsc(sortField);
            } else {
                queryWrapper.orderByDesc(sortField);
            }
            
            // 执行查询
            IPage<Feedback> feedbackPage = feedbackMapper.selectFeedbackPage(page, queryWrapper);
            
            // 转换为VO
            IPage<FeedbackVO> voPage = feedbackPage.convert(this::convertToVO);
            
            return Result.success(voPage);
            
        } catch (Exception e) {
            return Result.error("查询反馈列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<FeedbackVO> getFeedbackById(Long id) {
        try {
            Feedback feedback = feedbackMapper.selectById(id);
            if (feedback == null || feedback.getDeleted() == 1) {
                return Result.error("反馈不存在");
            }
            
            FeedbackVO vo = convertToVO(feedback);
            return Result.success(vo);
            
        } catch (Exception e) {
            return Result.error("获取反馈详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> createFeedback(FeedbackSaveRequest request, Long userId) {
        try {
            // 验证用户是否存在
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || user.getDeleted() == 1) {
                return Result.error("用户不存在");
            }
            
            Feedback feedback = new Feedback();
            BeanUtils.copyProperties(request, feedback);
            feedback.setUserId(userId);
            feedback.setStatus(0); // 默认待处理状态
            feedback.setCreateTime(LocalDateTime.now());
            feedback.setUpdateTime(LocalDateTime.now());
            feedback.setDeleted(0);
            
            // 处理图片列表
            if (request.getImageList() != null && !request.getImageList().isEmpty()) {
                feedback.setImages(String.join(",", request.getImageList()));
            }
            
            int result = feedbackMapper.insert(feedback);
            if (result > 0) {
                return Result.success("反馈提交成功");
            } else {
                return Result.error("反馈提交失败");
            }
            
        } catch (Exception e) {
            return Result.error("反馈提交失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateFeedback(Long id, FeedbackSaveRequest request, Long userId) {
        try {
            Feedback feedback = feedbackMapper.selectById(id);
            if (feedback == null || feedback.getDeleted() == 1) {
                return Result.error("反馈不存在");
            }
            
            // 检查权限：只有反馈创建者且状态为待处理时才能修改
            if (!feedback.getUserId().equals(userId)) {
                return Result.error("无权限修改此反馈");
            }
            
            if (feedback.getStatus() != 0) {
                return Result.error("只有待处理状态的反馈才能修改");
            }
            
            // 更新字段
            if (StringUtils.hasText(request.getTitle())) {
                feedback.setTitle(request.getTitle());
            }
            if (StringUtils.hasText(request.getContent())) {
                feedback.setContent(request.getContent());
            }
            if (request.getFeedbackType() != null) {
                feedback.setFeedbackType(request.getFeedbackType());
            }
            if (request.getImageList() != null) {
                feedback.setImages(String.join(",", request.getImageList()));
            }
            
            feedback.setUpdateTime(LocalDateTime.now());
            
            int result = feedbackMapper.updateById(feedback);
            if (result > 0) {
                return Result.success("反馈更新成功");
            } else {
                return Result.error("反馈更新失败");
            }
            
        } catch (Exception e) {
            return Result.error("反馈更新失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> deleteFeedback(Long id, Long userId) {
        try {
            Feedback feedback = feedbackMapper.selectById(id);
            if (feedback == null || feedback.getDeleted() == 1) {
                return Result.error("反馈不存在");
            }
            
            // 检查权限：只有反馈创建者且状态为待处理时才能删除
            if (!feedback.getUserId().equals(userId)) {
                return Result.error("无权限删除此反馈");
            }
            
            if (feedback.getStatus() != 0) {
                return Result.error("只有待处理状态的反馈才能删除");
            }
            
            // 软删除
            feedback.setDeleted(1);
            feedback.setUpdateTime(LocalDateTime.now());
            
            int result = feedbackMapper.updateById(feedback);
            if (result > 0) {
                return Result.success("反馈删除成功");
            } else {
                return Result.error("反馈删除失败");
            }
            
        } catch (Exception e) {
            return Result.error("反馈删除失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> batchDeleteFeedbacks(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的反馈");
            }
            
            // 批量软删除
            LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Feedback::getId, ids);
            
            Feedback updateEntity = new Feedback();
            updateEntity.setDeleted(1);
            updateEntity.setUpdateTime(LocalDateTime.now());
            
            int result = feedbackMapper.update(updateEntity, wrapper);
            if (result > 0) {
                return Result.success("批量删除成功");
            } else {
                return Result.error("批量删除失败");
            }
            
        } catch (Exception e) {
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateFeedbackStatus(Long id, Integer status, String adminReply) {
        try {
            Feedback feedback = feedbackMapper.selectById(id);
            if (feedback == null || feedback.getDeleted() == 1) {
                return Result.error("反馈不存在");
            }
            
            feedback.setStatus(status);
            if (StringUtils.hasText(adminReply)) {
                feedback.setAdminReply(adminReply);
            }
            feedback.setUpdateTime(LocalDateTime.now());
            
            int result = feedbackMapper.updateById(feedback);
            if (result > 0) {
                return Result.success("状态更新成功");
            } else {
                return Result.error("状态更新失败");
            }
            
        } catch (Exception e) {
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<List<FeedbackVO>> getUserFeedbacks(Long userId) {
        try {
            List<Feedback> feedbacks = feedbackMapper.selectByUserId(userId);
            List<FeedbackVO> voList = feedbacks.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return Result.success(voList);
            
        } catch (Exception e) {
            return Result.error("获取用户反馈列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<Map<String, Object>> getFeedbackStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 总反馈数
            LambdaQueryWrapper<Feedback> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.eq(Feedback::getDeleted, 0);
            long totalCount = feedbackMapper.selectCount(totalWrapper);
            statistics.put("totalCount", totalCount);
            
            // 各状态统计
            List<Map<String, Object>> statusStats = feedbackMapper.countByStatus();
            Map<String, Integer> statusMap = new HashMap<>();
            for (Map<String, Object> stat : statusStats) {
                Integer status = (Integer) stat.get("status");
                Long count = (Long) stat.get("count");
                statusMap.put(getStatusName(status), count.intValue());
            }
            statistics.put("statusStats", statusMap);
            
            // 各类型统计
            List<Map<String, Object>> typeStats = feedbackMapper.countByType();
            Map<String, Integer> typeMap = new HashMap<>();
            for (Map<String, Object> stat : typeStats) {
                Integer type = (Integer) stat.get("feedback_type");
                Long count = (Long) stat.get("count");
                typeMap.put(getFeedbackTypeName(type), count.intValue());
            }
            statistics.put("typeStats", typeMap);
            
            return Result.success(statistics);
            
        } catch (Exception e) {
            return Result.error("获取反馈统计失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<List<FeedbackVO>> getRecentFeedbacks(Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }
            
            List<Feedback> feedbacks = feedbackMapper.selectRecentFeedbacks(limit);
            List<FeedbackVO> voList = feedbacks.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return Result.success(voList);
            
        } catch (Exception e) {
            return Result.error("获取最近反馈失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> uploadFeedbackImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("请选择要上传的图片");
            }
            
            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isImageFile(originalFilename)) {
                return Result.error("只支持上传图片文件");
            }
            
            // 检查文件大小（限制5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }
            
            // 获取文件扩展名
            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 按日期分目录存储
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String datePath = sdf.format(new Date());
            
            // 生成时间戳
            long timestamp = System.currentTimeMillis();
            
            // 生成UUID（取前8位）
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            
            // 生成唯一文件名：feedback/年/月/日/时间戳_UUID.扩展名
            String fileName = "feedback/" + datePath + "/" + timestamp + "_" + uuid + extension;
            
            // 上传到OSS
            String imageUrl = ossService.uploadFile(file, fileName);
            return Result.success(imageUrl);
            
        } catch (Exception e) {
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 转换为VO对象
     */
    private FeedbackVO convertToVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        
        // 设置反馈类型名称
        vo.setFeedbackTypeName(getFeedbackTypeName(feedback.getFeedbackType()));
        
        // 设置状态名称
        vo.setStatusName(getStatusName(feedback.getStatus()));
        
        // 处理图片列表
        if (StringUtils.hasText(feedback.getImages())) {
            vo.setImageList(Arrays.asList(feedback.getImages().split(",")));
        } else {
            vo.setImageList(new ArrayList<>());
        }
        
        // 获取用户名
        if (feedback.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(feedback.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }
        }
        
        return vo;
    }
    
    /**
     * 获取反馈类型名称
     */
    private String getFeedbackTypeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case 1:
                return "Bug反馈";
            case 2:
                return "功能建议";
            case 3:
                return "其他反馈";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待处理";
            case 1:
                return "已受理";
            case 2:
                return "已处理";
            case 3:
                return "已修复";
            case 4:
                return "已采纳";
            case 5:
                return "已失效";
            case 6:
                return "已撤销";
            default:
                return "未知";
        }
    }
    
    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }
}