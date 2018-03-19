package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.huobi.api.ApiException;
import com.money.game.robot.mail.MailQQ;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.sms.Sms;
import com.money.game.robot.vo.huobi.MarketDetailVo;
import com.money.game.robot.vo.huobi.MarketInfoVo;
import com.money.game.robot.vo.huobi.RateChangeVo;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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

    @Autowired
    private TransBiz transBiz;


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
        boolean isToOperate = false;
        String content = "";
        //指定时间段内价格降低超过阈值
        if (increase.compareTo(BigDecimal.ZERO) < 0 && (BigDecimal.ZERO.subtract(threshold)).compareTo(increase) >= 0) {
            if (threshold.equals(oneMinThreshold)) {
                content = symbol + " one min to lower " + hundredIncrease + "%";
            } else {
                content = symbol + " five min to lower " + hundredIncrease + "%";
            }
            isToOperate = true;
            log.info("nowVo={},otherVo={},threshold={},content={}", nowVo, otherVo, threshold, content);
        }
        //指定时间段内价格升高超过阈值
        if (increase.compareTo(BigDecimal.ZERO) > 0 && threshold.compareTo(increase) <= 0) {
            if (threshold.equals(oneMinThreshold)) {
                content = symbol + " one min to hoist " + hundredIncrease + "%";
            } else {
                content = symbol + " five min to hoist " + hundredIncrease + "%";
            }
            isToOperate = true;
            log.info("nowVo={},otherVo={},threshold={},content={}", nowVo, otherVo, threshold, content);
        }
        if (isToOperate) {
            //check trans
            checkToTrans(symbol, increase, threshold);
            //send eamil
            sendNotifyEmail(content);
            //send sms
            sendSms(content, symbol);
        }
    }

    /**
     * 检查是否可交易
     */
    public void checkToTrans(String symbol, BigDecimal increase, BigDecimal threshold) {
        log.info("checkToTrans,symbol={},increase={},threshold={}", symbol, increase, threshold);
        RateChangeVo rateChangeVo;
        BigDecimal salePrice;
        //应用对
        String quoteCurrency = getQuoteCurrency(symbol);

        boolean tranResult = false;

        String otherSymbol;
        //is btc
        if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode())) {
            //compare to eth
            otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_ETH.getCode();
            rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, threshold);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_BTC.getCode(), DictEnum.MARKET_BASE_ETH.getCode());
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_BTC.getCode(), DictEnum.MARKET_BASE_ETH.getCode());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_USDT.getCode();
                rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, threshold);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_BTC.getCode(), DictEnum.MARKET_BASE_USDT.getCode());
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_BTC.getCode(), DictEnum.MARKET_BASE_USDT.getCode());
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice);
                }
            }
        }
        //is eth
        else if (symbol.endsWith(DictEnum.MARKET_BASE_ETH.getCode())) {
            //compare to btc
            otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_BTC.getCode();
            rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, threshold);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_ETH.getCode(), DictEnum.MARKET_BASE_BTC.getCode());
                }
                //原对下降
                else {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_ETH.getCode(), DictEnum.MARKET_BASE_BTC.getCode());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_USDT.getCode();
                rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, threshold);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_ETH.getCode(), DictEnum.MARKET_BASE_USDT.getCode());
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_ETH.getCode(), DictEnum.MARKET_BASE_USDT.getCode());
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice);
                }
            }
        }
        //is usdt
        else {
            //compare to btc
            otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_BTC.getCode();
            rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, threshold);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_USDT.getCode(), DictEnum.MARKET_BASE_BTC.getCode());
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_USDT.getCode(), DictEnum.MARKET_BASE_BTC.getCode());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice);
            }
            //compare to eth
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.MARKET_BASE_ETH.getCode();
                rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, threshold);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_USDT.getCode(), DictEnum.MARKET_BASE_ETH.getCode());
                    }
                    //原对下降
                    else {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.MARKET_BASE_USDT.getCode(), DictEnum.MARKET_BASE_ETH.getCode());
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice);
                }
            }
        }
    }

    private boolean checkTransResult(RateChangeVo rateChangeVo, String quoteCurrency, BigDecimal salePrice) {
        boolean tranResult = false;
        try {
            rateChangeVo.setQuoteCurrency(quoteCurrency);
            //set sale price
            rateChangeVo.setSalePrice(salePrice);
            //要购买的交易对主对
            String baseCurrency = getBaseCurrency(rateChangeVo.getBuyerSymbol());
            rateChangeVo.setBaseCurrency(baseCurrency);
            //交易
            transBiz.trans(rateChangeVo);
            tranResult = true;
        } catch (ApiException e) {
            log.warn("trans fail.e={}", e);
        }
        return tranResult;
    }

    /**
     * compare with other currency growth rate
     */
    private RateChangeVo compareToOtherCurrency(String originSymbol, String otherSymbol, BigDecimal increase, BigDecimal threshold) {
        RateChangeVo rateChangeVo = new RateChangeVo();
        rateChangeVo.setOriginSymbol(originSymbol);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, otherSymbol);
        if (info == null) {
            log.info("other currency not found.");
            return rateChangeVo;
        }
        log.info("marketInfo={}", info);
        MarketDetailVo otherSymbolNowVo = info.getData().get(0);
        BigDecimal otherNowPrice = otherSymbolNowVo.getClose();
        BigDecimal otherMinPrice;
        BigDecimal otherMinIncrease;
        if (oneMinThreshold.equals(threshold)) {
            MarketDetailVo oneMinVo = info.getData().get(1);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (otherNowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        } else {
            MarketDetailVo oneMinVo = info.getData().get(5);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (otherNowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        }
        //上升
        if (increase.compareTo(BigDecimal.ZERO) >= 0) {
            // |a-b| > 0.05 两个交易对之间差异过大
            if (new BigDecimal(Math.abs(increase.subtract(otherMinIncrease).doubleValue())).compareTo(baseCurrencyAbsolute) > 0) {
                rateChangeVo.setBuyerSymbol(otherSymbol);
                rateChangeVo.setBuyPrice(otherNowPrice);
                rateChangeVo.setSaleSymbol(originSymbol);
                rateChangeVo.setNowMarketDetailVo(otherSymbolNowVo);
                rateChangeVo.setRateValue(increase.subtract(otherMinIncrease));


            }
        }
        //下降
        if (increase.compareTo(BigDecimal.ZERO) < 0) {
            // |b-a| > 0.05 两个交易对之间差异过大
            if (new BigDecimal(Math.abs(otherMinIncrease.subtract(increase).doubleValue())).compareTo(baseCurrencyAbsolute) > 0) {
                rateChangeVo.setBuyerSymbol(originSymbol);
                rateChangeVo.setSaleSymbol(otherSymbol);
                info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, originSymbol);
                MarketDetailVo detailVo = info.getData().get(0);
                rateChangeVo.setBuyPrice(detailVo.getClose());
                rateChangeVo.setNowMarketDetailVo(detailVo);
                rateChangeVo.setRateValue(otherMinIncrease.subtract(increase));
            }
        }
        log.info("compare to other currency. otherSymbol={},increase={},nowPrice={},otherMinPrice={},otherMinIncrease={},baseCurrencyAbsolute={},threshold={},rateChangeVo={}", otherSymbol, increase, otherNowPrice, otherMinPrice, otherMinIncrease, baseCurrencyAbsolute, threshold, rateChangeVo);
        return rateChangeVo;
    }

    private void sendSms(String content, String symbol) {
        if (!needSms) {
            log.info("do not need sms...");
            return;
        }
        if (StringUtils.isNotEmpty(symbol)) {
            Sms.smsSend(content, mobiles);
        }
    }

    private void sendNotifyEmail(String content) {
        String subject = "market info notify";
        MailQQ.sendEmail(subject, content, mailToUser);
    }


    private void setSalePrice(RateChangeVo rateChangeVo, String base1, String base2) {
        BigDecimal salePrice;
        if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
            //原对增长
            if (rateChangeVo.getRateValue().compareTo(BigDecimal.ZERO) > 0) {
                salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), base1, base2);
            }
            //原对下降
            else {
                salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), base1, base2);
            }
            rateChangeVo.setSalePrice(salePrice);
        }
    }

    /**
     * 相对主对相乘汇率
     *
     * @param buyPrice 买价
     * @param base1    主对1
     * @param base2    主对2
     */
    private BigDecimal getMultiplySalePrice(BigDecimal buyPrice, String base1, String base2) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, baseCurrencyGroup);
        return buyPrice.multiply(info.getData().get(0).getClose()).multiply(new BigDecimal(1).add(baseCurrencyAbsolute));
    }

    /**
     * 相对主对相除汇率
     */
    private BigDecimal getDivideSalePrice(BigDecimal buyPrice, String base1, String base2) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, baseCurrencyGroup);
        return buyPrice.divide(info.getData().get(0).getClose(), 8, BigDecimal.ROUND_FLOOR).multiply(new BigDecimal(1).add(baseCurrencyAbsolute));
    }


    /**
     * 两个主对组合顺序
     */
    private String twoBaseCurrencyGroup(String base1, String base2) {
        String baseSymbol = null;
        //is usdt
        if (DictEnum.MARKET_BASE_USDT.getCode().equals(base1)) {
            baseSymbol = base2 + base1;
        } else if (DictEnum.MARKET_BASE_USDT.getCode().equals(base2)) {
            baseSymbol = base1 + base2;
        } else if (DictEnum.MARKET_BASE_BTC.getCode().equals(base1)) {
            baseSymbol = base2 + base1;
        } else if (DictEnum.MARKET_BASE_BTC.getCode().equals(base2)) {
            baseSymbol = base1 + base2;
        }
        log.info("baseSymbol={}", baseSymbol);
        return baseSymbol;
    }

    private String getBaseCurrency(String symbol) {
        String baseCurrency;
        if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode()) || symbol.endsWith(DictEnum.MARKET_BASE_ETH.getCode())) {
            baseCurrency = symbol.substring(symbol.length() - 3, symbol.length());
        } else {
            baseCurrency = symbol.substring(symbol.length() - 4, symbol.length());
        }
        return baseCurrency;
    }

    private String getQuoteCurrency(String symbol) {
        String quoteCurrency;
        if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode()) || symbol.endsWith(DictEnum.MARKET_BASE_ETH.getCode())) {
            quoteCurrency = symbol.substring(0, symbol.length() - 3);
        } else {
            quoteCurrency = symbol.substring(0, symbol.length() - 4);
        }
        return quoteCurrency;
    }
}
