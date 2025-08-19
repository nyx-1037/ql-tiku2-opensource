package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.service.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/upload")
@CrossOrigin(origins = "*")
public class UploadController {
    
    @Autowired
    private OssService ossService;
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.badRequest("文件不能为空");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                return Result.badRequest("只支持JPG和PNG格式的图片");
            }
            
            // 验证文件大小（2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.badRequest("文件大小不能超过2MB");
            }
            
            // 上传到OSS
            String url = ossService.uploadAvatar(file);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 上传Logo
     */
    @PostMapping("/logo")
    public Result<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.badRequest("文件不能为空");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/svg+xml"))) {
                return Result.badRequest("只支持JPG、PNG和SVG格式的图片");
            }
            
            // 验证文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.badRequest("文件大小不能超过5MB");
            }
            
            // 上传到OSS
            String url = ossService.uploadLogo(file);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}