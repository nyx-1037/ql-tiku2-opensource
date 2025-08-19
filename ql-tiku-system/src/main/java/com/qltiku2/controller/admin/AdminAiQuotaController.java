package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.entity.UserAiQuota;
import com.qltiku2.entity.SysUser;
import com.qltiku2.service.AiQuotaService;
import com.qltiku2.service.UserService;
import com.qltiku2.vo.UserAiQuotaVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端AI配额管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/ai/quotas")
@CrossOrigin(origins = "*")
public class AdminAiQuotaController {
    
    @Autowired
    private AiQuotaService aiQuotaService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取用户AI配额列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<UserAiQuotaVO>> getUserQuotas(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer vipLevel) {
        
        try {
            Page<UserAiQuota> pageParam = new Page<>(page, size);
            QueryWrapper<UserAiQuota> queryWrapper = new QueryWrapper<>();
            
            // 关键词搜索（用户ID或用户名）
            if (StringUtils.hasText(keyword)) {
                // 先根据用户名查找用户ID
                QueryWrapper<SysUser> userQueryWrapper = new QueryWrapper<>();
                userQueryWrapper.like("username", keyword)
                               .or().like("nickname", keyword);
                List<SysUser> users = userService.list(userQueryWrapper);
                List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
                
                if (!userIds.isEmpty()) {
                    queryWrapper.in("user_id", userIds);
                } else {
                    // 如果没有找到用户，尝试按用户ID搜索
                    try {
                        Long userId = Long.parseLong(keyword);
                        queryWrapper.eq("user_id", userId);
                    } catch (NumberFormatException e) {
                        // 如果不是数字，返回空结果
                        return Result.success(new Page<>(page, size, 0));
                    }
                }
            }
            
            // VIP等级筛选
            if (vipLevel != null) {
                queryWrapper.eq("vip_level", vipLevel);
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc("created_time");
            
            IPage<UserAiQuota> quotaPage = aiQuotaService.page(pageParam, queryWrapper);
            
            // 转换为VO对象
            Page<UserAiQuotaVO> voPage = new Page<>(page, size, quotaPage.getTotal());
            List<UserAiQuotaVO> voList = quotaPage.getRecords().stream().map(quota -> {
                UserAiQuotaVO vo = new UserAiQuotaVO();
                vo.setId(quota.getId());
                vo.setUserId(quota.getUserId());
                vo.setVipLevel(quota.getVipLevel());
                vo.setDailyQuota(quota.getDailyQuota());
                vo.setUsedDaily(quota.getUsedDaily());
                vo.setMonthlyQuota(quota.getMonthlyQuota());
                vo.setUsedMonthly(quota.getUsedMonthly());
                vo.setCreateTime(quota.getCreateTime());
                vo.setUpdateTime(quota.getUpdateTime());
                
                // 获取用户信息
                SysUser user = userService.getById(quota.getUserId());
                if (user != null) {
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                }
                
                return vo;
            }).collect(Collectors.toList());
            
            voPage.setRecords(voList);
            return Result.success(voPage);
            
        } catch (Exception e) {
            return Result.error("获取AI配额列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户AI配额详情
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserAiQuotaVO> getUserQuota(@PathVariable Long userId) {
        try {
            UserAiQuota quota = aiQuotaService.getUserQuota(userId);
            if (quota == null) {
                return Result.error("用户配额不存在");
            }
            
            UserAiQuotaVO vo = new UserAiQuotaVO();
            vo.setId(quota.getId());
            vo.setUserId(quota.getUserId());
            vo.setVipLevel(quota.getVipLevel());
            vo.setDailyQuota(quota.getDailyQuota());
            vo.setUsedDaily(quota.getUsedDaily());
            vo.setMonthlyQuota(quota.getMonthlyQuota());
            vo.setUsedMonthly(quota.getUsedMonthly());
            vo.setCreateTime(quota.getCreateTime());
            vo.setUpdateTime(quota.getUpdateTime());
            
            // 获取用户信息
            SysUser user = userService.getById(userId);
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取用户配额详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新用户AI配额
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateUserQuota(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        try {
            Integer vipLevel = (Integer) request.get("vipLevel");
            Integer dailyQuota = (Integer) request.get("dailyQuota");
            Integer monthlyQuota = (Integer) request.get("monthlyQuota");
            
            boolean success = aiQuotaService.updateUserQuota(userId, vipLevel, dailyQuota, monthlyQuota);
            if (success) {
                return Result.success("配额更新成功");
            } else {
                return Result.error("配额更新失败");
            }
        } catch (Exception e) {
            return Result.error("配额更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 重置用户每日配额
     */
    @PostMapping("/{userId}/reset-daily")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> resetDailyQuota(@PathVariable Long userId) {
        try {
            boolean success = aiQuotaService.resetDailyQuota(userId);
            if (success) {
                return Result.success("每日配额重置成功");
            } else {
                return Result.error("每日配额重置失败");
            }
        } catch (Exception e) {
            return Result.error("每日配额重置失败：" + e.getMessage());
        }
    }
    
    /**
     * 重置用户每月配额
     */
    @PostMapping("/{userId}/reset-monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> resetMonthlyQuota(@PathVariable Long userId) {
        try {
            boolean success = aiQuotaService.resetMonthlyQuota(userId);
            if (success) {
                return Result.success("每月配额重置成功");
            } else {
                return Result.error("每月配额重置失败");
            }
        } catch (Exception e) {
            return Result.error("每月配额重置失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量更新用户配额
     */
    @PutMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchUpdateQuotas(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) request.get("userIds");
            Integer vipLevel = (Integer) request.get("vipLevel");
            Integer dailyQuota = (Integer) request.get("dailyQuota");
            Integer monthlyQuota = (Integer) request.get("monthlyQuota");
            
            if (userIds == null || userIds.isEmpty()) {
                return Result.badRequest("请选择要更新的用户");
            }
            
            boolean success = aiQuotaService.batchUpdateQuotas(userIds, vipLevel, dailyQuota, monthlyQuota);
            if (success) {
                return Result.success("批量更新成功");
            } else {
                return Result.error("批量更新失败");
            }
        } catch (Exception e) {
            return Result.error("批量更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取AI配额统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getQuotaStats() {
        try {
            Map<String, Object> stats = aiQuotaService.getQuotaStats();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 初始化用户配额
     */
    @PostMapping("/{userId}/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> initializeUserQuota(@PathVariable Long userId) {
        try {
            aiQuotaService.initializeQuota(userId);
            return Result.success("用户配额初始化成功");
        } catch (Exception e) {
            return Result.error("用户配额初始化失败：" + e.getMessage());
        }
    }
}
