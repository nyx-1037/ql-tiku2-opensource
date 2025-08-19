package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0")
    SysUser selectByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    SysUser selectByPhone(@Param("phone") String phone);
    
    /**
     * 统计用户数量按类型
     */
    @Select("SELECT user_type, COUNT(*) as count FROM sys_user WHERE deleted = 0 GROUP BY user_type")
    List<Map<String, Object>> countUsersByType();
    
    /**
     * 获取活跃用户列表
     */
    @Select("SELECT * FROM sys_user WHERE status = 1 AND deleted = 0 ORDER BY last_login_time DESC LIMIT #{limit}")
    List<SysUser> selectActiveUsers(@Param("limit") Integer limit);
}