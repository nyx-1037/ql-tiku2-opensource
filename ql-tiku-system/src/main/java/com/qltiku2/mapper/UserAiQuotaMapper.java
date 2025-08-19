package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.UserAiQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 用户AI配额Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface UserAiQuotaMapper extends BaseMapper<UserAiQuota> {
    
    /**
     * 重置所有用户的每日配额
     * 仅重置已使用次数，不修改最后重置日期
     */
    @Update("UPDATE user_ai_quota SET used_daily = 0")
    void resetAllDailyQuota();
    
    /**
     * 重置所有用户的每月配额
     * 仅重置已使用次数
     */
    @Update("UPDATE user_ai_quota SET used_monthly = 0")
    void resetAllMonthlyQuota();
}