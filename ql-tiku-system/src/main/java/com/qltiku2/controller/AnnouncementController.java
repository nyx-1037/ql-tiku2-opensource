package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.service.AnnouncementService;
import com.qltiku2.vo.AnnouncementVO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 系统公告控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    
    @Resource
    private AnnouncementService announcementService;
    
    /**
     * 获取最新公告列表
     * 
     * @param limit 限制数量，默认10条
     * @return 公告列表
     */
    @GetMapping("/latest")
    public Result<List<AnnouncementVO>> getLatestAnnouncements(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnnouncementVO> announcements = announcementService.getLatestAnnouncements(limit);
            return Result.success(announcements);
        } catch (Exception e) {
            return Result.error("获取公告列表失败：" + e.getMessage());
        }
    }
}