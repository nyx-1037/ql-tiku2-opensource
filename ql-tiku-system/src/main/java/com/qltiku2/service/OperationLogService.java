package com.qltiku2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qltiku2.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {
    
    /**
     * 异步保存操作日志
     * @param operationLog 操作日志
     */
    void saveLogAsync(OperationLog operationLog);
    
    /**
     * 根据IP地址获取地理位置
     * @param ip IP地址
     * @return 地理位置
     */
    String getLocationByIp(String ip);
}