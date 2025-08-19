package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.FeedbackQueryRequest;
import com.qltiku2.dto.FeedbackSaveRequest;
import com.qltiku2.service.FeedbackService;
import com.qltiku2.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 反馈管理控制器（管理端）
 * 
 * @author qltiku2
 */
@Tag(name = "反馈管理（管理端）", description = "管理端反馈相关接口")
@RestController
@RequestMapping("/api/admin/feedback")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {
    
    @Autowired
    private FeedbackService feedbackService;
    
    @Operation(summary = "分页查询反馈列表")
    @PostMapping("/page")
    public Result<IPage<FeedbackVO>> getFeedbackPage(@RequestBody FeedbackQueryRequest request) {
        try {
            return feedbackService.getFeedbackPage(request);
            
        } catch (Exception e) {
            return Result.error("查询反馈列表失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取反馈详情")
    @GetMapping("/{id}")
    public Result<FeedbackVO> getFeedbackById(@PathVariable Long id) {
        try {
            return feedbackService.getFeedbackById(id);
            
        } catch (Exception e) {
            return Result.error("获取反馈详情失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "创建反馈（管理员代创建）")
    @PostMapping("/create")
    public Result<String> createFeedback(@Valid @RequestBody FeedbackSaveRequest request) {
        try {
            // 管理员代创建时，需要指定用户ID
            if (request.getId() == null) {
                return Result.error("请指定用户ID");
            }
            
            return feedbackService.createFeedback(request, request.getId());
            
        } catch (Exception e) {
            return Result.error("创建反馈失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "更新反馈状态")
    @PutMapping("/{id}/status")
    public Result<String> updateFeedbackStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            Integer status = (Integer) params.get("status");
            String adminReply = (String) params.get("adminReply");
            
            if (status == null) {
                return Result.error("状态不能为空");
            }
            
            return feedbackService.updateFeedbackStatus(id, status, adminReply);
            
        } catch (Exception e) {
            return Result.error("更新状态失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "批量删除反馈")
    @DeleteMapping("/batch")
    public Result<String> batchDeleteFeedbacks(@RequestBody Map<String, List<Long>> params) {
        try {
            List<Long> ids = params.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的反馈");
            }
            
            return feedbackService.batchDeleteFeedbacks(ids);
            
        } catch (Exception e) {
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "删除单个反馈")
    @DeleteMapping("/{id}")
    public Result<String> deleteFeedback(@PathVariable Long id) {
        try {
            return feedbackService.batchDeleteFeedbacks(List.of(id));
            
        } catch (Exception e) {
            return Result.error("删除反馈失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取反馈统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFeedbackStatistics() {
        try {
            return feedbackService.getFeedbackStatistics();
            
        } catch (Exception e) {
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取最近反馈")
    @GetMapping("/recent")
    public Result<List<FeedbackVO>> getRecentFeedbacks(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            return feedbackService.getRecentFeedbacks(limit);
            
        } catch (Exception e) {
            return Result.error("获取最近反馈失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取反馈类型选项")
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getFeedbackTypes() {
        try {
            List<Map<String, Object>> types = List.of(
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
    public Result<List<Map<String, Object>>> getFeedbackStatuses() {
        try {
            List<Map<String, Object>> statuses = List.of(
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
    
    @Operation(summary = "批量更新状态")
    @PutMapping("/batch/status")
    public Result<String> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) params.get("ids");
            Integer status = (Integer) params.get("status");
            String adminReply = (String) params.get("adminReply");
            
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要更新的反馈");
            }
            
            if (status == null) {
                return Result.error("状态不能为空");
            }
            
            int successCount = 0;
            for (Long id : ids) {
                Result<String> result = feedbackService.updateFeedbackStatus(id, status, adminReply);
                if (result.isSuccess()) {
                    successCount++;
                }
            }
            
            return Result.success("批量更新完成，成功更新 " + successCount + " 条记录");
            
        } catch (Exception e) {
            return Result.error("批量更新状态失败：" + e.getMessage());
        }
    }
}