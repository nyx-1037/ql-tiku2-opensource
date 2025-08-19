package com.qltiku2.service;

import com.qltiku2.config.FtpConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FTP服务类
 * 专用于资料库文件管理
 */
@Slf4j
@Service
public class FtpService {
    
    @Autowired
    private FtpConfig ftpConfig;
    
    /**
     * 创建FTP连接
     */
    private FTPClient createFtpClient() throws IOException {
        FTPClient ftpClient = new FTPClient();
        
        try {
            // 连接FTP服务器
            ftpClient.connect(ftpConfig.getServer().getHost(), ftpConfig.getServer().getPort());
            
            // 登录
            boolean loginSuccess = ftpClient.login(
                ftpConfig.getServer().getUsername(), 
                ftpConfig.getServer().getPassword()
            );
            
            if (!loginSuccess) {
                throw new IOException("FTP登录失败");
            }
            
            // 设置文件传输模式为二进制
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            // 设置被动模式
            if (ftpConfig.getServer().isPassiveMode()) {
                ftpClient.enterLocalPassiveMode();
                log.debug("FTP被动模式已启用");
            } else {
                ftpClient.enterLocalActiveMode();
                log.debug("FTP主动模式已启用");
            }
            
            // 设置更短的超时时间以快速失败
            ftpClient.setConnectTimeout(10000); // 10秒连接超时
            ftpClient.setDataTimeout(15000);    // 15秒数据超时
            ftpClient.setControlKeepAliveTimeout(10000); // 10秒控制连接超时
            
            // 设置缓冲区大小
            ftpClient.setBufferSize(32768); // 32KB缓冲区
            
            // 设置编码
            ftpClient.setControlEncoding("UTF-8");
            
            log.info("FTP连接成功: {}:{}", ftpConfig.getServer().getHost(), ftpConfig.getServer().getPort());
            return ftpClient;
            
        } catch (IOException e) {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ex) {
                    log.error("关闭FTP连接失败", ex);
                }
            }
            throw e;
        }
    }
    
    /**
     * 关闭FTP连接
     */
    private void closeFtpClient(FTPClient ftpClient) {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
                log.debug("FTP连接已关闭");
            } catch (IOException e) {
                log.error("关闭FTP连接失败", e);
            }
        }
    }
    
    /**
     * 上传文件到FTP服务器
     */
    public String uploadFile(MultipartFile file, String description) throws IOException {
        // 验证文件
        validateFile(file);
        
        FTPClient ftpClient = null;
        try {
            ftpClient = createFtpClient();
            
            // 生成唯一文件名
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String generatedFileName = generateFileName(fileExtension);
            
            // 创建目录结构
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String remotePath = ftpConfig.getStorage().getRootPath() + datePath + "/";
            createDirectoryTree(ftpClient, remotePath);
            
            // 上传文件
            String remoteFilePath = remotePath + generatedFileName;
            try (InputStream inputStream = file.getInputStream()) {
                boolean uploadSuccess = ftpClient.storeFile(remoteFilePath, inputStream);
                if (!uploadSuccess) {
                    throw new IOException("文件上传失败: " + ftpClient.getReplyString());
                }
            }
            
            log.info("文件上传成功: {} -> {}", originalFileName, remoteFilePath);
            return remoteFilePath;
            
        } finally {
            closeFtpClient(ftpClient);
        }
    }
    
    /**
     * 从FTP服务器下载文件
     */
    public InputStream downloadFile(String remotePath) throws IOException {
        FTPClient ftpClient = createFtpClient();
        
        try {
            // 设置更短的数据传输超时时间
            ftpClient.setDataTimeout(30000); // 30秒
            
            log.info("开始从FTP服务器获取文件流: {}", remotePath);
            InputStream inputStream = ftpClient.retrieveFileStream(remotePath);
            if (inputStream == null) {
                closeFtpClient(ftpClient);
                log.error("无法获取文件流，文件可能不存在: {}", remotePath);
                throw new IOException("文件不存在或下载失败: " + remotePath);
            }
            
            log.info("FTP文件流获取成功: {}", remotePath);
            
            // 返回一个包装的InputStream，在关闭时同时关闭FTP连接
            return new FtpInputStream(inputStream, ftpClient, remotePath);
            
        } catch (IOException e) {
            log.error("FTP文件下载失败: {}, 错误: {}", remotePath, e.getMessage());
            closeFtpClient(ftpClient);
            throw e;
        }
    }
    
    /**
     * 删除FTP服务器上的文件
     */
    public boolean deleteFile(String remotePath) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = createFtpClient();
            
            boolean deleteSuccess = ftpClient.deleteFile(remotePath);
            if (deleteSuccess) {
                log.info("文件删除成功: {}", remotePath);
            } else {
                log.warn("文件删除失败: {}, 原因: {}", remotePath, ftpClient.getReplyString());
            }
            
            return deleteSuccess;
            
        } finally {
            closeFtpClient(ftpClient);
        }
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String remotePath) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = createFtpClient();
            
            FTPFile[] files = ftpClient.listFiles(remotePath);
            return files.length > 0;
            
        } finally {
            closeFtpClient(ftpClient);
        }
    }
    
    /**
     * 获取文件大小
     */
    public long getFileSize(String remotePath) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = createFtpClient();
            
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files.length > 0) {
                return files[0].getSize();
            }
            
            return -1;
            
        } finally {
            closeFtpClient(ftpClient);
        }
    }
    
    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > ftpConfig.getUpload().getMaxFileSize()) {
            throw new IOException("文件大小超过限制: " + ftpConfig.getUpload().getMaxFileSize() + " 字节");
        }
        
        // 检查文件类型
        String fileExtension = getFileExtension(file.getOriginalFilename());
        if (fileExtension == null || !ftpConfig.getUpload().getAllowedTypes().contains(fileExtension.toLowerCase())) {
            throw new IOException("不支持的文件类型: " + fileExtension);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + uuid + "." + extension;
    }
    
    /**
     * 创建目录树
     */
    private void createDirectoryTree(FTPClient ftpClient, String remotePath) throws IOException {
        String[] pathElements = remotePath.split("/");
        StringBuilder currentPath = new StringBuilder();
        
        for (String pathElement : pathElements) {
            if (pathElement.isEmpty()) {
                currentPath.append("/");
                continue;
            }
            
            currentPath.append(pathElement).append("/");
            
            // 尝试切换到目录，如果失败则创建目录
            if (!ftpClient.changeWorkingDirectory(currentPath.toString())) {
                if (!ftpClient.makeDirectory(currentPath.toString())) {
                    throw new IOException("创建目录失败: " + currentPath.toString());
                }
                log.debug("创建目录: {}", currentPath.toString());
            }
        }
        
        // 重置工作目录
        ftpClient.changeWorkingDirectory("/");
    }
    
    /**
     * 生成文件访问URL
     */
    public String generateFileUrl(String remotePath) {
        if (remotePath.startsWith(ftpConfig.getStorage().getRootPath())) {
            String relativePath = remotePath.substring(ftpConfig.getStorage().getRootPath().length());
            return ftpConfig.getStorage().getUrlPrefix() + relativePath;
        }
        return ftpConfig.getStorage().getUrlPrefix() + remotePath;
    }
    
    /**
     * FTP输入流包装类，用于在关闭流时同时关闭FTP连接
     */
    private static class FtpInputStream extends InputStream {
        private final InputStream inputStream;
        private final FTPClient ftpClient;
        private final String remotePath;
        private volatile boolean closed = false;
        
        public FtpInputStream(InputStream inputStream, FTPClient ftpClient, String remotePath) {
            this.inputStream = inputStream;
            this.ftpClient = ftpClient;
            this.remotePath = remotePath;
        }
        
        @Override
        public int read() throws IOException {
            if (closed) {
                throw new IOException("Stream已关闭");
            }
            try {
                return inputStream.read();
            } catch (IOException e) {
                log.error("FTP流读取失败: {}, 错误: {}", remotePath, e.getMessage());
                throw e;
            }
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            if (closed) {
                throw new IOException("Stream已关闭");
            }
            try {
                return inputStream.read(b);
            } catch (IOException e) {
                log.error("FTP流读取失败: {}, 错误: {}", remotePath, e.getMessage());
                throw e;
            }
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Stream已关闭");
            }
            try {
                return inputStream.read(b, off, len);
            } catch (IOException e) {
                log.error("FTP流读取失败: {}, 错误: {}", remotePath, e.getMessage());
                throw e;
            }
        }
        
        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            
            closed = true;
            log.debug("开始关闭FTP流: {}", remotePath);
            
            try {
                if (inputStream != null) {
                    inputStream.close();
                    log.debug("输入流已关闭: {}", remotePath);
                }
            } catch (IOException e) {
                log.error("关闭输入流失败: {}", remotePath, e);
            }
            
            try {
                if (ftpClient != null && ftpClient.isConnected()) {
                    ftpClient.completePendingCommand();
                    log.debug("FTP命令完成: {}", remotePath);
                }
            } catch (IOException e) {
                log.error("完成FTP命令失败: {}", remotePath, e);
            }
            
            try {
                if (ftpClient != null && ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                    log.debug("FTP连接已关闭: {}", remotePath);
                }
            } catch (IOException e) {
                log.error("关闭FTP连接失败: {}", remotePath, e);
            }
        }
    }
}