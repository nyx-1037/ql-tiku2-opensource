package com.qltiku2;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        // 数据库中的密码哈希
        String dbPasswordHash = "$2a$10$7JB720yubVSOfvVWbazBOeUlsNqHuqHd.YfNYa6kMBAB.39bZTWBa";
        
        // 测试密码
        String testPassword = "123456";
        
        // 验证密码是否匹配
        boolean matches = passwordEncoder.matches(testPassword, dbPasswordHash);
        
        System.out.println("密码匹配结果: " + matches);
        System.out.println("测试密码: " + testPassword);
        System.out.println("数据库哈希: " + dbPasswordHash);
        
        // 生成新的密码哈希用于比较
        String newHash = passwordEncoder.encode(testPassword);
        System.out.println("新生成的哈希: " + newHash);
        
        // 验证新哈希是否匹配
        boolean newMatches = passwordEncoder.matches(testPassword, newHash);
        System.out.println("新哈希匹配结果: " + newMatches);
    }
}