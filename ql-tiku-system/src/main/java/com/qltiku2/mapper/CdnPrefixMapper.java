package com.qltiku2.mapper;

import com.qltiku2.entity.CdnPrefix;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * CDN前缀配置数据访问接口
 * 提供CDN前缀的CRUD操作
 * 
 * @author system
 */
@Mapper
public interface CdnPrefixMapper {
    
    @Insert("INSERT INTO cdn_prefix (name, prefix, description, is_default, is_active, create_time, update_time) " +
            "VALUES (#{name}, #{prefix}, #{description}, #{isDefault}, #{isActive}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdnPrefix cdnPrefix);
    
    @Select("SELECT * FROM cdn_prefix WHERE id = #{id}")
    CdnPrefix selectById(Long id);
    
    @Select("SELECT * FROM cdn_prefix ORDER BY is_default DESC, create_time ASC")
    List<CdnPrefix> selectAll();
    
    @Select("SELECT * FROM cdn_prefix WHERE is_active = 1 ORDER BY is_default DESC, create_time ASC")
    List<CdnPrefix> selectAllActive();
    
    @Select("SELECT * FROM cdn_prefix WHERE is_default = 1 AND is_active = 1 LIMIT 1")
    CdnPrefix selectDefault();
    
    @Update("UPDATE cdn_prefix SET name = #{name}, prefix = #{prefix}, description = #{description}, " +
            "is_default = #{isDefault}, is_active = #{isActive}, update_time = #{updateTime} WHERE id = #{id}")
    int update(CdnPrefix cdnPrefix);
    
    @Update("UPDATE cdn_prefix SET is_default = 0")
    int clearAllDefault();
    
    @Update("UPDATE cdn_prefix SET is_default = 1 WHERE id = #{id}")
    int setDefault(Long id);
    
    @Delete("DELETE FROM cdn_prefix WHERE id = #{id}")
    int deleteById(Long id);
    
    @Select("SELECT COUNT(*) FROM cdn_prefix WHERE name = #{name} AND id != #{excludeId}")
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
    
    @Select("SELECT COUNT(*) FROM cdn_prefix WHERE prefix = #{prefix} AND id != #{excludeId}")
    int countByPrefix(@Param("prefix") String prefix, @Param("excludeId") Long excludeId);
}