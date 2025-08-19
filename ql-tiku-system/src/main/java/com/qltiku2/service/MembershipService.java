package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.Membership;

import java.util.List;
import java.util.Map;

/**
 * 会员等级服务接口
 * 
 * @author qltiku2
 * @since 2025-01-08
 */
public interface MembershipService extends IService<Membership> {

    /**
     * 根据等级代码查询会员信息
     * 
     * @param levelCode 等级代码
     * @return 会员信息
     */
    Membership getByLevelCode(Integer levelCode);

    /**
     * 查询所有启用的会员等级
     * 
     * @return 会员等级列表
     */
    List<Membership> getActiveMemberships();

    /**
     * 更新用户会员等级
     * 
     * @param userId 用户ID
     * @param membershipLevel 会员等级代码
     * @return 是否成功
     */
    boolean updateUserMembership(Long userId, Integer membershipLevel);

    /**
     * 根据注册码设置用户会员等级
     * 
     * @param userId 用户ID
     * @param registrationCode 注册码
     * @return 是否成功
     */
    boolean setUserMembershipByCode(Long userId, String registrationCode);

    /**
     * 获取会员等级统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getMembershipStats();

    /**
     * 同步用户AI配额（根据会员等级）
     * 
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean syncUserAiQuota(Long userId);
    
    /**
     * 同步用户AI配额（指定会员等级）
     * 
     * @param userId 用户ID
     * @param membershipLevel 会员等级代码
     */
    void syncUserAiQuota(Long userId, Integer membershipLevel);
    
    /**
     * 根据注册码获取会员等级
     * 
     * @param registrationCode 注册码
     * @return 会员等级代码
     */
    Integer getMembershipLevelByRegistrationCode(String registrationCode);
}