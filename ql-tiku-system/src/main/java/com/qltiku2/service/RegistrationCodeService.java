package com.qltiku2.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.RegistrationCode;
import java.util.List;
import java.util.Map;

/**
 * 注册码服务接口
 * 
 * @author qltiku2
 */
public interface RegistrationCodeService extends IService<RegistrationCode> {
    /**
     * 生成注册码
     */
    RegistrationCode generateCode(Integer maxUses, Integer validDays);
    
    /**
     * 生成注册码（带会员等级）
     */
    RegistrationCode generateCode(Integer maxUses, Integer validDays, Integer membershipLevel);
    
    /**
     * 批量生成注册码
     */
    List<RegistrationCode> batchGenerateCode(Integer count, Integer maxUses, Integer validDays);
    
    /**
     * 批量生成注册码（带会员等级）
     */
    List<RegistrationCode> batchGenerateCode(Integer count, Integer maxUses, Integer validDays, Integer membershipLevel);
    
    /**
     * 验证注册码
     */
    boolean validateCode(String code);
    
    /**
     * 使用注册码
     */
    boolean useCode(String code, Long userId, String ipAddress);
    
    /**
     * 获取注册码列表（分页）
     */
    IPage<RegistrationCode> getRegistrationCodes(int current, int size, String keyword, Boolean isActive, String startDate, String endDate);
    
    /**
     * 获取注册码列表
     */
    List<RegistrationCode> getCodeList();
    
    /**
     * 禁用注册码
     */
    boolean disableCode(Long codeId);
    
    /**
     * 更新注册码状态
     */
    boolean updateStatus(Long id, Boolean isActive);
    
    /**
     * 获取注册码使用统计
     */
    Map<String, Object> getUsageStats();
    
    /**
     * 获取注册码统计信息
     */
    Map<String, Object> getRegistrationCodeStats();
    
    /**
     * 获取注册码使用记录（分页）
     */
    IPage<Map<String, Object>> getUsageRecords(Long codeId, int current, int size);
    
    /**
     * 获取注册码使用次数
     */
    int getUsageCount(Long codeId);
    
    /**
     * 检查用户是否使用过该注册码
     */
    boolean checkUserUsedCode(Long codeId, Long userId);
}