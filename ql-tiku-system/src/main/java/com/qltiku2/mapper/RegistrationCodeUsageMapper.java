package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.entity.RegistrationCodeUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 注册码使用记录Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface RegistrationCodeUsageMapper extends BaseMapper<RegistrationCodeUsage> {
    
    /**
     * 获取注册码使用记录（带用户和注册码信息）
     */
    @Select("SELECT rcu.*, rc.code as registration_code, u.username, u.email " +
            "FROM registration_code_usage rcu " +
            "LEFT JOIN registration_code rc ON rcu.code_id = rc.id " +
            "LEFT JOIN sys_user u ON rcu.user_id = u.id " +
            "WHERE rcu.code_id = #{codeId} " +
            "ORDER BY rcu.used_time DESC")
    IPage<Map<String, Object>> getUsageRecordsByCodeId(Page<Map<String, Object>> page, @Param("codeId") Long codeId);
    
    /**
     * 统计注册码使用次数
     */
    @Select("SELECT COUNT(*) FROM registration_code_usage WHERE code_id = #{codeId}")
    int countUsageByCodeId(@Param("codeId") Long codeId);
    
    /**
    /**
     * 检查用户是否使用过该注册码
     */
    @Select("SELECT COUNT(*) FROM registration_code_usage WHERE code_id = #{codeId} AND user_id = #{userId}")
    int checkUserUsedCode(@Param("codeId") Long codeId, @Param("userId") Long userId);
    
    /**
    /**
     * 更新注册码使用记录中的用户ID
     */
    @Update("UPDATE registration_code_usage rcu " +
            "JOIN registration_code rc ON rcu.code_id = rc.id " +
            "SET rcu.user_id = #{userId} " +
            "WHERE rc.code = #{registrationCode} AND rcu.user_id = 0")
    int updateUserIdByCode(@Param("registrationCode") String registrationCode, @Param("userId") Long userId);
}
