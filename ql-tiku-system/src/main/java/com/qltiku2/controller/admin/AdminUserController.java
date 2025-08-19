package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.UserQueryRequest;
import com.qltiku2.dto.UserSaveRequest;
import com.qltiku2.service.UserService;
import com.qltiku2.utils.JwtUtils;
import com.qltiku2.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端用户管理控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 获取用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<UserVO>> getUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        UserQueryRequest request = new UserQueryRequest();
        request.setCurrent(current);
        request.setSize(size);
        request.setKeyword(keyword);
        request.setUserType(userType);
        request.setStatus(status);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        
        return userService.getUserPage(request);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createUser(@Valid @RequestBody UserSaveRequest request) {
        return userService.createUser(request);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateUser(@PathVariable Long id, 
                                     @Valid @RequestBody UserSaveRequest request) {
        return userService.updateUser(id, request);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
    
    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        if (status == null) {
            return Result.badRequest("状态不能为空");
        }
        return userService.updateUserStatus(id, status);
    }
    
    /**
     * 获取在线用户列表
     */
    @GetMapping("/online")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getOnlineUsers() {
        try {
            List<Map<String, Object>> onlineUsers = new ArrayList<>();
            
            // 获取所有用户登录状态的key
            Set<String> userKeys = redisTemplate.keys("user:login:*");
            if (userKeys == null || userKeys.isEmpty()) {
                return Result.success(onlineUsers);
            }
            
            for (String userKey : userKeys) {
                String username = userKey.replace("user:login:", "");
                String token = (String) redisTemplate.opsForValue().get(userKey);
                
                if (token != null) {
                    try {
                        // 验证token是否有效
                        if (jwtUtils.validateToken(token, username)) {
                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("username", username);
                            
                            // 获取token信息
                            String tokenKey = "jwt:token:" + token;
                            Map<String, Object> tokenInfo = (Map<String, Object>) redisTemplate.opsForValue().get(tokenKey);
                            
                            if (tokenInfo != null) {
                                userInfo.put("userId", tokenInfo.get("userId"));
                                userInfo.put("userType", tokenInfo.get("userType"));
                                userInfo.put("loginTime", new Date((Long) tokenInfo.get("createTime")));
                            }
                            
                            // 获取token过期时间
                            Date expiration = jwtUtils.getExpirationDateFromToken(token);
                            userInfo.put("expireTime", expiration);
                            userInfo.put("token", token);
                            
                            onlineUsers.add(userInfo);
                        }
                    } catch (Exception e) {
                        // token无效，跳过
                        continue;
                    }
                }
            }
            
            // 按登录时间倒序排列
            onlineUsers.sort((a, b) -> {
                Date timeA = (Date) a.get("loginTime");
                Date timeB = (Date) b.get("loginTime");
                if (timeA == null || timeB == null) return 0;
                return timeB.compareTo(timeA);
            });
            
            return Result.success(onlineUsers);
        } catch (Exception e) {
            return Result.error("获取在线用户列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 强制用户下线
     */
    @PostMapping("/force-logout/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> forceLogout(@PathVariable String username) {
        try {
            jwtUtils.forceLogout(username);
            return Result.success("用户已强制下线");
        } catch (Exception e) {
            return Result.error("强制下线失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量强制用户下线
     */
    @PostMapping("/batch-force-logout")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchForceLogout(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> usernames = request.get("usernames");
            if (usernames == null || usernames.isEmpty()) {
                return Result.badRequest("用户名列表不能为空");
            }
            
            int successCount = 0;
            for (String username : usernames) {
                try {
                    jwtUtils.forceLogout(username);
                    successCount++;
                } catch (Exception e) {
                    // 记录错误但继续处理其他用户
                    continue;
                }
            }
            
            return Result.success("成功强制下线 " + successCount + " 个用户");
        } catch (Exception e) {
            return Result.error("批量强制下线失败：" + e.getMessage());
        }
    }
}
