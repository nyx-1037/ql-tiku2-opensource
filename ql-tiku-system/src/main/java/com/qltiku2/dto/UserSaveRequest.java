package com.qltiku2.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户保存请求
 * 
 * @author qltiku2
 */
@Data
public class UserSaveRequest {
    
    /**
     * 用户ID（更新时使用）
     */
    private Long id;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;
    
    /**
     * 密码（创建时必填，更新时可选）
     */
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
    
    /**
     * 用户类型（0-学生，1-教师，2-管理员）
     */
    @NotNull(message = "用户类型不能为空")
    private Integer userType;
    
    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status = 1;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 备注
     */
    private String remark;
}