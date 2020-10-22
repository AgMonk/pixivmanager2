package com.gin.pixivmanager2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan(value = {"com.gin.pixivmanager2.dao"})
public class Pixivmanager2Application {

    public static void main(String[] args) {
        SpringApplication.run(Pixivmanager2Application.class, args);
    }

}
