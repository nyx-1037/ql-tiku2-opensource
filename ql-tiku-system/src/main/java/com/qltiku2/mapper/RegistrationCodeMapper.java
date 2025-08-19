package com.qltiku2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qltiku2.entity.RegistrationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 注册码Mapper接口
 * 
 * @author qltiku2
 */
@Mapper
public interface RegistrationCodeMapper extends BaseMapper<RegistrationCode> {
    
    /**
     * 根据注册码查询有效的注册码
     */
    @Select("SELECT * FROM registration_code WHERE code = #{code} AND is_active = 1 AND valid_from <= NOW() AND valid_until >= NOW() AND used_count < max_uses")
    RegistrationCode findValidCode(String code);
    
    /**
     * 增加注册码使用次数
     */
    @Update("UPDATE registration_code SET used_count = used_count + 1 WHERE id = #{id}")
    void incrementUsedCount(Long id);
}