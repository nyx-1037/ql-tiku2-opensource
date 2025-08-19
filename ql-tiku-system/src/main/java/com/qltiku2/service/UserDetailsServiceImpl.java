package com.qltiku2.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户详情服务实现类
 * 
 * @author qltiku2
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== UserDetailsService.loadUserByUsername 开始 ===");
        System.out.println("查询用户名: " + username);
        
        // 查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username)
                   .eq(SysUser::getStatus, 1); // 只查询正常状态的用户
        
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        
        if (sysUser == null) {
            System.err.println("用户不存在: " + username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        System.out.println("找到用户: " + sysUser.getUsername());
        System.out.println("用户密码哈希: " + sysUser.getPassword());
        System.out.println("用户状态: " + sysUser.getStatus());
        System.out.println("用户类型: " + sysUser.getUserType());
        
        // 构建权限列表
        Collection<GrantedAuthority> authorities = getAuthorities(sysUser.getUserType());
        
        // 返回UserDetails对象
        UserDetails userDetails = new User(
            sysUser.getUsername(),
            sysUser.getPassword(),
            sysUser.getStatus() == 1, // enabled
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            authorities
        );
        
        System.out.println("UserDetails创建成功，用户名: " + userDetails.getUsername());
        System.out.println("UserDetails密码: " + userDetails.getPassword());
        System.out.println("UserDetails启用状态: " + userDetails.isEnabled());
        System.out.println("=== UserDetailsService.loadUserByUsername 结束 ===");
        
        return userDetails;
    }
    
    /**
     * 根据用户类型获取权限列表
     */
    private Collection<GrantedAuthority> getAuthorities(Integer userType) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        switch (userType) {
            case 0: // 学生
                authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // 添加基础用户权限
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
                break;
            case 1: // 教师
                authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // 添加基础用户权限
                authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT")); // 教师也有学生权限
                break;
            case 2: // 管理员
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // 添加基础用户权限
                authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
                break;
            default:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // 添加基础用户权限
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
                break;
        }
        
        return authorities;
    }
}