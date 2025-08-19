package com.qltiku2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 反馈保存请求DTO
 * 
 * @author qltiku2
 */
public class FeedbackSaveRequest {
    
    /**
     * 反馈ID（更新时需要）
     */
    private Long id;
    
    /**
     * 反馈类型：1-bug反馈，2-功能建议，3-其他反馈
     */
    @NotNull(message = "反馈类型不能为空")
    @Min(value = 1, message = "反馈类型值不正确")
    @Max(value = 3, message = "反馈类型值不正确")
    private Integer feedbackType;
    
    /**
     * 反馈标题
     */
    @NotBlank(message = "反馈标题不能为空")
    private String title;
    
    /**
     * 反馈内容
     */
    @NotBlank(message = "反馈内容不能为空")
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
     * 管理员回复
     */
    private String adminReply;
    
    // 构造方法
    public FeedbackSaveRequest() {}
    
    public FeedbackSaveRequest(Integer feedbackType, String title, String content) {
        this.feedbackType = feedbackType;
        this.title = title;
        this.content = content;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getFeedbackType() {
        return feedbackType;
    }
    
    public void setFeedbackType(Integer feedbackType) {
        this.feedbackType = feedbackType;
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
    }
    
    public String getAdminReply() {
        return adminReply;
    }
    
    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }
    
    @Override
    public String toString() {
        return "FeedbackSaveRequest{" +
                "id=" + id +
                ", feedbackType=" + feedbackType +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                '}';
    }
}