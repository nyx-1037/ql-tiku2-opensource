package com.qltiku2.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.qltiku2.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * OSS文件上传服务实现类
 * 
 * @author qltiku2
 */
@Service
public class OssServiceImpl implements OssService {
    
    private static final Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);
    
    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;
    
    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;
    
    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;
    
    @Value("${aliyun.oss.bucket-name:}")
    private String bucketName;
    
    @Value("${aliyun.oss.url-prefix:}")
    private String urlPrefix;
    
    @Override
    public String uploadAvatar(MultipartFile file) throws Exception {
        // 打印OSS配置信息
        logger.info("=== OSS配置检查开始 ===");
        logger.info("endpoint: [{}]", endpoint);
        logger.info("accessKeyId: [{}]", accessKeyId);
        logger.info("accessKeySecret: [{}]", accessKeySecret != null && !accessKeySecret.isEmpty() ? "已配置" : "未配置");
        logger.info("bucketName: [{}]", bucketName);
        logger.info("urlPrefix: [{}]", urlPrefix);
        
        // 检查配置
        if (endpoint == null || endpoint.isEmpty() || 
            accessKeyId == null || accessKeyId.isEmpty() || 
            accessKeySecret == null || accessKeySecret.isEmpty() || 
            bucketName == null || bucketName.isEmpty()) {
            logger.error("OSS配置不完整！");
            logger.error("endpoint为空: {}", endpoint == null || endpoint.isEmpty());
            logger.error("accessKeyId为空: {}", accessKeyId == null || accessKeyId.isEmpty());
            logger.error("accessKeySecret为空: {}", accessKeySecret == null || accessKeySecret.isEmpty());
            logger.error("bucketName为空: {}", bucketName == null || bucketName.isEmpty());
            throw new RuntimeException("阿里云OSS配置不完整，请检查配置文件");
        }
        
        logger.info("OSS配置检查通过，开始上传文件");
        
        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            // 获取文件输入流
            InputStream inputStream = file.getInputStream();
            
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 按日期分目录存储
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String datePath = sdf.format(new Date());
            
            // 生成时间戳
            long timestamp = System.currentTimeMillis();
            
            // 生成UUID（取前8位）
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            
            // 生成唯一文件名：avatar/年/月/日/时间戳_UUID.扩展名
            String fileName = "avatar/" + datePath + "/" + timestamp + "_" + uuid + extension;
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);
            
            // 上传文件
            ossClient.putObject(putObjectRequest);
            
            // 返回文件访问URL
            String url = urlPrefix + "/" + fileName;
            return url;
            
        } finally {
            // 关闭OSS客户端
            ossClient.shutdown();
        }
    }
    
    @Override
    public String uploadLogo(MultipartFile file) throws Exception {
        // 打印OSS配置信息
        logger.info("=== OSS配置检查开始（Logo上传） ===");
        logger.info("endpoint: [{}]", endpoint);
        logger.info("accessKeyId: [{}]", accessKeyId);
        logger.info("accessKeySecret: [{}]", accessKeySecret != null && !accessKeySecret.isEmpty() ? "已配置" : "未配置");
        logger.info("bucketName: [{}]", bucketName);
        logger.info("urlPrefix: [{}]", urlPrefix);
        
        // 检查配置
        if (endpoint == null || endpoint.isEmpty() || 
            accessKeyId == null || accessKeyId.isEmpty() || 
            accessKeySecret == null || accessKeySecret.isEmpty() || 
            bucketName == null || bucketName.isEmpty()) {
            logger.error("OSS配置不完整！");
            throw new RuntimeException("阿里云OSS配置不完整，请检查配置文件");
        }
        
        logger.info("OSS配置检查通过，开始上传Logo文件");
        
        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            // 获取文件输入流
            InputStream inputStream = file.getInputStream();
            
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 生成时间戳
            long timestamp = System.currentTimeMillis();
            
            // 生成UUID（取前8位）
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            
            // 生成唯一文件名：logo/时间戳_UUID.扩展名
            String fileName = "logo/" + timestamp + "_" + uuid + extension;
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);
            
            // 上传文件
            ossClient.putObject(putObjectRequest);
            
            // 返回文件访问URL
            String url = urlPrefix + "/" + fileName;
            return url;
            
        } finally {
            // 关闭OSS客户端
            ossClient.shutdown();
        }
    }
    
    @Override
    public String uploadFile(MultipartFile file, String fileName) throws Exception {
        // 打印OSS配置信息
        logger.info("=== OSS配置检查开始（文件上传） ===");
        logger.info("endpoint: [{}]", endpoint);
        logger.info("accessKeyId: [{}]", accessKeyId);
        logger.info("accessKeySecret: [{}]", accessKeySecret != null && !accessKeySecret.isEmpty() ? "已配置" : "未配置");
        logger.info("bucketName: [{}]", bucketName);
        logger.info("urlPrefix: [{}]", urlPrefix);
        
        // 检查配置
        if (endpoint == null || endpoint.isEmpty() || 
            accessKeyId == null || accessKeyId.isEmpty() || 
            accessKeySecret == null || accessKeySecret.isEmpty() || 
            bucketName == null || bucketName.isEmpty()) {
            logger.error("OSS配置不完整！");
            throw new RuntimeException("阿里云OSS配置不完整，请检查配置文件");
        }
        
        logger.info("OSS配置检查通过，开始上传文件: {}", fileName);
        
        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            // 获取文件输入流
            InputStream inputStream = file.getInputStream();
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);
            
            // 上传文件
            ossClient.putObject(putObjectRequest);
            
            // 返回文件访问URL
            String url = urlPrefix + "/" + fileName;
            logger.info("文件上传成功，URL: {}", url);
            return url;
            
        } finally {
            // 关闭OSS客户端
            ossClient.shutdown();
        }
    }
    
    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // 检查配置
            if (endpoint == null || endpoint.isEmpty() || 
                accessKeyId == null || accessKeyId.isEmpty() || 
                accessKeySecret == null || accessKeySecret.isEmpty() || 
                bucketName == null || bucketName.isEmpty()) {
                return false;
            }
            
            // 从URL中提取文件key
            String fileKey = fileUrl.replace(urlPrefix + "/", "");
            
            // 创建OSS客户端
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            
            try {
                // 删除文件
                ossClient.deleteObject(bucketName, fileKey);
                return true;
            } finally {
                // 关闭OSS客户端
                ossClient.shutdown();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public String uploadQuestionImage(MultipartFile file) throws Exception {
        // 打印OSS配置信息
        logger.info("=== OSS配置检查开始（题目图片上传） ===");
        logger.info("endpoint: [{}]", endpoint);
        logger.info("accessKeyId: [{}]", accessKeyId);
        logger.info("accessKeySecret: [{}]", accessKeySecret != null && !accessKeySecret.isEmpty() ? "已配置" : "未配置");
        logger.info("bucketName: [{}]", bucketName);
        logger.info("urlPrefix: [{}]", urlPrefix);
        
        // 检查配置
        if (endpoint == null || endpoint.isEmpty() || 
            accessKeyId == null || accessKeyId.isEmpty() || 
            accessKeySecret == null || accessKeySecret.isEmpty() || 
            bucketName == null || bucketName.isEmpty()) {
            logger.error("OSS配置不完整！");
            throw new RuntimeException("阿里云OSS配置不完整，请检查配置文件");
        }
        
        logger.info("OSS配置检查通过，开始上传题目图片文件");
        
        // 创建OSS客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            // 获取文件输入流
            InputStream inputStream = file.getInputStream();
            
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 按日期分目录存储
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String datePath = sdf.format(new Date());
            
            // 生成时间戳
            long timestamp = System.currentTimeMillis();
            
            // 生成UUID（取前8位）
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            
            // 生成唯一文件名：question/年/月/日/时间戳_UUID.扩展名
            String fileName = "question/" + datePath + "/" + timestamp + "_" + uuid + extension;
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);
            
            // 上传文件
            ossClient.putObject(putObjectRequest);
            
            // 返回文件访问URL
            String url = urlPrefix + "/" + fileName;
            logger.info("题目图片上传成功，URL: {}", url);
            return url;
            
        } finally {
            // 关闭OSS客户端
            ossClient.shutdown();
        }
    }
}