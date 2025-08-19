package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.entity.RegistrationCode;
import com.qltiku2.entity.RegistrationCodeUsage;
import com.qltiku2.mapper.RegistrationCodeMapper;
import com.qltiku2.mapper.RegistrationCodeUsageMapper;
import com.qltiku2.service.RegistrationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 注册码服务实现类
 * 
 * @author qltiku2
 */
@Slf4j
@Service
public class RegistrationCodeServiceImpl extends ServiceImpl<RegistrationCodeMapper, RegistrationCode> implements RegistrationCodeService {
    
    @Autowired
    private RegistrationCodeMapper registrationCodeMapper;
    
    @Autowired
    private RegistrationCodeUsageMapper registrationCodeUsageMapper;
    
    @Override
    public RegistrationCode generateCode(Integer maxUses, Integer validDays) {
        return generateCode(maxUses, validDays, 0); // 默认普通用户
    }
    
    @Override
    public RegistrationCode generateCode(Integer maxUses, Integer validDays, Integer membershipLevel) {
        RegistrationCode code = new RegistrationCode();
        code.setCode(generateUniqueCode());
        code.setMaxUses(maxUses);
        code.setUsedCount(0);
        code.setValidFrom(LocalDateTime.now());
        code.setValidUntil(LocalDateTime.now().plusDays(validDays));
        code.setIsActive(true);
        code.setMembershipLevel(membershipLevel);
        code.setCreatedBy(1L); // 默认管理员创建
        code.setCreatedTime(LocalDateTime.now());
        code.setUpdatedTime(LocalDateTime.now());
        
        registrationCodeMapper.insert(code);
        log.info("生成注册码: {}, 最大使用次数: {}, 有效期: {}天, 会员等级: {}", code.getCode(), maxUses, validDays, membershipLevel);
        
        return code;
    }
    
    @Override
    public boolean validateCode(String code) {
        RegistrationCode regCode = registrationCodeMapper.findValidCode(code);
        return regCode != null;
    }
    
    @Override
    @Transactional
    public boolean useCode(String code, Long userId, String ipAddress) {
        RegistrationCode regCode = registrationCodeMapper.findValidCode(code);
        if (regCode == null) {
            return false;
        }
        
        // 检查用户是否已经使用过该注册码
        if (checkUserUsedCode(regCode.getId(), userId)) {
            log.warn("用户 {} 已经使用过注册码 {}, 拒绝重复记录", userId, code);
            return false;
        }
        
        // 增加注册码使用次数
        registrationCodeMapper.incrementUsedCount(regCode.getId());
        
        // 记录使用记录到registration_code_usage表
        RegistrationCodeUsage usageRecord = new RegistrationCodeUsage();
        usageRecord.setCodeId(regCode.getId());
        usageRecord.setUserId(userId);
        usageRecord.setIpAddress(ipAddress);
        registrationCodeUsageMapper.insert(usageRecord);
        
        log.info("用户 {} 使用注册码 {} 成功，记录ID: {}, IP: {}", userId, code, usageRecord.getId(), ipAddress);
        return true;
    }
    
    @Override
    public List<RegistrationCode> getCodeList() {
        QueryWrapper<RegistrationCode> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_time");
        return registrationCodeMapper.selectList(wrapper);
    }
    
    @Override
    public boolean disableCode(Long codeId) {
        RegistrationCode code = registrationCodeMapper.selectById(codeId);
        if (code != null) {
            code.setIsActive(false);
            code.setUpdatedTime(LocalDateTime.now());
            registrationCodeMapper.updateById(code);
            log.info("禁用注册码: {}", code.getCode());
            return true;
        }
        return false;
    }
    
