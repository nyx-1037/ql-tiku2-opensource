package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.entity.RegistrationCode;
import com.qltiku2.service.RegistrationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端注册码管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/registration-codes")
@CrossOrigin(origins = "*")
public class AdminRegistrationCodeController {
    
    @Autowired
    private RegistrationCodeService registrationCodeService;
    
    /**
     * 获取注册码列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<RegistrationCode>> getRegistrationCodes(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        
        try {
            Page<RegistrationCode> pageParam = new Page<>(current, size);
            QueryWrapper<RegistrationCode> queryWrapper = new QueryWrapper<>();
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                    .like("code", keyword)
                    .or()
                    .like("created_by", keyword)
                );
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc("created_time");
            
            IPage<RegistrationCode> page = registrationCodeService.page(pageParam, queryWrapper);
            return Result.success(page);
            
        } catch (Exception e) {
            return Result.error("获取注册码列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取注册码详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RegistrationCode> getRegistrationCode(@PathVariable Long id) {
        try {
            RegistrationCode code = registrationCodeService.getById(id);
            if (code == null) {
                return Result.error("注册码不存在");
            }
            return Result.success(code);
        } catch (Exception e) {
            return Result.error("获取注册码详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成单个注册码
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RegistrationCode> generateCode(@RequestBody Map<String, Object> request) {
        try {
            Integer maxUses = (Integer) request.get("maxUses");
            Integer validDays = (Integer) request.get("validDays");
            Integer membershipLevel = (Integer) request.get("membershipLevel");
            
            if (maxUses == null || validDays == null) {
                return Result.badRequest("参数不完整");
            }
            
            if (membershipLevel == null) {
                membershipLevel = 0; // 默认普通用户
            }
            
            RegistrationCode code = registrationCodeService.generateCode(maxUses, validDays, membershipLevel);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error("生成注册码失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量生成注册码
     */
    @PostMapping("/batch-generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RegistrationCode>> batchGenerateCode(@RequestBody Map<String, Object> request) {
        try {
            Integer count = (Integer) request.get("count");
            Integer maxUses = (Integer) request.get("maxUses");
            Integer validDays = (Integer) request.get("validDays");
            Integer membershipLevel = (Integer) request.get("membershipLevel");
            
            if (count == null || maxUses == null || validDays == null) {
                return Result.badRequest("参数不完整");
            }
            
            if (membershipLevel == null) {
                membershipLevel = 0; // 默认普通用户
            }
            
            List<RegistrationCode> codes = registrationCodeService.batchGenerateCode(count, maxUses, validDays, membershipLevel);
            return Result.success(codes);
        } catch (Exception e) {
            return Result.error("批量生成注册码失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新注册码状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Integer status = (Integer) request.get("status");
            if (status == null) {
                return Result.badRequest("状态不能为空");
            }
            
            boolean success = registrationCodeService.updateStatus(id, status == 1);
            if (success) {
                return Result.success("状态更新成功");
            } else {
                return Result.error("状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除注册码
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteCode(@PathVariable Long id) {
        try {
            boolean success = registrationCodeService.removeById(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除注册码
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDelete(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.badRequest("请选择要删除的记录");
            }
            
            boolean success = registrationCodeService.removeByIds(ids);
            if (success) {
                return Result.success("批量删除成功");
            } else {
                return Result.error("批量删除失败");
            }
        } catch (Exception e) {
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取注册码使用情况统计
     */
    @GetMapping("/usage")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getUsageStats() {
        try {
            Map<String, Object> stats = registrationCodeService.getUsageStats();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取使用统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 验证注册码
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> validateCode(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            if (!StringUtils.hasText(code)) {
                return Result.badRequest("注册码不能为空");
            }
            
            boolean isValid = registrationCodeService.validateCode(code);
            return Result.success(isValid);
        } catch (Exception e) {
            return Result.error("验证失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取注册码统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = registrationCodeService.getRegistrationCodeStats();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取注册码使用记录
     */
    @GetMapping("/{id}/usage")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<Map<String, Object>>> getRegistrationCodeUsage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            IPage<Map<String, Object>> usageRecords = registrationCodeService.getUsageRecords(id, current, size);
            return Result.success(usageRecords);
        } catch (Exception e) {
            return Result.error("获取使用记录失败：" + e.getMessage());
        }
    }
}
