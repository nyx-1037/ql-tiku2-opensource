package com.qltiku2.utils;

import com.qltiku2.QlTikuSystemApplication;
import com.qltiku2.entity.SysConfig;
import com.qltiku2.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

// @Component // 暂时注释掉，避免启动冲突
public class DatabaseChecker implements CommandLineRunner {
    
    @Autowired
    private SysConfigMapper sysConfigMapper;
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(QlTikuSystemApplication.class, args);
        context.close();
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 开始检查系统配置数据 ===");
        
        try {
            // 查询所有系统配置
            List<SysConfig> allConfigs = sysConfigMapper.selectList(null);
            System.out.println("数据库中所有配置项数量: " + allConfigs.size());
            
            for (SysConfig config : allConfigs) {
                 System.out.println("ID: " + config.getId() + 
                                  ", Key: " + config.getConfigKey() + 
                                  ", Value: " + config.getConfigValue() + 
                                  ", Type: " + config.getConfigType() + 
                                  ", Name: " + config.getConfigName());
             }
            
            System.out.println("\n=== 查询系统配置 (config_type = 'Y') ===");
            List<SysConfig> systemConfigs = sysConfigMapper.getSystemConfigs();
            System.out.println("系统配置项数量: " + systemConfigs.size());
            
            for (SysConfig config : systemConfigs) {
                System.out.println("Key: " + config.getConfigKey() + 
                                 ", Value: " + config.getConfigValue() + 
                                 ", Type: " + config.getConfigType());
            }
            
        } catch (Exception e) {
            System.out.println("数据库查询失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== 检查完成 ===");
    }
}