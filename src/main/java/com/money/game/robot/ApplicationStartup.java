package com.money.game.robot;

import com.money.game.robot.biz.ShuffleBiz;
import com.money.game.robot.biz.ZbMarketMonitorBiz;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private ShuffleBiz shuffleBiz;

    @Value("${is.listener.event:true}")
    private boolean listenerEvent;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (listenerEvent) {
            log.info("start application event");
            zbMarketMonitorBiz.initScaleToRedisAndMonitor();
            shuffleBiz.shuffleMonitor();
        }
    }
}
