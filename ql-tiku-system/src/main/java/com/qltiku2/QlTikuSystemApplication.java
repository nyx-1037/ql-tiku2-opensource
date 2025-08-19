package com.qltiku2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 在线刷题系统启动类
 * 
 * @author qltiku2
 */
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@MapperScan("com.qltiku2.mapper")
public class QlTikuSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(QlTikuSystemApplication.class, args);
        System.out.println("七洛刷题系统启动成功！");
    }
}