package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.Membership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会员等级Mapper接口
 * 
 * @author qltiku2
 * @since 2025-01-08
 */
@Mapper
public interface MembershipMapper extends BaseMapper<Membership> {

    /**
     * 根据等级代码查询会员信息
     * 
     * @param levelCode 等级代码
     * @return 会员信息
     */
    @Select("SELECT * FROM membership WHERE level_code = #{levelCode} AND is_active = 1")
    Membership selectByLevelCode(Integer levelCode);

    /**
     * 查询所有启用的会员等级
     * 
     * @return 会员等级列表
     */
    @Select("SELECT * FROM membership WHERE is_active = 1 ORDER BY sort_order ASC")
    List<Membership> selectActiveMemberships();
}