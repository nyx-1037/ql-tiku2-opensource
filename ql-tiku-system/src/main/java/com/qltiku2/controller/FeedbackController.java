package com.qltiku2.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.FeedbackQueryRequest;
import com.qltiku2.dto.FeedbackSaveRequest;
import com.qltiku2.service.FeedbackService;
import com.qltiku2.utils.JwtUtils;
import com.qltiku2.vo.FeedbackVO;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 反馈控制器（客户端）
 * 
 * @author qltiku2
 */
@Tag(name = "反馈管理", description = "反馈相关接口")
@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    
    @Autowired
    private FeedbackService feedbackService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Operation(summary = "获取我的反馈列表")
    @PostMapping("/my/page")
    public Result<IPage<FeedbackVO>> getMyFeedbackPage(@RequestBody FeedbackQueryRequest request, HttpServletRequest httpRequest) {
        try {
            // 获取当前用户ID
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("用户未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            // 设置用户ID过滤
            request.setUserId(userId);
            
            return feedbackService.getFeedbackPage(request);
            
        } catch (Exception e) {
            return Result.error("获取反馈列表失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取我的反馈列表（不分页）")
    @GetMapping("/my/list")
    public Result<List<FeedbackVO>> getMyFeedbacks(HttpServletRequest request) {
        try {
            // 获取当前用户ID
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("用户未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            return feedbackService.getUserFeedbacks(userId);
            
        } catch (Exception e) {
            return Result.error("获取反馈列表失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取反馈详情")
    @GetMapping("/{id}")
    public Result<FeedbackVO> getFeedbackById(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 获取当前用户ID
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("用户未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            Result<FeedbackVO> result = feedbackService.getFeedbackById(id);
            if (result.isSuccess()) {
                FeedbackVO feedback = result.getData();
                // 检查权限：只能查看自己的反馈
                if (!feedback.getUserId().equals(userId)) {
                    return Result.error("无权限查看此反馈");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            return Result.error("获取反馈详情失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "提交反馈")
    @PostMapping("/submit")
    public Result<String> submitFeedback(@Valid @RequestBody FeedbackSaveRequest request, HttpServletRequest httpRequest) {
        try {
            // 获取当前用户ID
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("用户未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            return feedbackService.createFeedback(request, userId);
            
        } catch (Exception e) {
            return Result.error("提交反馈失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "更新反馈")
    @PutMapping("/{id}")
    public Result<String> updateFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackSaveRequest request, HttpServletRequest httpRequest) {
        try {
            // 获取当前用户ID
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("用户未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            return feedbackService.updateFeedback(id, request, userId);
            
        } catch (Exception e) {
            return Result.error("更新反馈失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "删除反馈")
    @DeleteMapping("/{id}")
    public Result<String> deleteFeedback(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 获取当前用户ID
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("用户未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            
            return feedbackService.deleteFeedback(id, userId);
            
        } catch (Exception e) {
            return Result.error("删除反馈失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "上传反馈图片")
    @PostMapping("/upload/image")
    public Result<String> uploadFeedbackImage(@RequestParam("file") MultipartFile file) {
        try {
            return feedbackService.uploadFeedbackImage(file);
            
        } catch (Exception e) {
            return Result.error("上传图片失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取反馈类型选项")
    @GetMapping("/types")
    public Result<List<Object>> getFeedbackTypes() {
        try {
            List<Object> types = List.of(
                Map.of("value", 1, "label", "Bug反馈"),
                Map.of("value", 2, "label", "功能建议"),
                Map.of("value", 3, "label", "其他反馈")
            );
            return Result.success(types);
            
        } catch (Exception e) {
            return Result.error("获取反馈类型失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取状态选项")
    @GetMapping("/statuses")
    public Result<List<Object>> getFeedbackStatuses() {
        try {
            List<Object> statuses = List.of(
                Map.of("value", 0, "label", "待处理"),
                Map.of("value", 1, "label", "已受理"),
                Map.of("value", 2, "label", "已处理"),
                Map.of("value", 3, "label", "已修复"),
                Map.of("value", 4, "label", "已采纳"),
                Map.of("value", 5, "label", "已失效"),
                Map.of("value", 6, "label", "已撤销")
            );
            return Result.success(statuses);
            
        } catch (Exception e) {
            return Result.error("获取状态选项失败：" + e.getMessage());
        }
    }
}