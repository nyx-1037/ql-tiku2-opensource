package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.entity.Announcement;
import com.qltiku2.service.AnnouncementService;
import com.qltiku2.vo.AnnouncementVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 管理端公告控制器
 * 
 * @author qltiku2
 */
@RestController
@RequestMapping("/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {
    private static final Logger log = LoggerFactory.getLogger(AdminAnnouncementController.class);
    
    @Resource
    private AnnouncementService announcementService;
    
    /**
     * 获取公告列表（分页）
     * 
     * @param current 当前页
     * @param size 页大小
     * @param title 标题（可选）
     * @param type 类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    @GetMapping
    public Result<IPage<AnnouncementVO>> getAnnouncementList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        try {
            Page<Announcement> page = new Page<>(current, size);
            QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
            
            // 添加查询条件
            if (StringUtils.hasText(title)) {
                queryWrapper.like("title", title);
            }
            if (type != null) {
                queryWrapper.eq("type", type);
            }
            if (status != null) {
                queryWrapper.eq("status", status);
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc("create_time");
            
            IPage<Announcement> announcementPage = announcementService.page(page, queryWrapper);
            
            // 转换为VO
            IPage<AnnouncementVO> voPage = announcementPage.convert(this::convertToVO);
            
            return Result.success(voPage);
        } catch (Exception e) {
            return Result.error("获取公告列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取公告详情
     * 
     * @param id 公告ID
     * @return 公告详情
     */
    @GetMapping("/{id}")
    public Result<AnnouncementVO> getAnnouncementDetail(@PathVariable Long id) {
        try {
            Announcement announcement = announcementService.getById(id);
            if (announcement == null) {
                return Result.error("公告不存在");
            }
            return Result.success(convertToVO(announcement));
        } catch (Exception e) {
            return Result.error("获取公告详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建公告
     * 
     * @param announcement 公告信息
     * @return 创建结果
     */
    @PostMapping
    public Result<String> createAnnouncement(@RequestBody Announcement announcement) {
        try {
            // 设置创建时间
            announcement.setCreateTime(LocalDateTime.now());
            announcement.setUpdateTime(LocalDateTime.now());
            
            boolean success = announcementService.save(announcement);
            if (success) {
                return Result.success("创建公告成功");
            } else {
                return Result.error("创建公告失败");
            }
        } catch (Exception e) {
            return Result.error("创建公告失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新公告
     * 
     * @param id 公告ID
     * @param announcement 公告信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<String> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        try {
            Announcement existingAnnouncement = announcementService.getById(id);
            if (existingAnnouncement == null) {
                return Result.error("公告不存在");
            }
            
            // 设置ID和更新时间
            announcement.setId(id);
            announcement.setUpdateTime(LocalDateTime.now());
            // 保留原创建时间
            announcement.setCreateTime(existingAnnouncement.getCreateTime());
            
            boolean success = announcementService.updateById(announcement);
            if (success) {
                return Result.success("更新公告成功");
            } else {
                return Result.error("更新公告失败");
            }
        } catch (Exception e) {
            return Result.error("更新公告失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除公告
     * 
     * @param id 公告ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteAnnouncement(@PathVariable Long id) {
        try {
            boolean success = announcementService.removeById(id);
            if (success) {
                return Result.success("删除公告成功");
            } else {
                return Result.error("删除公告失败");
            }
        } catch (Exception e) {
            return Result.error("删除公告失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除公告
     * 
     * @param request 包含ID列表的请求
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result<String> batchDeleteAnnouncement(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的公告");
            }
            
            boolean success = announcementService.removeByIds(ids);
            if (success) {
                return Result.success("批量删除公告成功");
            } else {
                return Result.error("批量删除公告失败");
            }
        } catch (Exception e) {
            return Result.error("批量删除公告失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新公告状态
     * 
     * @param id 公告ID
     * @param request 包含状态的请求
     * @return 更新结果
     */
    @PutMapping("/{id}/status")
    public Result<String> updateAnnouncementStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        log.info("Updating status for announcement id: {} with request: {}", id, request);
        try {
            Integer status = request.get("status");
            if (status == null) {
                log.warn("Status parameter is null for id: {}", id);
                return Result.error("状态参数不能为空");
            }
            log.info("Extracted status: {} for id: {}", status, id);
            
            Announcement announcement = new Announcement();
            announcement.setId(id);
            announcement.setStatus(status);
            announcement.setUpdateTime(LocalDateTime.now());
            log.info("Preparing to update announcement: id={}, status={}, updateTime={}", id, status, announcement.getUpdateTime());
            
            boolean success = announcementService.updateById(announcement);
            log.info("Update success: {} for id: {}", success, id);
            if (success) {
                return Result.success("更新状态成功");
            } else {
                return Result.error("更新状态失败");
            }
        } catch (Exception e) {
            log.error("Error updating status for id {}: {}", id, e.getMessage(), e);
            return Result.error("更新状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 将Announcement实体转换为AnnouncementVO
     * 
     * @param announcement 公告实体
     * @return 公告VO
     */
    private AnnouncementVO convertToVO(Announcement announcement) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(announcement, vo);
        // 设置类型描述
        vo.setType(announcement.getType());
        // 确保状态字段正确设置
        vo.setStatus(announcement.getStatus());
        return vo;
    }
}