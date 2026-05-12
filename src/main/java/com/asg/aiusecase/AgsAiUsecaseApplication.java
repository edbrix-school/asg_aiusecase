package com.asg.aiusecase;

import com.asg.aiusecase.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AgsAiUsecaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgsAiUsecaseApplication.class, args);
    }
}
