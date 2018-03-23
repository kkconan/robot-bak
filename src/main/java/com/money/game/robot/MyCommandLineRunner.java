package com.money.game.robot;

import com.money.game.robot.biz.MarketMonitorBiz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author conan
 *         2018/3/23 13:29
 **/
@Component
public class MyCommandLineRunner implements CommandLineRunner {

    @Autowired
    private MarketMonitorBiz marketMonitorBiz;

    @Override
    public void run(String... var1) throws Exception{
//        marketMonitorBiz.huoBiAllSymBolsMonitor();
    }
}