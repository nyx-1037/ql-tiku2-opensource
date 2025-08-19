package com.qltiku2.mapper;

import com.qltiku2.entity.FileInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 文件信息数据访问接口
 * 提供文件信息的CRUD操作
 * 
 * @author system
 */
@Mapper
public interface FileInfoMapper {
    
    @Insert("INSERT INTO file_info (original_file_name, generated_file_name, file_extension, " +
            "file_size, description, cdn_prefix, full_url, ftp_path, upload_time, download_count, " +
            "mime_type, checksum, status) VALUES (#{originalFileName}, #{generatedFileName}, #{fileExtension}, " +
            "#{fileSize}, #{description}, #{cdnPrefix}, #{fullUrl}, #{ftpPath}, #{uploadTime}, " +
            "#{downloadCount}, #{mimeType}, #{checksum}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileInfo fileInfo);
    
    @Select("SELECT * FROM file_info WHERE id = #{id}")
    FileInfo selectById(Long id);
    
    @Select("SELECT * FROM file_info ORDER BY upload_time DESC")
    List<FileInfo> selectAll();
    
    @Select("SELECT * FROM file_info ORDER BY upload_time DESC LIMIT #{offset}, #{limit}")
    List<FileInfo> selectWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    @Update("UPDATE file_info SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(Long id);
    
    @Delete("DELETE FROM file_info WHERE id = #{id}")
    int deleteById(Long id);
    
    @Select("SELECT COUNT(*) FROM file_info")
    long count();
    
    @Select("SELECT * FROM file_info WHERE generated_file_name = #{generatedFileName}")
    FileInfo selectByGeneratedFileName(String generatedFileName);
    
    @Select("SELECT * FROM file_info WHERE original_file_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')")
    List<FileInfo> searchByKeyword(String keyword);
    
    @Select("SELECT * FROM file_info WHERE original_file_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%') ORDER BY upload_time DESC LIMIT #{offset}, #{limit}")
    List<FileInfo> searchByKeywordWithPagination(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM file_info WHERE original_file_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')")
    long countByKeyword(String keyword);
    
    @Update("UPDATE file_info SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    // 客户端查询方法 - 只返回启用的文件
    @Select("SELECT * FROM file_info WHERE status = 1 ORDER BY upload_time DESC")
    List<FileInfo> selectAllEnabled();
    
    @Select("SELECT * FROM file_info WHERE status = 1 ORDER BY upload_time DESC LIMIT #{offset}, #{limit}")
    List<FileInfo> selectEnabledWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    @Select("SELECT * FROM file_info WHERE status = 1 AND (original_file_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    List<FileInfo> searchEnabledByKeyword(String keyword);
    
    @Select("SELECT * FROM file_info WHERE status = 1 AND (original_file_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) ORDER BY upload_time DESC LIMIT #{offset}, #{limit}")
    List<FileInfo> searchEnabledByKeywordWithPagination(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM file_info WHERE status = 1")
    long countEnabled();
    
    @Select("SELECT COUNT(*) FROM file_info WHERE status = 1 AND (original_file_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    long countEnabledByKeyword(String keyword);
}