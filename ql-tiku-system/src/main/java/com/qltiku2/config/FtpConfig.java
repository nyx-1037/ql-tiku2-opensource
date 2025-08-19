package com.qltiku2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FTP配置类
 * 专用于资料库文件管理
 */
@Data
@Component
@ConfigurationProperties(prefix = "ftp")
public class FtpConfig {
    
    /**
     * FTP服务器配置
     */
    private Server server = new Server();
    
    /**
     * 文件存储配置
     */
    private Storage storage = new Storage();
    
    /**
     * 文件上传配置
     */
    private Upload upload = new Upload();
    
    @Data
    public static class Server {
        /**
         * FTP服务器地址
         */
        private String host = "localhost";
        
        /**
         * FTP服务器端口
         */
        private int port = 21;
        
        /**
         * FTP用户名
         */
        private String username = "ftpuser";
        
        /**
         * FTP密码
         */
        private String password = "ftppass";
        
        /**
         * 是否使用被动模式
         */
        private boolean passiveMode = true;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 30000;
        
        /**
         * 数据传输超时时间（毫秒）
         */
        private int dataTimeout = 60000;
        
        /**
         * 控制连接超时时间（毫秒）
         */
        private int controlTimeout = 30000;
    }
    
    @Data
    public static class Storage {
        /**
         * FTP服务器上的根目录
         */
        private String rootPath = "/resource-library/";
        
        /**
         * 临时文件目录
         */
        private String tempPath = "/tmp/";
        
        /**
         * 文件访问URL前缀
         */
        private String urlPrefix = "http://localhost:8080/ftp/files/";
    }
    
    @Data
    public static class Upload {
        /**
         * 单个文件最大大小（字节）
         */
        private long maxFileSize = 104857600L; // 100MB
        
        /**
         * 允许的文件类型
         */
        private List<String> allowedTypes = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            "zip", "rar", "7z", "jpg", "jpeg", "png", "gif",
            "mp4", "avi", "mp3", "wav"
        );
    }
}