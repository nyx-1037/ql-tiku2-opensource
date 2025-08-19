package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.entity.FileInfo;
import com.qltiku2.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件管理控制器（客户端接口）
 * 提供文件查询、下载统计等功能
 * 
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {
    
    @Autowired
    private FileService fileService;
    
    /**
     * 分页获取文件列表
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 文件列表
     */
    @GetMapping
    public Result<Map<String, Object>> getFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        
        try {
            List<FileInfo> files;
            long total;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                files = fileService.searchEnabledFilesWithPagination(keyword, page, size);
                total = fileService.getEnabledFileCountByKeyword(keyword);
            } else {
                files = fileService.getEnabledFilesWithPagination(page, size);
                total = fileService.getEnabledFileCount();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取文件列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取文件详情
     * @param id 文件ID
     * @return 文件详情
     */
    @GetMapping("/{id}")
    public Result<FileInfo> getFileById(@PathVariable Long id) {
        try {
            FileInfo fileInfo = fileService.getFileById(id);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            return Result.success(fileInfo);
        } catch (Exception e) {
            return Result.error("获取文件详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 搜索文件
     * @param keyword 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<FileInfo> files = fileService.searchEnabledFilesWithPagination(keyword, page, size);
            long total = fileService.getEnabledFileCountByKeyword(keyword);
            
            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (total + size - 1) / size);
            result.put("keyword", keyword);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("搜索文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 增加文件下载次数
     * @param id 文件ID
     * @return 操作结果
     */
    @PostMapping("/{id}/download")
    public Result<String> incrementDownloadCount(@PathVariable Long id) {
        try {
            FileInfo fileInfo = fileService.getFileById(id);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            
            boolean success = fileService.incrementDownloadCount(id);
            if (success) {
                return Result.success("下载统计更新成功");
            } else {
                return Result.error("下载统计更新失败");
            }
        } catch (Exception e) {
            return Result.error("下载统计更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     * @param id 文件ID
     * @param response HTTP响应
     */
    @GetMapping("/download/{id}")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            FileInfo fileInfo = fileService.getFileById(id);
            if (fileInfo == null) {
                log.warn("文件不存在: id={}", id);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            log.info("开始下载文件: id={}, name={}, path={}", id, fileInfo.getOriginalFileName(), fileInfo.getFtpPath());
            
            // 跳过文件存在性检查，直接尝试下载（避免FTP数据连接超时问题）
            log.info("跳过文件存在性检查，直接尝试下载文件");
            
            // 设置响应头
            response.setContentType(fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream");
            response.setContentLengthLong(fileInfo.getFileSize());
            
            // 设置文件名
            String encodedFileName = URLEncoder.encode(fileInfo.getOriginalFileName(), StandardCharsets.UTF_8.toString());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // 获取文件流
            inputStream = fileService.getFileStream(fileInfo.getFtpPath());
            outputStream = response.getOutputStream();
            
            // 使用更大的缓冲区提高传输效率
            byte[] buffer = new byte[32768]; // 32KB缓冲区
            int bytesRead;
            long totalBytesRead = 0;
            long startTime = System.currentTimeMillis();
            
            log.info("开始传输文件数据，文件大小: {} bytes", fileInfo.getFileSize());
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                // 每传输1MB记录一次进度
                if (totalBytesRead % (1024 * 1024) == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.debug("文件传输进度: {} / {} bytes, 耗时: {}ms", totalBytesRead, fileInfo.getFileSize(), elapsed);
                }
                
                // 检查传输超时（超过60秒）
                if (System.currentTimeMillis() - startTime > 60000) {
                    log.error("文件传输超时，已传输: {} / {} bytes", totalBytesRead, fileInfo.getFileSize());
                    response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                    return;
                }
            }
            
            outputStream.flush();
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("文件下载完成: id={}, 传输字节数: {}, 耗时: {}ms", id, totalBytesRead, elapsed);
            
            // 增加下载次数
            fileService.incrementDownloadCount(id);
            
        } catch (IOException e) {
            log.error("文件下载IO异常: id={}, error={}", id, e.getMessage(), e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error("文件下载异常: id={}, error={}", id, e.getMessage(), e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            // 确保资源被正确关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                    log.debug("输入流已关闭");
                } catch (IOException e) {
                    log.error("关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                    log.debug("输出流已关闭");
                } catch (IOException e) {
                    log.error("关闭输出流失败", e);
                }
            }
        }
    }
}