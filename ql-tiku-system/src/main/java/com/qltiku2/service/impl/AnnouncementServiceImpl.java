package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.Announcement;
import com.qltiku2.mapper.AnnouncementMapper;
import com.qltiku2.service.AnnouncementService;
import com.qltiku2.vo.AnnouncementVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统公告服务实现类
 * 
 * @author qltiku2
 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {
    
    @Resource
    private AnnouncementMapper announcementMapper;
    
    @Override
    public List<AnnouncementVO> getLatestAnnouncements(int limit) {
        List<Announcement> announcements = announcementMapper.selectActiveAnnouncements(limit);
        return announcements.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
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
        return vo;
    }
}