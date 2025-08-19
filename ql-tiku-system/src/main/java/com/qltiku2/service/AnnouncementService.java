package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.Announcement;
import com.qltiku2.vo.AnnouncementVO;

import java.util.List;

/**
 * 系统公告服务接口
 * 
 * @author qltiku2
 */
public interface AnnouncementService extends IService<Announcement> {
    
    /**
     * 获取最新的有效公告列表
     * 
     * @param limit 限制数量
     * @return 公告VO列表
     */
    List<AnnouncementVO> getLatestAnnouncements(int limit);
}