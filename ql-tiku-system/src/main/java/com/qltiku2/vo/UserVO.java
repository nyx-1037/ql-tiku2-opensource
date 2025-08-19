package com.qltiku2.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象
 * 
 * @author qltiku2
 */
@Data
public class UserVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户类型（0-学生，1-教师，2-管理员）
     */
    private Integer userType;
    
    /**
     * 用户类型名称
     */
    private String userTypeName;
    
    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;
    
    /**
     * 前端状态值（active-启用，disabled-禁用）
     */
    private String statusValue;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
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
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    
    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;
    
    /**
     * 练习题数
     */
    private Integer practiceCount;
    
    /**
     * 考试次数
     */
    private Integer examCount;
    
    /**
     * 注册时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerTime;
    
    /**
     * 正确率
     */
    private Double correctRate;
    
    /**
     * 学习天数
     */
    private Integer studyDays;
    
    /**
     * 最近活动
     */
    private java.util.List<RecentActivity> recentActivities;
    
    /**
     * 最近活动内部类
     */
    @Data
    public static class RecentActivity {
        private Long id;
        private String description;
        private String time;
    }
}
