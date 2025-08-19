package com.qltiku2.controller.admin;

import com.qltiku2.common.Result;
import com.qltiku2.entity.FileInfo;
import com.qltiku2.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
 * 管理端文件管理控制器
 * 提供文件的增删改查等管理功能
 * 
 * @author system
 */
@RestController
@RequestMapping("/admin/files")
public class AdminFileController {
    
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
                files = fileService.searchFilesWithPagination(keyword, page, size);
                total = fileService.getFileCountByKeyword(keyword);
            } else {
                files = fileService.getFilesWithPagination(page, size);
                total = fileService.getFileCount();
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
     * 上传文件
     * @param file 上传的文件
     * @param description 文件描述
     * @param cdnPrefixId CDN前缀ID（可选）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long cdnPrefixId) {
        
        try {
            FileInfo fileInfo = fileService.uploadFile(file, description, cdnPrefixId);
            return Result.success(fileInfo);
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteFile(@PathVariable Long id) {
        try {
            boolean success = fileService.deleteFile(id);
            if (success) {
                return Result.success("文件删除成功");
            } else {
                return Result.error("文件删除失败，文件不存在");
            }
        } catch (Exception e) {
            return Result.error("文件删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除文件
     * @param ids 文件ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result<String> batchDeleteFiles(@RequestBody List<Long> ids) {
        try {
            int successCount = 0;
            int failCount = 0;
            
            for (Long id : ids) {
                try {
                    boolean success = fileService.deleteFile(id);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                }
            }
            
            String message = String.format("批量删除完成，成功：%d，失败：%d", successCount, failCount);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error("批量删除失败：" + e.getMessage());
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
            List<FileInfo> files = fileService.searchFilesWithPagination(keyword, page, size);
            long total = fileService.getFileCountByKeyword(keyword);
            
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
     * 更新文件状态
     * @param id 文件ID
     * @param status 状态（1-启用，0-禁用）
     * @return 更新结果
     */
    @PutMapping("/{id}/status")
    public Result<String> updateFileStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        
        try {
            if (status != 0 && status != 1) {
                return Result.error("状态值无效，只能是0或1");
            }
            
            boolean success = fileService.updateFileStatus(id, status);
            if (success) {
                return Result.success("文件状态更新成功");
            } else {
                return Result.error("文件状态更新失败，文件可能不存在");
            }
        } catch (Exception e) {
            return Result.error("更新文件状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文件统计信息
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            long totalFiles = fileService.getFileCount();
            long enabledFiles = fileService.getEnabledFileCount();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalFiles", totalFiles);
            statistics.put("enabledFiles", enabledFiles);
            statistics.put("disabledFiles", totalFiles - enabledFiles);
            
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     * @param id 文件ID
     * @param response HTTP响应
     */
    @GetMapping("/download/{id}")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) {
        try {
            FileInfo fileInfo = fileService.getFileById(id);
            if (fileInfo == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 检查文件是否存在
            if (!fileService.fileExists(fileInfo.getFtpPath())) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 设置响应头
            response.setContentType(fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream");
            response.setContentLengthLong(fileInfo.getFileSize());
            
            // 设置文件名
            String encodedFileName = URLEncoder.encode(fileInfo.getOriginalFileName(), StandardCharsets.UTF_8.toString());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            
            // 获取文件流并写入响应
            try (InputStream inputStream = fileService.getFileStream(fileInfo.getFtpPath());
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            
            // 增加下载次数
            fileService.incrementDownloadCount(id);
            
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}