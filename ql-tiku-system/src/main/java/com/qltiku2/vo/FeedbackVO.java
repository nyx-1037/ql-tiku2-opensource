package com.qltiku2.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 反馈视图对象VO
 * 
 * @author qltiku2
 */
public class FeedbackVO {
    
    private Long id;
    
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 反馈类型：1-bug反馈，2-功能建议，3-其他反馈
     */
    private Integer feedbackType;
    
    /**
     * 反馈类型名称
     */
    private String feedbackTypeName;
    
    /**
     * 反馈标题
     */
    private String title;
    
    /**
     * 反馈内容
     */
    private String content;
    
    /**
     * 图片URL列表
     */
    private List<String> imageList;
    
    /**
     * 状态：0-待处理，1-已受理，2-已处理，3-已修复，4-已采纳，5-已失效，6-已撤销
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 管理员回复
     */
    private String adminReply;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 构造方法
    public FeedbackVO() {}
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Integer getFeedbackType() {
        return feedbackType;
    }
    
    public void setFeedbackType(Integer feedbackType) {
        this.feedbackType = feedbackType;
        // 设置反馈类型名称
        switch (feedbackType) {
            case 1:
                this.feedbackTypeName = "Bug反馈";
                break;
            case 2:
                this.feedbackTypeName = "功能建议";
                break;
            case 3:
                this.feedbackTypeName = "其他反馈";
                break;
            default:
                this.feedbackTypeName = "未知";
                break;
        }
    }
    
    public String getFeedbackTypeName() {
        return feedbackTypeName;
    }
    
    public void setFeedbackTypeName(String feedbackTypeName) {
        this.feedbackTypeName = feedbackTypeName;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public List<String> getImageList() {
        return imageList;
    }
    
    public void setImageList(List<String> imageList) {
        this.imageList = imageList;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
        // 设置状态名称
        switch (status) {
            case 0:
                this.statusName = "待处理";
                break;
            case 1:
                this.statusName = "已受理";
                break;
            case 2:
                this.statusName = "已处理";
                break;
            case 3:
                this.statusName = "已修复";
                break;
            case 4:
                this.statusName = "已采纳";
                break;
            case 5:
                this.statusName = "已失效";
                break;
            case 6:
                this.statusName = "已撤销";
                break;
            default:
                this.statusName = "未知";
                break;
        }
    }
    
    public String getStatusName() {
        return statusName;
    }
    
    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
    
    public String getAdminReply() {
        return adminReply;
    }
    
    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    @Override
    public String toString() {
        return "FeedbackVO{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", feedbackType=" + feedbackType +
                ", feedbackTypeName='" + feedbackTypeName + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", statusName='" + statusName + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}