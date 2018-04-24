package com.money.game.robot.schedule;

import com.money.game.robot.biz.HbMarketMonitorBiz;
import com.money.game.robot.biz.ShuffleBiz;
import com.money.game.robot.biz.TransBiz;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private HbMarketMonitorBiz hbMarketMonitorBiz;

    @Autowired
    private TransBiz transBiz;

    @Autowired
    private ShuffleBiz shuffleBiz;

    @Value("${is.schedule:true}")
    private boolean isSchedule;


    /**
     * hb所有交易对实时监控
     */
    @Scheduled(cron = "${cron.option[huoBi.symbols]:55 0/3 * * * ?}")
    public void huoBiSymBolsSchedule() {
        if (isSchedule) {
            log.info("huobi all symbol monitor start...");
            this.huoBiAllSymBolsMonitor();
            log.info("huobi all symbol monitor end...");
        }
    }

    /**
     * hb限价单监控
     */
    @Scheduled(cron = "${cron.option[hb.trans.model.limit.order]:0 0/4 * * * ?}")
    public void checkHbTransModelLimitOrder() {
        if (isSchedule) {
            log.info("check to trans model limit order start...");
            transBiz.hbTransModelLimitOrder();
            log.info("check to trans model limit order end...");
        }
    }


    /**
     * 检查是否有成交的实时买单可以挂单售出(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[hb.check.order.to.Sale]:0/5 * * * * ?}")
    public void checkHbRealOrderToSale() {
        if (isSchedule) {
            transBiz.hbToSale();
        }
    }

    /**
     * 检查实时卖单是否已完成(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[hb.check.order.hb.sale.finish]:0/30 * * * * ?}")
    public void checkHbRealOrderSaleFinish() {
        if (isSchedule) {
            transBiz.hbCheckSaleFinish();
        }
    }

    /**
     * 异步方法调用不能再同一个类，否则异步注解不起作用
     */
    public void huoBiAllSymBolsMonitor() {
        List<SymBolsDetailVo> list = huobiApi.getSymbolsInfo();
        List<List<SymBolsDetailVo>> allList = averageAssign(list, 100);
        for (List<SymBolsDetailVo> subList : allList) {
            hbMarketMonitorBiz.asyncDoMonitor(subList);
        }

    }


    /**
     * zb限价单监控
     */
    @Scheduled(cron = "${cron.option[zb.trans.model.limit.order]:0 0/4 * * * ?}")
    public void checkZbTransModelLimitOrder() {
        if (isSchedule) {
            log.info("zb check to trans model limit order start...");
            transBiz.zbTransModelLimitOrder();
            log.info("zb check to trans model limit order end...");
        }
    }


    /**
     * 检查是否有成交的实时买单可以挂单售出(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[zb.check.order.to.Sale]:0/5 * * * * ?}")
    public void checkZbRealOrderToSale() {
        if (isSchedule) {
            transBiz.zbToSale();
        }
    }

    /**
     * 检查实时卖单是否已完成(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[zb.check.order.hb.sale.finish]:0/30 * * * * ?}")
    public void checkZbRealOrderSaleFinish() {
        if (isSchedule) {
            transBiz.zbCheckSaleFinish();
        }
    }


    /**
     * 检查搬砖单是否已完成(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[check.shuffle.order.finish]:0 0/3 * * * ?}")
    public void chcekShuffleOrderFinish() {
        if (isSchedule) {
            shuffleBiz.checkHbShuffleOrder();
            shuffleBiz.checkZbShuffleOrder();
        }
    }


    /**
     * 检查hb beta限价单(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[check.hb.limit.beta.order]:0 0/2 * * * ?}")
    public void chcekHbLimitBetaOrder() {
        if (isSchedule) {
            transBiz.hbCheckLimitBetaOrder();
        }
    }

    /**
     * 检查zb beta限价单(切日志方法已check开头)
     */
    @Scheduled(cron = "${cron.option[check.zb.limit.beta.order]:30 0/2 * * * ?}")
    public void chcekZbLimitBetaOrder() {
        if (isSchedule) {
            transBiz.zbCheckLimitBetaOrder();
        }
    }

    /**
     * 将一个list均分成n个list,主要通过偏移量来实现的
     */
    private static <T> List<List<T>> averageAssign(List<T> source, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        //(先计算出余数)
        int remaider = source.size() % n;
        //然后是商
        int number = source.size() / n;
        //偏移量
        int offset = 0;
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
