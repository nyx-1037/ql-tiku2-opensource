package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.dto.LoginRequest;
import com.qltiku2.dto.RegisterRequest;
import com.qltiku2.service.AuthService;
import com.qltiku2.vo.LoginResponse;
import com.qltiku2.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 认证控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("=== 用户登录请求开始 ===");
        System.out.println("请求用户名: " + request.getUsername());
        System.out.println("请求路径: /auth/login");
        try {
            LoginResponse response = authService.login(request);
            System.out.println("用户登录成功: " + request.getUsername());
            return Result.success(response);
        } catch (Exception e) {
            System.err.println("用户登录失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("登录失败：" + e.getMessage());
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // 密码格式验证：必须包含数字和英文字母，长度6-18位
            String password = request.getPassword();
            if (password != null) {
                if (password.length() < 6 || password.length() > 18) {
                    return Result.badRequest("密码长度必须在6-18个字符之间");
                }
                
                String passwordRegex = "^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{6,18}$";
                if (!password.matches(passwordRegex)) {
                    return Result.badRequest("密码必须包含数字和英文字母，长度6-18位");
                }
            }
            
            authService.register(request);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error("注册失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/user")
    public Result<UserInfoVO> getCurrentUser() {
        try {
            UserInfoVO userInfo = authService.getCurrentUserInfo();
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                LoginResponse response = authService.refreshToken(token);
                return Result.success(response);
            } catch (Exception e) {
                return Result.error("刷新令牌失败：" + e.getMessage());
            }
        }
        return Result.badRequest("无效的令牌格式");
    }
    
    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        try {
            authService.logout();
            return Result.success("退出登录成功");
        } catch (Exception e) {
            return Result.error("退出登录失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, String> request) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.badRequest("原密码不能为空");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.badRequest("新密码不能为空");
        }
        
        // 密码长度验证：6-18位
        if (newPassword.length() < 6 || newPassword.length() > 18) {
            return Result.badRequest("密码长度必须在6-18个字符之间");
        }
        
        // 密码格式验证：必须包含数字和英文字母
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-zA-Z])[0-9a-zA-Z]{6,18}$";
        if (!newPassword.matches(passwordRegex)) {
            return Result.badRequest("密码必须包含数字和英文字母，长度6-18位");
        }
        
        try {
            authService.changePassword(oldPassword, newPassword);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error("密码修改失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return Result.badRequest("用户名不能为空");
        }
        
        // 这里可以调用service方法检查用户名是否已存在
        // 暂时返回true表示可用
        return Result.success(true);
    }
    
    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        if (email == null || email.trim().isEmpty()) {
            return Result.badRequest("邮箱不能为空");
        }
        
        // 这里可以调用service方法检查邮箱是否已存在
        // 暂时返回true表示可用
        return Result.success(true);
    }
    
    /**
     * 验证邀请码（教师注册用）
     */
    @PostMapping("/validate-invite-code")
    public Result<Boolean> validateInviteCode(@RequestBody Map<String, String> request) {
        String inviteCode = request.get("inviteCode");
        
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return Result.badRequest("邀请码不能为空");
        }
        
        // 验证教师邀请码
        boolean isValid = "TEACHER2024".equals(inviteCode);
        
        if (isValid) {
            return Result.success(true);
        } else {
            return Result.error("邀请码不正确");
        }
    }
    
    /**
     * 更新用户头像
     */
    @PostMapping("/update-avatar")
    public Result<String> updateAvatar(@RequestBody Map<String, String> request) {
        String avatarUrl = request.get("avatarUrl");
        
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return Result.badRequest("头像URL不能为空");
        }
        
        try {
            authService.updateAvatar(avatarUrl);
            return Result.success("头像更新成功");
        } catch (Exception e) {
            return Result.error("头像更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 临时密码测试端点
     */
    @GetMapping("/test-password")
    public Result<Map<String, Object>> testPassword() {
        try {
            // 从数据库获取admin用户的实际密码哈希
            String dbPasswordHash = "$2a$10$0D9Yf8YhfPFgbaLTsf5h5./iYEd9Mg.jn.MchKISqgtUFwGQH4f36";
            
            // 测试密码
            String testPassword = "123456";
            
            // 验证密码是否匹配
            boolean matches = passwordEncoder.matches(testPassword, dbPasswordHash);
            
            // 生成新的密码哈希用于比较
            String newHash = passwordEncoder.encode(testPassword);
            boolean newMatches = passwordEncoder.matches(testPassword, newHash);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("testPassword", testPassword);
            result.put("dbPasswordHash", dbPasswordHash);
            result.put("matches", matches);
            result.put("newHash", newHash);
            result.put("newMatches", newMatches);
            
            System.out.println("=== 密码测试结果 ===");
            System.out.println("测试密码: " + testPassword);
            System.out.println("数据库哈希: " + dbPasswordHash);
            System.out.println("密码匹配结果: " + matches);
            System.out.println("新生成的哈希: " + newHash);
            System.out.println("新哈希匹配结果: " + newMatches);
            
            return Result.success(result);
        } catch (Exception e) {
            System.err.println("密码测试失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("密码测试失败：" + e.getMessage());
        }
    }
}