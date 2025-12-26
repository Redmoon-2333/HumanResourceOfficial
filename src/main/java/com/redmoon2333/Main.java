package com.redmoon2333;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.redmoon2333.mapper")
@EnableScheduling  // 启用定时任务，用于定期清理Redis内存
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}