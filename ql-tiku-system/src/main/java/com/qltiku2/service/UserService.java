package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.common.Result;
import com.qltiku2.dto.UserQueryRequest;
import com.qltiku2.dto.UserSaveRequest;
import com.qltiku2.entity.SysUser;
import com.qltiku2.vo.UserVO;

/**
 * 用户服务接口
 * 
 * @author qltiku2
 */
public interface UserService extends IService<SysUser> {
    
    /**
     * 分页查询用户列表
     */
    Result<IPage<UserVO>> getUserPage(UserQueryRequest request);
    
    /**
     * 根据ID获取用户详情
     */
    Result<UserVO> getUserById(Long id);
    
    /**
     * 创建用户
     */
    Result<String> createUser(UserSaveRequest request);
    
    /**
     * 更新用户
     */
    Result<String> updateUser(Long id, UserSaveRequest request);
    
    /**
     * 删除用户
     */
    Result<String> deleteUser(Long id);
    
    /**
     * 重置用户密码
     */
    Result<String> resetPassword(Long id);
    
    /**
     * 更新用户状态
     */
    Result<String> updateUserStatus(Long id, Integer status);
}