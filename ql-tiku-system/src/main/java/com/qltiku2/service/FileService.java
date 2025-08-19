package com.qltiku2.service;

import com.qltiku2.entity.FileInfo;
import com.qltiku2.entity.CdnPrefix;
import com.qltiku2.mapper.FileInfoMapper;
import com.qltiku2.mapper.CdnPrefixMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文件管理服务类
 * 提供文件上传、下载、删除等核心功能
 * 
 * @author system
 */
@Slf4j
@Service
public class FileService {
    
    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private CdnPrefixMapper cdnPrefixMapper;
    
    @Autowired
    private FtpService ftpService;
    
    @Value("${file.upload.max-size:10485760}0") // 默认100MB
    private long maxFileSize;
    
    @Value("${file.upload.allowed-types:.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif,.zip,.rar}")
    private String allowedTypes;
    
    /**
     * 获取所有文件信息
     * @return 文件信息列表
     */
    public List<FileInfo> getAllFiles() {
        return fileInfoMapper.selectAll();
    }
    
    /**
     * 分页获取文件信息
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 文件信息列表
     */
    public List<FileInfo> getFilesWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        return fileInfoMapper.selectWithPagination(offset, size);
    }
    
    /**
     * 根据关键词搜索文件
     * @param keyword 搜索关键词
     * @return 文件信息列表
     */
    public List<FileInfo> searchFiles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllFiles();
        }
        return fileInfoMapper.searchByKeyword(keyword.trim());
    }
    
    /**
     * 分页搜索文件
     * @param keyword 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 文件信息列表
     */
    public List<FileInfo> searchFilesWithPagination(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFilesWithPagination(page, size);
        }
        return fileInfoMapper.searchByKeywordWithPagination(keyword.trim(), offset, size);
    }
    
    /**
     * 获取文件总数
     * @return 文件总数
     */
    public long getFileCount() {
        return fileInfoMapper.count();
    }
    
    /**
     * 根据关键词获取文件总数
     * @param keyword 搜索关键词
     * @return 文件总数
     */
    public long getFileCountByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFileCount();
        }
        return fileInfoMapper.countByKeyword(keyword.trim());
    }
    
    // 客户端专用方法 - 只返回启用的文件
    /**
     * 获取所有启用的文件信息
     * @return 启用的文件信息列表
     */
    public List<FileInfo> getAllEnabledFiles() {
        return fileInfoMapper.selectAllEnabled();
    }
    
    /**
     * 分页获取启用的文件信息
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 启用的文件信息列表
     */
    public List<FileInfo> getEnabledFilesWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        return fileInfoMapper.selectEnabledWithPagination(offset, size);
    }
    
    /**
     * 根据关键词搜索启用的文件
     * @param keyword 搜索关键词
     * @return 启用的文件信息列表
     */
    public List<FileInfo> searchEnabledFiles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEnabledFiles();
        }
        return fileInfoMapper.searchEnabledByKeyword(keyword.trim());
    }
    
    /**
     * 分页搜索启用的文件
     * @param keyword 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 启用的文件信息列表
     */
    public List<FileInfo> searchEnabledFilesWithPagination(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        if (keyword == null || keyword.trim().isEmpty()) {
            return getEnabledFilesWithPagination(page, size);
        }
        return fileInfoMapper.searchEnabledByKeywordWithPagination(keyword.trim(), offset, size);
    }
    
    /**
     * 获取启用的文件总数
     * @return 启用的文件总数
     */
    public long getEnabledFileCount() {
        return fileInfoMapper.countEnabled();
    }
    
    /**
     * 根据关键词获取启用的文件总数
     * @param keyword 搜索关键词
     * @return 启用的文件总数
     */
    public long getEnabledFileCountByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getEnabledFileCount();
        }
        return fileInfoMapper.countEnabledByKeyword(keyword.trim());
    }
    
    /**
     * 根据ID获取文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    public FileInfo getFileById(Long id) {
        return fileInfoMapper.selectById(id);
    }
    
    /**
     * 增加下载次数
     * @param id 文件ID
     * @return 更新是否成功
     */
    @Transactional
    public boolean incrementDownloadCount(Long id) {
        int result = fileInfoMapper.incrementDownloadCount(id);
        return result > 0;
    }
    
    /**
     * 获取文件流用于下载
     */
    public InputStream getFileStream(String ftpPath) throws IOException {
        return ftpService.downloadFile(ftpPath);
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String ftpPath) throws IOException {
        return ftpService.fileExists(ftpPath);
    }
    
    /**
     * 上传文件
     * @param file 上传的文件
     * @param description 文件描述
     * @return 文件信息
     */
    @Transactional
    public FileInfo uploadFile(MultipartFile file, String description, Long cdnPrefixId) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 获取指定的CDN前缀，如果未指定则使用默认CDN前缀
        CdnPrefix selectedPrefix;
        if (cdnPrefixId != null) {
            selectedPrefix = cdnPrefixMapper.selectById(cdnPrefixId);
            if (selectedPrefix == null) {
                throw new RuntimeException("指定的CDN前缀不存在");
            }
        } else {
            selectedPrefix = cdnPrefixMapper.selectDefault();
            if (selectedPrefix == null) {
                throw new RuntimeException("未配置默认CDN前缀");
            }
        }
        
        // 生成文件名
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String generatedFileName = generateFileName(fileExtension);
        
        // 计算文件MD5
        String checksum;
        try {
            checksum = calculateMD5(file.getBytes());
        } catch (IOException e) {
            throw new IOException("计算文件MD5失败", e);
        }
        
        // 上传文件到FTP服务器，获取FTP路径
        String ftpPath = ftpService.uploadFile(file, description);
        
        // 使用选定的CDN前缀生成文件访问URL
        String relativePath = ftpPath;
        if (ftpPath.startsWith("/tiku-resource-library/")) {
            relativePath = ftpPath.substring("/tiku-resource-library/".length());
        }
        String fullUrl = selectedPrefix.getPrefix().replaceAll("/$", "") + "/" + relativePath;
        
        // 创建文件信息对象
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalFileName(originalFileName);
        fileInfo.setGeneratedFileName(generatedFileName);
        fileInfo.setFileExtension(fileExtension);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setDescription(description);
        fileInfo.setCdnPrefix(selectedPrefix.getPrefix());
        fileInfo.setFullUrl(fullUrl);
        fileInfo.setFtpPath(ftpPath);
        fileInfo.setUploadTime(LocalDateTime.now());
        fileInfo.setDownloadCount(0);
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setChecksum(checksum);
        fileInfo.setStatus(1); // 默认启用状态
        
        // 保存到数据库
        int result = fileInfoMapper.insert(fileInfo);
        if (result > 0) {
            return fileInfo;
        } else {
            // 如果数据库保存失败，删除已上传的文件
            try {
                ftpService.deleteFile(ftpPath);
            } catch (IOException e) {
                log.error("删除FTP文件失败: {}", ftpPath, e);
            }
            throw new RuntimeException("保存文件信息失败");
        }
    }
    
    /**
     * 上传文件（向后兼容方法）
     * @param file 上传的文件
     * @param description 文件描述
     * @return 文件信息
     */
    @Transactional
    public FileInfo uploadFile(MultipartFile file, String description) throws IOException {
        return uploadFile(file, description, null);
    }
    
    /**
     * 更新文件状态
     * @param id 文件ID
     * @param status 状态（1-启用，0-禁用）
     * @return 更新是否成功
     */
    @Transactional
    public boolean updateFileStatus(Long id, Integer status) {
        return fileInfoMapper.updateStatus(id, status) > 0;
    }
    
    /**
     * 删除文件
     * @param id 文件ID
     * @return 删除是否成功
     */
    @Transactional
    public boolean deleteFile(Long id) {
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null) {
            return false;
        }
        
        // 从FTP服务器删除文件
        try {
            ftpService.deleteFile(fileInfo.getFtpPath());
        } catch (IOException e) {
            log.error("删除FTP文件失败: {}", e.getMessage());
            // 继续删除数据库记录，即使FTP文件删除失败
        }
        
        int result = fileInfoMapper.deleteById(id);
        return result > 0;
    }
    
    /**
     * 验证文件
     * @param file 上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        // 检查文件类型
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String fileExtension = getFileExtension(fileName).toLowerCase();
        if (!allowedTypes.toLowerCase().contains(fileExtension)) {
            throw new RuntimeException("不支持的文件类型：" + fileExtension);
        }
    }
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    /**
     * 生成文件名
     * @param fileExtension 文件扩展名
     * @return 生成的文件名
     */
    private String generateFileName(String fileExtension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuid + "_" + timestamp + fileExtension;
    }
    
    /**
     * 计算文件MD5
     * @param fileBytes 文件字节数组
     * @return MD5值
     */
    private String calculateMD5(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5计算失败", e);
        }
    }
}