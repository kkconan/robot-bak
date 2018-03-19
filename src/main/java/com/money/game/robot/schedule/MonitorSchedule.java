package com.money.game.robot.schedule;

import com.money.game.robot.biz.MarketMonitorBiz;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author conan
 *         2018/1/9 17:58
 **/
@Component
@Slf4j
public class MonitorSchedule {


    @Autowired
    private HuobiApi huobiApi;

    @Autowired
    private MarketMonitorBiz marketMonitorBiz;

    /**
     * 一次线程最多执行次数
     */
    private static Integer asyncDoCount = 1;

    @Scheduled(cron = "${cron.option[huoBi.symBols]:0/59 * * * * ?}")
    public void huoBiSymBols() {
        log.info("huoBi all symBols monitor start...");
        this.huoBiAllSymBolsMonitor();
    }


    /**
     * 异步方法调用不能再同一个类，否则异步注解不起作用
     */
    public void huoBiAllSymBolsMonitor() {
        List<SymBolsDetailVo> list = huobiApi.getSymbolsInfo();
        for (SymBolsDetailVo detailVo : list) {
            marketMonitorBiz.asyncOneDoMonitor(detailVo);

        }
    }

}
