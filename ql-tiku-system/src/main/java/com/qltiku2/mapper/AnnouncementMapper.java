package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统公告Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
    
    /**
     * 获取有效的公告列表（按优先级和创建时间排序）
     * 
     * @param limit 限制数量
     * @return 公告列表
     */
    @Select("SELECT * FROM announcements " +
            "WHERE status = 1 " +
            "AND (start_time IS NULL OR start_time <= NOW()) " +
            "AND (end_time IS NULL OR end_time >= NOW()) " +
            "ORDER BY priority DESC, create_time DESC " +
            "LIMIT #{limit}")
    List<Announcement> selectActiveAnnouncements(int limit);
}