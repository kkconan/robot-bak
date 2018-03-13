package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.mail.MailQQ;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.sms.Sms;
import com.money.game.robot.vo.client.MarketDetailVo;
import com.money.game.robot.vo.client.MarketInfoVo;
import com.money.game.robot.vo.client.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author conan
 *         2018/3/8 15:58
 **/
@Component
@Slf4j
public class MarketMonitorBiz {

    /**
     * 一分钟变动阈值
     */
    @Value("${one.min.threshold:0.05}")
    private BigDecimal oneMinThreshold;

    /**
     * 五分钟变动阈值
     */
    @Value("${five.min.threshold:0.1}")
    private BigDecimal fiveMinThreshold;

    /**
     * 不同基础交易对之间绝对值差异
     */
    @Value("${base.currency.absolute:0.05}")
    private BigDecimal baseCurrencyAbsolute;


    @Value("${mail.users:824968443@qq.com}")
    private String mailToUser;


    @Value("${need.sms:false}")
    private boolean needSms;

    @Value("${sms.mobiles:13564452580}")
    private String mobiles;


    @Autowired
    private HuobiApi huobiApi;


    @Async("marketMonitor")
    public void asyncDoMonitor(List<SymBolsDetailVo> list) {
        for (SymBolsDetailVo detailVo : list) {
            huoBiMonitor(detailVo.getSymbols());
        }
    }

    @Async("oneMarketMonitor")
    public void asyncOneDoMonitor(SymBolsDetailVo detailVo) {
        huoBiMonitor(detailVo.getSymbols());
    }

    public void huoBiMonitor(String symbol) {
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, symbol);

        if (info != null && info.getData().size() > 0) {
            MarketDetailVo nowVo = info.getData().get(0);
            // 1min monitor
            oneMinMonitor(symbol, nowVo, info.getData());
            // 5min monitor
            fiveMinMonitor(symbol, nowVo, info.getData());
        }

    }

    private void oneMinMonitor(String symbol, MarketDetailVo nowVo, List<MarketDetailVo> detailVos) {
        MarketDetailVo lastMinVo = detailVos.get(1);
        initMonitor(symbol, nowVo, lastMinVo, oneMinThreshold);
    }

    private void fiveMinMonitor(String symbol, MarketDetailVo nowVo, List<MarketDetailVo> detailVos) {
        MarketDetailVo lastMinVo = detailVos.get(5);
        initMonitor(symbol, nowVo, lastMinVo, fiveMinThreshold);
    }

    private void initMonitor(String symbol, MarketDetailVo nowVo, MarketDetailVo otherVo, BigDecimal threshold) {
        BigDecimal nowPrice = nowVo.getClose();
        BigDecimal otherMinPrice = otherVo.getClose();
        BigDecimal increase = (nowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        BigDecimal hundredIncrease = increase.multiply(new BigDecimal(100));
        String content;
        //指定时间段内价格降低超过阈值
        if (increase.compareTo(BigDecimal.ZERO) < 0 && (BigDecimal.ZERO.subtract(threshold)).compareTo(increase) >= 0) {
            if (threshold.equals(oneMinThreshold)) {
                content = symbol + " one min to lower " + hundredIncrease + "%";
            } else {
                content = symbol + " five min to lower " + hundredIncrease + "%";
            }
            log.info(content);
            sendNotifyEmail(content);
            sendNotifySms(content, mobiles, symbol, increase, threshold);
        }
        //指定时间段内价格升高超过阈值
        if (increase.compareTo(BigDecimal.ZERO) > 0 && threshold.compareTo(increase) <= 0) {
            if (threshold.equals(oneMinThreshold)) {
                content = symbol + " one min to hoist " + hundredIncrease + "%";
            } else {
                content = symbol + " five min to hoist " + hundredIncrease + "%";
            }
            log.info(content);
            sendNotifyEmail(content);
            sendNotifySms(content, mobiles, symbol, increase, threshold);
        }
    }

    /**
     * if need to send sms notify
     */
    private void sendNotifySms(String content, String mobiles, String symbol, BigDecimal increase, BigDecimal threshold) {
        if (!needSms) {
            log.info("do not need sms...");
            return;
        }
        //应用对
        String quoteCurrency;
        String otherSymbol;
        boolean result;
        //is btc
        if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode())) {
            quoteCurrency = symbol.substring(0, symbol.length() - 3);
            otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_ETH.getCode();
            result = compareToOtherCurrency(otherSymbol, increase, threshold);
        }
        //is eth
        else if (symbol.endsWith(DictEnum.MARKET_BASE_ETH.getCode())) {
            quoteCurrency = symbol.substring(0, symbol.length() - 3);
            otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_BTC.getCode();
            result = compareToOtherCurrency(otherSymbol, increase, threshold);
        }
        //is usdt
        else {
            quoteCurrency = symbol.substring(0, symbol.length() - 4);
            otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_BTC.getCode();
            result = compareToOtherCurrency(otherSymbol, increase, threshold);
            //to btc is false,use eth
            if (!result) {
                otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_ETH.getCode();
                result = compareToOtherCurrency(otherSymbol, increase, threshold);
            }
        }
        if (result) {
            Sms.smsSend(content, mobiles);
        }
    }

    /**
     * compare with other currency growth rate
     */
    private boolean compareToOtherCurrency(String otherSymbol, BigDecimal increase, BigDecimal threshold) {
        boolean reslut = false;
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, otherSymbol);
        if (info == null) {
            log.info("other currency not found.");
            return reslut;
        }
        log.info("marketInfo={}", info);
        MarketDetailVo nowVo = info.getData().get(0);
        BigDecimal nowPrice = nowVo.getClose();
        BigDecimal otherMinPrice;
        BigDecimal otherMinIncrease;
        if (oneMinThreshold.equals(threshold)) {
            MarketDetailVo oneMinVo = info.getData().get(1);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (nowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        } else {
            MarketDetailVo oneMinVo = info.getData().get(5);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (nowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        }
        //上升
        if (increase.compareTo(BigDecimal.ZERO) >= 0) {
            // |a-b| < 0.05 两个交易对之间差异过大
            if (new BigDecimal(Math.abs(increase.subtract(otherMinIncrease).doubleValue())).compareTo(baseCurrencyAbsolute) > 0) {
                reslut = true;
            }
        }
        //下降
        if (increase.compareTo(BigDecimal.ZERO) < 0) {
            // |b-a| < 0.05 两个交易对之间差异过大
            if (new BigDecimal(Math.abs(otherMinIncrease.subtract(increase).doubleValue())).compareTo(baseCurrencyAbsolute) > 0) {
                reslut = true;
            }
        }
        log.info("compare to other currency. otherSymbol={},increase={},nowPrice={},otherMinPrice={},otherMinIncrease={},baseCurrencyAbsolute={},threshold={},reslut={}",otherSymbol,increase, nowPrice, otherMinPrice, otherMinIncrease, baseCurrencyAbsolute, threshold,reslut);
        return reslut;
    }

    private void sendNotifyEmail(String content) {
        String subject = "market info notify";
        MailQQ.sendEmail(subject, content, mailToUser);
    }

}
