package com.money.game.robot.schedule;

import com.money.game.robot.biz.MarketMonitorBiz;
import com.money.game.robot.biz.TransBiz;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    @Autowired
    private TransBiz transBiz;

    @Scheduled(cron = "${cron.option[huoBi.symBols]:0 0/5 * * * ?}")
    public void huoBiSymBolsSchedule() {
        log.info("huobi all symbol monitor start...");
        this.huoBiAllSymBolsMonitor();
        log.info("huobi all symbol monitor end...");
    }


    @Scheduled(cron = "${cron.option[trans.model.limit.order]:0 0/4 * * * ?}")
    public void checkTransModelLimitOrder() {
        log.info("check to trans model limit order start...");
        transBiz.transModelLimitOrder();
        log.info("check to trans model limit order end...");
    }


    /**
     * 检查是否有成交的实时买单可以挂单售出(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[check.order.to.sale]:0/5 * * * * ?}")
    public void checkRealOrderToSale() {
        transBiz.sale();
    }

    /**
     * 检查实时卖单是否已完成(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[check.order.sale.finish]:0/30 * * * * ?}")
    public void checkRealOrderSaleFinish() {
        transBiz.checkSaleFinish();
    }

    /**
     * 异步方法调用不能再同一个类，否则异步注解不起作用
     */
    public void huoBiAllSymBolsMonitor() {
        List<SymBolsDetailVo> list = huobiApi.getSymbolsInfo();
        List<List<SymBolsDetailVo>> allList = averageAssign(list, 50);
        for (List<SymBolsDetailVo> subList : allList) {
            marketMonitorBiz.asyncDoMonitor(subList);
        }

    }

    /**
     * 将一个list均分成n个list,主要通过偏移量来实现的
     */
    private static <T> List<List<T>> averageAssign(List<T> source, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        int remaider = source.size() % n;  //(先计算出余数)
        int number = source.size() / n;  //然后是商
        int offset = 0;//偏移量
        for (int i = 0; i < n; i++) {
            List<T> value = null;
            if (remaider > 0) {
                value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
                remaider--;
                offset++;
            } else {
                value = source.subList(i * number + offset, (i + 1) * number + offset);
            }
            result.add(value);
        }
        return result;
    }

}
