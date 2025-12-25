package com.hotelsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 管理端应用 - 运行在8081端口
 * 使用 application-admin.yml 配置文件
 */
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
public class AdminApplication {

    public static void main(String[] args) {
        System.setProperty("spring.config.name", "application-admin");
        SpringApplication.run(AdminApplication.class, args);
    }
}

