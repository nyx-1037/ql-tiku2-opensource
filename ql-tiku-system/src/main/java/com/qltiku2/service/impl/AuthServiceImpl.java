package com.qltiku2.service.impl;

import com.qltiku2.dto.LoginRequest;
import com.qltiku2.dto.RegisterRequest;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.service.AuthService;
import com.qltiku2.service.SysConfigService;
import com.qltiku2.service.RegistrationCodeService;
import com.qltiku2.service.MembershipService;
import com.qltiku2.utils.JwtUtils;
import com.qltiku2.vo.LoginResponse;
import com.qltiku2.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 认证服务实现类
 * 
 * @author qltiku2
 */
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private SysConfigService sysConfigService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RegistrationCodeService registrationCodeService;
    

    @Autowired
    private MembershipService membershipService;
    
    @Autowired
    private com.qltiku2.mapper.RegistrationCodeUsageMapper registrationCodeUsageMapper;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            System.out.println("[AuthService] 开始登录验证，用户名: " + request.getUsername());
            
            // 1. 验证验证码（如果启用）
            boolean captchaEnabled = Boolean.parseBoolean(
                sysConfigService.getConfigValue("sys.captcha.enable", "false")
            );
            
            if (captchaEnabled) {
                System.out.println("[AuthService] 验证码功能已启用，开始验证验证码");
                if (request.getCaptchaId() == null || request.getCaptchaCode() == null) {
                    throw new RuntimeException("请输入验证码");
                }
                
                String redisKey = "captcha:" + request.getCaptchaId();
                String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
                
                if (storedCode == null) {
                    throw new RuntimeException("验证码已过期，请重新获取");
                }
                
                if (!storedCode.equalsIgnoreCase(request.getCaptchaCode().trim())) {
                    throw new RuntimeException("验证码错误");
                }
                
                // 验证成功后删除验证码
                redisTemplate.delete(redisKey);
                System.out.println("[AuthService] 验证码验证成功");
            } else {
                System.out.println("[AuthService] 验证码功能未启用，跳过验证码验证");
            }
            
            // 2. 验证用户名和密码
            System.out.println("[AuthService] 调用AuthenticationManager进行身份验证");
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            System.out.println("[AuthService] 身份验证成功: " + authentication.isAuthenticated());
            
            // 3. 查询用户信息
            SysUser user = sysUserMapper.selectByUsername(request.getUsername());
            if (user == null) {
                System.out.println("[AuthService] 用户不存在: " + request.getUsername());
                throw new RuntimeException("用户不存在");
            }
            System.out.println("[AuthService] 找到用户: " + user.getUsername() + ", 状态: " + user.getStatus());
            
            if (user.getStatus() == 0) {
                System.out.println("[AuthService] 账户已被禁用: " + user.getUsername());
                throw new RuntimeException("账户已被禁用");
            }
            
            // 4. 生成JWT令牌
            String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getUserType());
            System.out.println("[AuthService] JWT令牌生成成功");
            
            // 5. 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
            // 6. 构建用户信息VO
            UserInfoVO userInfoVO = buildUserInfoVO(user);
            
            // 7. 构建登录响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(token);
            response.setExpiresIn(jwtUtils.getExpiration()); // 7天过期时间（秒）
            response.setUserInfo(userInfoVO);
            
            System.out.println("[AuthService] 登录成功，返回响应");
            return response;
            
        } catch (Exception e) {
            System.out.println("[AuthService] 登录失败，异常信息: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            
            // 如果是验证码相关错误，直接抛出原始错误信息
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("验证码") || errorMessage.contains("captcha"))) {
                throw new RuntimeException(errorMessage);
            }
            
            // 其他错误统一返回用户名或密码错误
            throw new RuntimeException("用户名或密码错误");
        }
    }
    
    @Override
    public void register(RegisterRequest request) {
        // 1. 验证验证码（如果启用）
        boolean captchaEnabled = Boolean.parseBoolean(
            sysConfigService.getConfigValue("sys.captcha.enable", "false")
        );
        
        if (captchaEnabled) {
            if (request.getCaptchaId() == null || request.getCaptchaCode() == null) {
                throw new RuntimeException("请输入验证码");
            }
            
            String redisKey = "captcha:" + request.getCaptchaId();
            String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (storedCode == null) {
                throw new RuntimeException("验证码已过期，请重新获取");
            }
            
            if (!storedCode.equalsIgnoreCase(request.getCaptchaCode().trim())) {
                throw new RuntimeException("验证码错误");
            }
            
            // 验证成功后删除验证码
            redisTemplate.delete(redisKey);
        }
        
        // 2. 验证密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        // 3. 检查用户名是否已存在
        if (sysUserMapper.selectByUsername(request.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 4. 验证注册码（如果启用注册码校验）
        // 4. 验证注册码（必须提供注册码才能注册）
        boolean registrationCodeRequired = Boolean.parseBoolean(
            sysConfigService.getConfigValue("sys.registration.code.required", "true")
        );
        
        if (registrationCodeRequired) {
            if (request.getRegistrationCode() == null || request.getRegistrationCode().isEmpty()) {
                throw new RuntimeException("请输入注册码");
            }
        }
        
        // 如果提供了注册码，必须验证其有效性
        if (request.getRegistrationCode() != null && !request.getRegistrationCode().isEmpty()) {
            boolean isValidCode = registrationCodeService.validateCode(request.getRegistrationCode());
            if (!isValidCode) {
                throw new RuntimeException("注册码无效或已过期");
            }
        } else if (registrationCodeRequired) {
            throw new RuntimeException("注册码不能为空");
        }
        
        // 5. 检查邮箱是否已存在
        if (sysUserMapper.selectByEmail(request.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 6. 检查手机号是否已存在（如果提供了手机号）
        if (StringUtils.hasText(request.getPhone()) && 
            sysUserMapper.selectByPhone(request.getPhone()) != null) {
            throw new RuntimeException("手机号已被注册");
        }
        
        // 7. 教师注册需要验证邀请码
        if (request.getUserType() == 1) {
            if (!"TEACHER2024".equals(request.getInviteCode())) {
                throw new RuntimeException("教师邀请码不正确");
            }
        }
        
        // 8. 获取注册码对应的会员等级
        Integer membershipLevel = 0; // 默认普通用户
        if (request.getRegistrationCode() != null && !request.getRegistrationCode().isEmpty()) {
            try {
                membershipLevel = membershipService.getMembershipLevelByRegistrationCode(request.getRegistrationCode());
                System.out.println("获取到注册码会员等级: " + membershipLevel);
            } catch (Exception e) {
                System.err.println("获取注册码会员等级失败: " + e.getMessage());
                // 如果获取会员等级失败，使用默认等级
                membershipLevel = 0;
            }
        }
        
        // 9. 先消耗注册码（确保注册码可用且记录使用）
        if (request.getRegistrationCode() != null && !request.getRegistrationCode().isEmpty()) {
            // 获取用户IP地址
            String ipAddress = "unknown";
            try {
                if (RequestContextHolder.currentRequestAttributes() instanceof ServletRequestAttributes) {
                    ipAddress = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                            .getRequest().getRemoteAddr();
                }
            } catch (Exception e) {
                System.err.println("获取IP地址失败: " + e.getMessage());
            }
            
            // 预先消耗注册码（使用临时用户ID 0，注册成功后更新）
            boolean codeUsed = registrationCodeService.useCode(request.getRegistrationCode(), 0L, ipAddress);
            if (!codeUsed) {
                throw new RuntimeException("注册码使用失败，可能已被使用或无效");
            }
            System.out.println("注册码预消耗成功: " + request.getRegistrationCode());
        }
        
        // 10. 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUserType(request.getUserType());
        user.setMembershipLevel(membershipLevel);
        user.setStatus(1); // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 11. 保存用户
        try {
            sysUserMapper.insert(user);
            System.out.println("用户创建成功，用户ID: " + user.getId());
        } catch (Exception e) {
            System.err.println("用户创建失败: " + e.getMessage());
            throw new RuntimeException("注册失败，请稍后重试");
        }

        // 12. 更新注册码使用记录中的用户ID
        if (request.getRegistrationCode() != null && !request.getRegistrationCode().isEmpty()) {
            try {
                registrationCodeUsageMapper.updateUserIdByCode(request.getRegistrationCode(), user.getId());
                System.out.println("注册码使用记录更新成功，用户ID: " + user.getId());
            } catch (Exception e) {
                System.err.println("更新注册码使用记录失败: " + e.getMessage());
            }
        }

        // 13. 根据会员等级同步用户AI配额
        try {
            membershipService.syncUserAiQuota(user.getId(), membershipLevel);
            System.out.println("用户AI配额同步成功，用户ID: " + user.getId() + ", 会员等级: " + membershipLevel);
        } catch (Exception e) {
            System.err.println("同步用户AI配额失败，用户ID: " + user.getId() + ", 会员等级: " + membershipLevel + ", 错误: " + e.getMessage());
        }
    }
    
    @Override
    public UserInfoVO getCurrentUserInfo() {
        try {
            // 从Security上下文获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            UserInfoVO userInfoVO = buildUserInfoVO(user);
            return userInfoVO;
            
        } catch (Exception e) {
            throw new RuntimeException("获取用户信息失败");
        }
    }
    
    @Override
    public LoginResponse refreshToken(String token) {
        try {
            // 1. 验证令牌
            String username = jwtUtils.getUsernameFromToken(token);
            if (!jwtUtils.validateToken(token, username)) {
                throw new RuntimeException("令牌无效");
            }
            
            // 2. 获取用户信息
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null || user.getStatus() == 0) {
                throw new RuntimeException("用户不存在或已被禁用");
            }
            
            // 3. 使用JwtUtils的refreshToken方法生成新令牌
            String newToken = jwtUtils.refreshToken(token);
            
            // 4. 构建响应
            UserInfoVO userInfoVO = buildUserInfoVO(user);
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newToken);
            response.setExpiresIn(jwtUtils.getExpiration());
            response.setUserInfo(userInfoVO);
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("刷新令牌失败: " + e.getMessage());
        }
    }
    
    @Override
    public void logout() {
        try {
            // 1. 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                
                // 2. 使当前用户的token失效
                jwtUtils.forceLogout(username);
            }
            
            // 3. 清除Security上下文
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            throw new RuntimeException("退出登录失败: " + e.getMessage());
        }
    }
    
    @Override
    public void changePassword(String oldPassword, String newPassword) {
        try {
            // 1. 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("原密码不正确");
            }
            
            // 3. 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
        } catch (Exception e) {
            throw new RuntimeException("密码修改失败");
        }
    }
    
    @Override
    public void updateAvatar(String avatarUrl) {
        try {
            // 1. 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 更新头像URL
            user.setAvatar(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
        } catch (Exception e) {
            throw new RuntimeException("头像更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建用户信息VO
     */
    private UserInfoVO buildUserInfoVO(SysUser user) {
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        
        // 设置角色和权限
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        
        switch (user.getUserType()) {
            case 0: // 学生
                roles.add("STUDENT");
                permissions.addAll(Arrays.asList(
                    "question:view", "question:answer", "exam:take", 
                    "record:view", "wrong:view", "profile:edit"
                ));
                break;
            case 1: // 教师
                roles.add("TEACHER");
                permissions.addAll(Arrays.asList(
                    "question:view", "question:create", "question:edit", "question:delete",
                    "exam:view", "exam:create", "exam:edit", "exam:delete",
                    "student:view", "record:view", "profile:edit"
                ));
                break;
            case 2: // 管理员
                roles.add("ADMIN");
                permissions.addAll(Arrays.asList(
                    "*:*" // 所有权限
                ));
                break;
        }
        
        userInfoVO.setRoles(roles);
        userInfoVO.setPermissions(permissions);
        
        return userInfoVO;
    }
    
    @Override
    public void updateAdminInfo(String nickname, String email, String phone) {
        try {
            // 1. 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new RuntimeException("用户未登录");
            }
            
            String username = authentication.getName();
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 更新用户信息
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setNickname(nickname.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim());
            }
            if (phone != null && !phone.trim().isEmpty()) {
                user.setPhone(phone.trim());
            }
            
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
        } catch (Exception e) {
            throw new RuntimeException("信息更新失败: " + e.getMessage());
        }
    }
}