    /**
     * 生成唯一的注册码
     */
    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        } while (isCodeExists(code));
        return code;
    }
    
    /**
     * 检查注册码是否已存在
     */
    private boolean isCodeExists(String code) {
        QueryWrapper<RegistrationCode> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        return registrationCodeMapper.selectCount(wrapper) > 0;
    }
    
    @Override
    @Transactional
    public List<RegistrationCode> batchGenerateCode(Integer count, Integer maxUses, Integer validDays) {
        return batchGenerateCode(count, maxUses, validDays, 0); // 默认普通用户
    }
    
    @Override
    @Transactional
    public List<RegistrationCode> batchGenerateCode(Integer count, Integer maxUses, Integer validDays, Integer membershipLevel) {
        List<RegistrationCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RegistrationCode code = generateCode(maxUses, validDays, membershipLevel);
            codes.add(code);
        }
        log.info("批量生成注册码 {} 个, 会员等级: {}", count, membershipLevel);
        return codes;
    }
    
    @Override
    public IPage<RegistrationCode> getRegistrationCodes(int current, int size, String keyword, Boolean isActive, String startDate, String endDate) {
        Page<RegistrationCode> page = new Page<>(current, size);
        QueryWrapper<RegistrationCode> wrapper = new QueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("code", keyword)
                           .or().like("remark", keyword));
        }
        
        // 状态筛选
        if (isActive != null) {
            wrapper.eq("is_active", isActive);
        }
        
        // 日期范围筛选
        if (StringUtils.hasText(startDate)) {
            wrapper.ge("created_time", LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("created_time", LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        
        wrapper.orderByDesc("created_time");
        return registrationCodeMapper.selectPage(page, wrapper);
    }
    
    @Override
    public boolean updateStatus(Long id, Boolean isActive) {
        try {
            RegistrationCode code = registrationCodeMapper.selectById(id);
            if (code == null) {
                return false;
            }
            code.setIsActive(isActive);
            code.setUpdatedTime(LocalDateTime.now());
            return registrationCodeMapper.updateById(code) > 0;
        } catch (Exception e) {
            log.error("更新注册码状态失败", e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 总注册码数
            long totalCodes = registrationCodeMapper.selectCount(null);
            stats.put("totalCodes", totalCodes);
            
            // 有效注册码数
            QueryWrapper<RegistrationCode> activeWrapper = new QueryWrapper<>();
            activeWrapper.eq("is_active", true)
                        .gt("valid_until", LocalDateTime.now());
            long activeCodes = registrationCodeMapper.selectCount(activeWrapper);
            stats.put("activeCodes", activeCodes);
            
            // 已使用注册码数
            QueryWrapper<RegistrationCode> usedWrapper = new QueryWrapper<>();
            usedWrapper.gt("used_count", 0);
            long usedCodes = registrationCodeMapper.selectCount(usedWrapper);
            stats.put("usedCodes", usedCodes);
            
            // 过期注册码数
            QueryWrapper<RegistrationCode> expiredWrapper = new QueryWrapper<>();
            expiredWrapper.lt("valid_until", LocalDateTime.now());
            long expiredCodes = registrationCodeMapper.selectCount(expiredWrapper);
            stats.put("expiredCodes", expiredCodes);
            
        } catch (Exception e) {
            log.error("获取注册码使用统计失败", e);
        }
        return stats;
    }
    
    @Override
    public Map<String, Object> getRegistrationCodeStats() {
        return getUsageStats();
    }
    
    @Override
    public IPage<Map<String, Object>> getUsageRecords(Long codeId, int current, int size) {
        Page<Map<String, Object>> page = new Page<>(current, size);
        return registrationCodeUsageMapper.getUsageRecordsByCodeId(page, codeId);
    }
    
    @Override
    public int getUsageCount(Long codeId) {
        return registrationCodeUsageMapper.countUsageByCodeId(codeId);
    }
    
    @Override
    public boolean checkUserUsedCode(Long codeId, Long userId) {
        int count = registrationCodeUsageMapper.checkUserUsedCode(codeId, userId);
        return count > 0;
    }
}