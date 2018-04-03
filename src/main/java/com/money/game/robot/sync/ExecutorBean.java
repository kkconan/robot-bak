package com.money.game.robot.sync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class ExecutorBean {

    private int corePoolSize = 50;
    private int maxPoolSize = 200;
    private int queueCapacity = 1;


    @Bean
    public Executor marketMonitor() {
        ThreadPoolTaskExecutor executor = init();
        executor.setThreadNamePrefix("marketMonitor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor zbMarketMonitor() {
        ThreadPoolTaskExecutor executor = init();
        executor.setThreadNamePrefix("zbMarketMonitor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor oneMarketMonitor() {
        ThreadPoolTaskExecutor executor = init();
        executor.setThreadNamePrefix("oneMarketMonitor-");
        executor.initialize();
        return executor;
    }

    private ThreadPoolTaskExecutor init() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;

    }

}
