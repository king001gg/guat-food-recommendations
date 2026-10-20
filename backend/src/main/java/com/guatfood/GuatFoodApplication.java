package com.guatfood;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.guatfood.mapper")
@EnableScheduling
public class GuatFoodApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuatFoodApplication.class, args);
    }
}
