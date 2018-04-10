package com.money.game.robot;

import com.money.game.robot.biz.ZbMarketMonitorBiz;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author conan
 *         2018/4/9 15:59
 **/
@Slf4j
@Component
@Async
public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ZbMarketMonitorBiz zbMarketMonitorBiz;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("start application event");
        zbMarketMonitorBiz.initScaleToRedisAndMonitor();
    }
}
