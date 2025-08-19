package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.entity.Membership;
import com.qltiku2.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端会员等级管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/memberships")
@CrossOrigin(origins = "*")
public class AdminMembershipController {
    
    @Autowired
    private MembershipService membershipService;
    
    /**
     * 获取会员等级列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<Membership>> getMemberships(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        
        try {
            Page<Membership> pageParam = new Page<>(current, size);
            QueryWrapper<Membership> queryWrapper = new QueryWrapper<>();
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                    .like("level_name", keyword)
                    .or()
                    .like("description", keyword)
                );
            }
            
            // 状态筛选
            if (isActive != null) {
                queryWrapper.eq("is_active", isActive);
            }
            
            // 按排序字段排序
            queryWrapper.orderByAsc("sort_order");
            
            IPage<Membership> page = membershipService.page(pageParam, queryWrapper);
            return Result.success(page);
            
        } catch (Exception e) {
            return Result.error("获取会员等级列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有启用的会员等级
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Membership>> getActiveMemberships() {
        try {
            List<Membership> memberships = membershipService.getActiveMemberships();
            return Result.success(memberships);
        } catch (Exception e) {
            return Result.error("获取启用会员等级失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取会员等级详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Membership> getMembership(@PathVariable Long id) {
        try {
            Membership membership = membershipService.getById(id);
            if (membership == null) {
                return Result.error("会员等级不存在");
            }
            return Result.success(membership);
        } catch (Exception e) {
            return Result.error("获取会员等级详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建会员等级
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createMembership(@RequestBody Membership membership) {
        try {
            // 检查等级代码是否已存在
            QueryWrapper<Membership> wrapper = new QueryWrapper<>();
            wrapper.eq("level_code", membership.getLevelCode());
            if (membershipService.count(wrapper) > 0) {
                return Result.error("等级代码已存在");
            }
            
            boolean success = membershipService.save(membership);
            if (success) {
                return Result.success("创建成功");
            } else {
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            return Result.error("创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新会员等级
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateMembership(@PathVariable Long id, @RequestBody Membership membership) {
        try {
            Membership existingMembership = membershipService.getById(id);
            if (existingMembership == null) {
                return Result.error("会员等级不存在");
            }
            
            // 检查等级代码是否被其他记录使用
            if (!existingMembership.getLevelCode().equals(membership.getLevelCode())) {
                QueryWrapper<Membership> wrapper = new QueryWrapper<>();
                wrapper.eq("level_code", membership.getLevelCode())
                       .ne("id", id);
                if (membershipService.count(wrapper) > 0) {
                    return Result.error("等级代码已被其他记录使用");
                }
            }
            
            membership.setId(id);
            boolean success = membershipService.updateById(membership);
            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除会员等级
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteMembership(@PathVariable Long id) {
        try {
            Membership membership = membershipService.getById(id);
            if (membership == null) {
                return Result.error("会员等级不存在");
            }
            
            // 检查是否有用户使用此等级
            // 这里可以添加检查逻辑，防止删除正在使用的等级
            
            boolean success = membershipService.removeById(id);
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
     * 批量删除会员等级
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDelete(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.badRequest("请选择要删除的记录");
            }
            
            boolean success = membershipService.removeByIds(ids);
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
     * 更新会员等级状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Boolean isActive = (Boolean) request.get("isActive");
            if (isActive == null) {
                return Result.badRequest("状态不能为空");
            }
            
            Membership membership = membershipService.getById(id);
            if (membership == null) {
                return Result.error("会员等级不存在");
            }
            
            membership.setIsActive(isActive);
            boolean success = membershipService.updateById(membership);
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
     * 更新用户会员等级
     */
    @PutMapping("/users/{userId}/membership")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateUserMembership(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        try {
            Integer membershipLevel = (Integer) request.get("membershipLevel");
            if (membershipLevel == null) {
                return Result.badRequest("会员等级不能为空");
            }
            
            boolean success = membershipService.updateUserMembership(userId, membershipLevel);
            if (success) {
                return Result.success("用户会员等级更新成功");
            } else {
                return Result.error("用户会员等级更新失败");
            }
        } catch (Exception e) {
            return Result.error("用户会员等级更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 同步用户AI配额
     */
    @PostMapping("/users/{userId}/sync-quota")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> syncUserAiQuota(@PathVariable Long userId) {
        try {
            boolean success = membershipService.syncUserAiQuota(userId);
            if (success) {
                return Result.success("用户AI配额同步成功");
            } else {
                return Result.error("用户AI配额同步失败");
            }
        } catch (Exception e) {
            return Result.error("用户AI配额同步失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取会员等级统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getMembershipStats() {
        try {
            Map<String, Object> stats = membershipService.getMembershipStats();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
}