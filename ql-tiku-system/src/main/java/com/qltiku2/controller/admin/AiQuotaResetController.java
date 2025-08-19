package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.service.AiQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI配额重置控制器
 * 用于管理员手动触发配额重置任务
 */
@RestController
@RequestMapping("/admin/ai-quota")
public class AiQuotaResetController {

    @Autowired
    private AiQuotaService aiQuotaService;

    /**
     * 手动触发每日配额重置
     */
    @PostMapping("/reset-daily")
    public Result<String> resetDailyQuota() {
        try {
            aiQuotaService.resetDailyQuota();
            return Result.success("每日配额重置成功");
        } catch (Exception e) {
            return Result.error("每日配额重置失败：" + e.getMessage());
        }
    }

    /**
     * 手动触发每月配额重置
     */
    @PostMapping("/reset-monthly")
    public Result<String> resetMonthlyQuota() {
        try {
            aiQuotaService.resetMonthlyQuota();
            return Result.success("每月配额重置成功");
        } catch (Exception e) {
            return Result.error("每月配额重置失败：" + e.getMessage());
        }
    }
}