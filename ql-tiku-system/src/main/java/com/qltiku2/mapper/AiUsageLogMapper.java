package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.AiUsageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * AI使用日志Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface AiUsageLogMapper extends BaseMapper<AiUsageLog> {
    
    /**
     * 统计用户今日AI使用次数
     */
    @Select("SELECT COUNT(*) FROM ai_usage_log WHERE user_id = #{userId} AND DATE(usage_time) = CURDATE()")
    int countTodayUsage(Long userId);
    
    /**
     * 统计用户本月AI使用次数
     */
    @Select("SELECT COUNT(*) FROM ai_usage_log WHERE user_id = #{userId} AND YEAR(usage_time) = YEAR(NOW()) AND MONTH(usage_time) = MONTH(NOW())")
    int countMonthlyUsage(Long userId);
}