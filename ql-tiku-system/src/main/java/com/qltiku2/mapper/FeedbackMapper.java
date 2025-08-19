package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 反馈Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
    
    /**
     * 分页查询反馈（带用户名）
     */
    @Select("SELECT f.*, u.username " +
            "FROM feedback f " +
            "LEFT JOIN sys_user u ON f.user_id = u.id " +
            "${ew.customSqlSegment}")
    IPage<Feedback> selectFeedbackPage(Page<Feedback> page, @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<Feedback> queryWrapper);
    
    /**
     * 根据用户ID查询反馈列表
     */
    @Select("SELECT * FROM feedback WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Feedback> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 统计各状态的反馈数量
     */
    @Select("SELECT status, COUNT(*) as count FROM feedback WHERE deleted = 0 GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();
    
    /**
     * 统计各类型的反馈数量
     */
    @Select("SELECT feedback_type, COUNT(*) as count FROM feedback WHERE deleted = 0 GROUP BY feedback_type")
    List<java.util.Map<String, Object>> countByType();
    
    /**
     * 获取最近的反馈列表
     */
    @Select("SELECT f.*, u.username " +
            "FROM feedback f " +
            "LEFT JOIN sys_user u ON f.user_id = u.id " +
            "WHERE f.deleted = 0 " +
            "ORDER BY f.create_time DESC " +
            "LIMIT #{limit}")
    List<Feedback> selectRecentFeedbacks(@Param("limit") Integer limit);
}