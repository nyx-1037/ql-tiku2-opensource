package com.qltiku2.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 * 
 * @author qltiku2
 */
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 记住我
     */
    private Boolean rememberMe = false;
    
    /**
     * 验证码ID
     */
    private String captchaId;
    
    /**
     * 验证码
     */
    private String captchaCode;
    
    // 构造方法
    public LoginRequest() {}
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getter和Setter方法
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getRememberMe() {
        return rememberMe;
    }
    
    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
    
    public String getCaptchaId() {
        return captchaId;
    }
    
    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }
    
    public String getCaptchaCode() {
        return captchaCode;
    }
    
    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
    
    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", rememberMe=" + rememberMe +
                ", captchaId='" + captchaId + '\'' +
                '}';
    }
}