package com.qltiku2.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 公告视图对象
 * 
 * @author qltiku2
 */
public class AnnouncementVO {
    
    /**
     * 公告ID
     */
    private Long id;
    
    /**
     * 公告标题
     */
    private String title;
    
    /**
     * 公告内容
     */
    private String content;
    
    /**
     * 公告类型：1-普通公告，2-重要公告，3-系统维护
     */
    private Integer type;
    
    /**
     * 类型描述
     */
    private String typeText;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    public AnnouncementVO() {}
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
        // 设置类型描述
        switch (type) {
            case 1:
                this.typeText = "普通公告";
                break;
            case 2:
                this.typeText = "重要公告";
                break;
            case 3:
                this.typeText = "系统维护";
                break;
            default:
                this.typeText = "未知类型";
                break;
        }
    }
    
    public String getTypeText() {
        return typeText;
    }
    
    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "AnnouncementVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", typeText='" + typeText + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", createTime=" + createTime +
                '}';
    }
}