package com.notaskflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notask Flow 后端服务启动入口。
 *
 * @author LIN
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.notaskflow.mapper")
@SpringBootApplication
public class NotaskFlowApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NotaskFlowApplication.class, args);
    }
}
