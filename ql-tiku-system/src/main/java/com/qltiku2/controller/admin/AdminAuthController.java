package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.dto.LoginRequest;
import com.qltiku2.service.AuthService;
import com.qltiku2.vo.LoginResponse;
import com.qltiku2.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 管理端认证控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/auth")
@CrossOrigin(origins = "*")
public class AdminAuthController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("=== 管理员登录请求开始 ===");
        System.out.println("请求用户名: " + request.getUsername());
        System.out.println("请求路径: /admin/auth/login");
        try {
            LoginResponse response = authService.login(request);
            System.out.println("管理员认证成功，检查权限...");
            // 验证用户是否有管理员权限
            if (response.getUserInfo().getUserType() != 2) { // 2表示管理员
                System.err.println("用户权限不足，用户类型: " + response.getUserInfo().getUserType());
                return Result.error("无管理员权限");
            }
            System.out.println("管理员登录成功: " + request.getUsername());
            return Result.success(response);
        } catch (Exception e) {
            System.err.println("管理员登录失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("登录失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取管理员信息
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserInfoVO> getAdminInfo() {
        try {
            UserInfoVO userInfo = authService.getCurrentUserInfo();
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error("获取管理员信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PutMapping("/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> changePassword(@RequestBody Map<String, String> request) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.badRequest("原密码不能为空");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.badRequest("新密码不能为空");
        }
        
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.badRequest("新密码长度必须在6-20个字符之间");
        }
        
        try {
            authService.changePassword(oldPassword, newPassword);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error("密码修改失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新管理员信息
     */
    @PutMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateAdminInfo(@RequestBody Map<String, String> request) {
        String nickname = request.get("nickname");
        String email = request.get("email");
        String phone = request.get("phone");
        
        try {
            authService.updateAdminInfo(nickname, email, phone);
            return Result.success("信息更新成功");
        } catch (Exception e) {
            return Result.error("信息更新失败：" + e.getMessage());
        }
    }
}