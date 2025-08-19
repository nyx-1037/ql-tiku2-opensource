package com.qltiku2.entity;

import java.time.LocalDateTime;

/**
 * 文件信息实体类
 * 对应数据库表：file_info
 * 
 * @author system
 */
public class FileInfo {
    
    private Long id;
    private String originalFileName;
    private String generatedFileName;
    private String fileExtension;
    private Long fileSize;
    private String description;
    private String cdnPrefix;
    private String fullUrl;
    private String ftpPath;
    private LocalDateTime uploadTime;
    private Integer downloadCount;
    private String mimeType;
    private String checksum;
    private Integer status; // 文件状态：1-启用，0-禁用
    
    // 构造函数
    public FileInfo() {}
    
    public FileInfo(String originalFileName, String generatedFileName, 
                   String fileExtension, Long fileSize, String description) {
        this.originalFileName = originalFileName;
        this.generatedFileName = generatedFileName;
        this.fileExtension = fileExtension;
        this.fileSize = fileSize;
        this.description = description;
        this.uploadTime = LocalDateTime.now();
        this.downloadCount = 0;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    
    public String getGeneratedFileName() { return generatedFileName; }
    public void setGeneratedFileName(String generatedFileName) { this.generatedFileName = generatedFileName; }
    
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCdnPrefix() { return cdnPrefix; }
    public void setCdnPrefix(String cdnPrefix) { this.cdnPrefix = cdnPrefix; }
    
    public String getFullUrl() { return fullUrl; }
    public void setFullUrl(String fullUrl) { this.fullUrl = fullUrl; }
    
    public String getFtpPath() { return ftpPath; }
    public void setFtpPath(String ftpPath) { this.ftpPath = ftpPath; }
    
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    @Override
    public String toString() {
        return "FileInfo{" +
                "id=" + id +
                ", originalFileName='" + originalFileName + '\'' +
                ", generatedFileName='" + generatedFileName + '\'' +
                ", fileExtension='" + fileExtension + '\'' +
                ", fileSize=" + fileSize +
                ", description='" + description + '\'' +
                ", cdnPrefix='" + cdnPrefix + '\'' +
                ", fullUrl='" + fullUrl + '\'' +
                ", ftpPath='" + ftpPath + '\'' +
                ", uploadTime=" + uploadTime +
                ", downloadCount=" + downloadCount +
                ", mimeType='" + mimeType + '\'' +
                ", checksum='" + checksum + '\'' +
                ", status=" + status +
                '}';
    }
}