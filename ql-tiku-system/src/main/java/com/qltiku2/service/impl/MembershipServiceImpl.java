package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.Membership;
import com.qltiku2.entity.RegistrationCode;
import com.qltiku2.entity.SysUser;
import com.qltiku2.entity.UserAiQuota;
import com.qltiku2.mapper.MembershipMapper;
import com.qltiku2.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员等级服务实现类
 * 
 * @author qltiku2
 * @since 2025-01-08
 */
@Service
public class MembershipServiceImpl extends ServiceImpl<MembershipMapper, Membership> implements MembershipService {

    @Autowired
    private UserService userService;

    @Autowired
    private AiQuotaService aiQuotaService;

    @Autowired
    private RegistrationCodeService registrationCodeService;

    @Override
    public Membership getByLevelCode(Integer levelCode) {
        return baseMapper.selectByLevelCode(levelCode);
    }

    @Override
    public List<Membership> getActiveMemberships() {
        return baseMapper.selectActiveMemberships();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserMembership(Long userId, Integer membershipLevel) {
        try {
            // 1. 验证会员等级是否存在
            Membership membership = getByLevelCode(membershipLevel);
            if (membership == null || !membership.getIsActive()) {
                return false;
            }

            // 2. 更新用户会员等级
            SysUser user = userService.getById(userId);
            if (user == null) {
                return false;
            }
            user.setMembershipLevel(membershipLevel);
            boolean userUpdated = userService.updateById(user);

            // 3. 同步AI配额
            boolean quotaUpdated = syncUserAiQuota(userId);

            return userUpdated && quotaUpdated;
        } catch (Exception e) {
            throw new RuntimeException("更新用户会员等级失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setUserMembershipByCode(Long userId, String registrationCode) {
        try {
            // 1. 验证注册码
            QueryWrapper<RegistrationCode> wrapper = new QueryWrapper<>();
            wrapper.eq("code", registrationCode)
                   .eq("is_active", true);
            RegistrationCode code = registrationCodeService.getOne(wrapper);
            
            if (code == null) {
                return false;
            }

            // 2. 获取注册码对应的会员等级
            Integer membershipLevel = code.getMembershipLevel();
            if (membershipLevel == null) {
                membershipLevel = 0; // 默认普通用户
            }

            // 3. 更新用户会员等级
            return updateUserMembership(userId, membershipLevel);
        } catch (Exception e) {
            throw new RuntimeException("根据注册码设置用户会员等级失败", e);
        }
    }

    @Override
    public Map<String, Object> getMembershipStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计各等级用户数量
        List<Membership> memberships = getActiveMemberships();
        for (Membership membership : memberships) {
            QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
            wrapper.eq("membership_level", membership.getLevelCode())
                   .eq("deleted", 0);
            long count = userService.count(wrapper);
            stats.put(membership.getLevelName(), count);
        }
        
        // 总用户数
        QueryWrapper<SysUser> totalWrapper = new QueryWrapper<>();
        totalWrapper.eq("deleted", 0);
        long totalUsers = userService.count(totalWrapper);
        stats.put("totalUsers", totalUsers);
        
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncUserAiQuota(Long userId) {
        try {
            // 1. 获取用户信息
            SysUser user = userService.getById(userId);
            if (user == null) {
                return false;
            }

            // 2. 获取用户会员等级信息
            Integer membershipLevel = user.getMembershipLevel();
            if (membershipLevel == null) {
                membershipLevel = 0; // 默认普通用户
            }
            
            Membership membership = getByLevelCode(membershipLevel);
            if (membership == null) {
                // 如果找不到对应等级，使用默认等级
                membership = getByLevelCode(0);
                if (membership == null) {
                    return false;
                }
            }

            // 3. 更新用户AI配额
            UserAiQuota quota = aiQuotaService.getUserQuota(userId);
            if (quota == null) {
                // 如果配额不存在，先初始化
                aiQuotaService.initializeQuota(userId);
                quota = aiQuotaService.getUserQuota(userId);
            }

            // 4. 根据会员等级更新配额
            quota.setVipLevel(membershipLevel);
            quota.setDailyQuota(membership.getDailyQuota());
            quota.setMonthlyQuota(membership.getMonthlyQuota());
            
            return aiQuotaService.updateById(quota);
        } catch (Exception e) {
            throw new RuntimeException("同步用户AI配额失败", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserAiQuota(Long userId, Integer membershipLevel) {
        try {
            // 1. 获取会员等级信息
            Membership membership = getByLevelCode(membershipLevel);
            if (membership == null) {
                // 如果找不到对应等级，使用默认等级
                membership = getByLevelCode(0);
                if (membership == null) {
                    throw new RuntimeException("找不到会员等级信息");
                }
            }

            // 2. 更新用户AI配额
            UserAiQuota quota = aiQuotaService.getUserQuota(userId);
            if (quota == null) {
                // 如果配额不存在，先初始化
                aiQuotaService.initializeQuota(userId);
                quota = aiQuotaService.getUserQuota(userId);
            }

            // 3. 根据会员等级更新配额
            quota.setVipLevel(membershipLevel);
            quota.setDailyQuota(membership.getDailyQuota());
            quota.setMonthlyQuota(membership.getMonthlyQuota());
            
            aiQuotaService.updateById(quota);
        } catch (Exception e) {
            throw new RuntimeException("同步用户AI配额失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Integer getMembershipLevelByRegistrationCode(String registrationCode) {
        try {
            // 通过注册码服务获取注册码信息
            RegistrationCode code = registrationCodeService.getCodeList()
                .stream()
                .filter(rc -> rc.getCode().equals(registrationCode))
                .findFirst()
                .orElse(null);
            
            if (code != null) {
                return code.getMembershipLevel();
            }
            
            return 0; // 默认普通用户
        } catch (Exception e) {
            return 0; // 出错时返回默认等级
        }
    }
}