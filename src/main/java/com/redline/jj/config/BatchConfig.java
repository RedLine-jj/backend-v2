package com.redline.jj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

/**
 * Spring Boot 3.x는 @EnableBatchProcessing 없이 auto-configuration 사용.
 * Job/Step은 도메인별 기능 구현 시 별도 BatchJobConfig로 정의.
 */
@Configuration
public class BatchConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("batch-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
