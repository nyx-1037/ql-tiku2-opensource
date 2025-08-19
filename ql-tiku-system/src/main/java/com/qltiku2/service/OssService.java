package com.qltiku2.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * OSS文件上传服务接口
 * 
 * @author qltiku2
 */
public interface OssService {
    
    /**
     * 上传头像文件
     * 
     * @param file 头像文件
     * @return 文件访问URL
     */
    String uploadAvatar(MultipartFile file) throws Exception;
    
    /**
     * 上传Logo文件
     * 
     * @param file Logo文件
     * @return 文件访问URL
     */
    String uploadLogo(MultipartFile file) throws Exception;
    
    /**
     * 上传文件
     * 
     * @param file 文件
     * @param fileName 文件名
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String fileName) throws Exception;
    
    /**
     * 删除文件
     * 
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);
    
    /**
     * 上传题目图片
     * 
     * @param file 图片文件
     * @return 文件访问URL
     */
    String uploadQuestionImage(MultipartFile file) throws Exception;
}