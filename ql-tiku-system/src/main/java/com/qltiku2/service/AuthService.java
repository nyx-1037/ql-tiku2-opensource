package com.qltiku2.service;

import com.qltiku2.dto.LoginRequest;
import com.qltiku2.dto.RegisterRequest;
import com.qltiku2.vo.LoginResponse;
import com.qltiku2.vo.UserInfoVO;

/**
 * 认证服务接口
 * 
 * @author qltiku2
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * 用户注册
     */
    void register(RegisterRequest registerRequest);
    
    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo();
    
    /**
     * 刷新令牌
     */
    LoginResponse refreshToken(String token);
    
    /**
     * 用户登出
     */
    void logout();
    
    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);
    
    /**
     * 更新用户头像
     */
    void updateAvatar(String avatarUrl);
    
    /**
     * 更新管理员信息
     */
    void updateAdminInfo(String nickname, String email, String phone);
}