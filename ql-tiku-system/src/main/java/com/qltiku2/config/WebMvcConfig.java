package com.qltiku2.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMVC配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private AsyncTaskExecutor taskExecutor;
    
    /**
     * 配置异步支持
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步任务执行器
        configurer.setTaskExecutor(taskExecutor);
        // 设置异步请求超时时间（毫秒）
        configurer.setDefaultTimeout(30000);
    }
    
    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射 - 使用更具体的路径避免与API冲突
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true);
        
        // 文件上传目录映射
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .resourceChain(true);
        
        // 确保API路径不被静态资源处理器拦截
        registry.setOrder(Ordered.LOWEST_PRECEDENCE);
    }
}