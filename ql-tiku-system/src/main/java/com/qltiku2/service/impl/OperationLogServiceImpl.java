package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qltiku2.entity.OperationLog;
import com.qltiku2.mapper.OperationLogMapper;
import com.qltiku2.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 操作日志服务实现类
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Async("taskExecutor")
    public void saveLogAsync(OperationLog operationLog) {
        try {
            // 获取地理位置信息
            if (operationLog.getIpAddress() != null && !"127.0.0.1".equals(operationLog.getIpAddress()) 
                && !"0:0:0:0:0:0:0:1".equals(operationLog.getIpAddress())) {
                String location = getLocationByIp(operationLog.getIpAddress());
                operationLog.setLocation(location);
            } else {
                operationLog.setLocation("本地访问");
            }
            
            // 保存日志
            this.save(operationLog);
            log.debug("操作日志保存成功: {}", operationLog.getOperationMethod());
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
    
    @Override
    public String getLocationByIp(String ip) {
        try {
            // 使用免费的IP地理位置API
            String url = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                String status = jsonNode.get("status").asText();
                
                if ("success".equals(status)) {
                    String country = jsonNode.get("country").asText();
                    String regionName = jsonNode.get("regionName").asText();
                    String city = jsonNode.get("city").asText();
                    String isp = jsonNode.get("isp").asText();
                    
                    return String.format("%s %s %s [%s]", country, regionName, city, isp);
                }
            }
        } catch (Exception e) {
            log.warn("获取IP地理位置失败: {}", ip, e);
        }
        
        return "未知位置";
    }
}