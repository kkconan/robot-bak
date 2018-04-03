package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.entity.UserEntity;
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
public class HbMarketMonitorBiz {


    @Value("${need.sms:false}")
    private boolean needSms;

    @Autowired
    private HuobiApi huobiApi;

    @Autowired
    private TransBiz transBiz;

    @Autowired
    private UserBiz userBiz;

    @Autowired
    private SymbolTradeConfigBiz symbolTradeConfigBiz;

    @Autowired
    private MarketRuleBiz marketRuleBiz;


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
            List<UserEntity> userList = userBiz.findAllByNormal();
            MarketDetailVo nowVo = info.getData().get(0);
            // 1min monitor
            oneMinMonitor(symbol, nowVo, info.getData(), userList);
            // 5min monitor
            fiveMinMonitor(symbol, nowVo, info.getData(), userList);
        }

    }

    private void oneMinMonitor(String symbol, MarketDetailVo nowVo, List<MarketDetailVo> detailVos, List<UserEntity> userList) {
        MarketDetailVo lastMinVo = detailVos.get(1);
        for (UserEntity user : userList) {
            //查询用户一分钟交易配置
            SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findByUserIdAndThresholdType(user.getOid(), DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode());
            if (symbolTradeConfig != null) {
                initMonitor(symbol, nowVo, lastMinVo, symbolTradeConfig, user);
            }
        }
    }

    private void fiveMinMonitor(String symbol, MarketDetailVo nowVo, List<MarketDetailVo> detailVos, List<UserEntity> userList) {
        MarketDetailVo lastMinVo = detailVos.get(5);
        for (UserEntity user : userList) {
            //查询用户五分钟交易配置
            SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findByUserIdAndThresholdType(user.getOid(), DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_FIVE_MIN.getCode());
            if (symbolTradeConfig != null) {
                initMonitor(symbol, nowVo, lastMinVo, symbolTradeConfig, user);
            }
        }
    }

    private void initMonitor(String symbol, MarketDetailVo nowVo, MarketDetailVo otherVo, SymbolTradeConfigEntity symbolTradeConfig, UserEntity user) {
        BigDecimal nowPrice = nowVo.getClose();
        BigDecimal otherMinPrice = otherVo.getClose();
        BigDecimal increase = (nowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        BigDecimal hundredIncrease = increase.multiply(new BigDecimal(100));
        boolean isToOperate = false;
        String content = "";
        //指定时间段内价格降低超过阈值
        if (increase.compareTo(BigDecimal.ZERO) < 0 && (BigDecimal.ZERO.subtract(symbolTradeConfig.getCurrencyAbsolute())).compareTo(increase) >= 0) {
            if (DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode().equals(symbolTradeConfig.getThresholdType())) {
                content = symbol + " one min to lower " + hundredIncrease + "%";
            } else {
                content = symbol + " five min to lower " + hundredIncrease + "%";
            }
            isToOperate = true;
            log.info("nowVo={},otherVo={},thresholdType={},currencyAbsolute={},content={}", nowVo, otherVo, symbolTradeConfig.getThresholdType(), symbolTradeConfig.getCurrencyAbsolute(), content);
        }
        //指定时间段内价格升高超过阈值
        if (increase.compareTo(BigDecimal.ZERO) > 0 && symbolTradeConfig.getCurrencyAbsolute().compareTo(increase) <= 0) {
            if (DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode().equals(symbolTradeConfig.getThresholdType())) {
                content = symbol + " one min to hoist " + hundredIncrease + "%";
            } else {
                content = symbol + " five min to hoist " + hundredIncrease + "%";
            }
            isToOperate = true;
            log.info("nowVo={},otherVo={},thresholdType={},currencyAbsolute={},content={}", nowVo, otherVo, symbolTradeConfig.getThresholdType(), symbolTradeConfig.getCurrencyAbsolute(), content);
        }
        if (isToOperate) {
            // send eamil
            sendNotifyEmail(content, user.getNotifyEmail());
            //check buy
            boolean transResult = checkToTrans(symbol, increase, symbolTradeConfig);
            //trans success to send sms
            if (transResult) {
                sendSms(content, symbol, user.getNotifyPhone());
            }
        }
    }

    /**
     * 检查是否可交易
     */
    public boolean checkToTrans(String symbol, BigDecimal increase, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("checkToZbTrans,symbol={},increase={}", symbol, increase);
        RateChangeVo rateChangeVo;
        BigDecimal salePrice;
        //应用对
        String quoteCurrency = marketRuleBiz.getHbQuoteCurrency(symbol);

        boolean tranResult = false;

        String otherSymbol;
        //is btc
        if (symbol.endsWith(DictEnum.HB_MARKET_BASE_BTC.getCode())) {
            //compare to eth
            otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_ETH.getCode();
            rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), symbolTradeConfig);
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), symbolTradeConfig);
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_USDT.getCode();
                rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), symbolTradeConfig);
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), symbolTradeConfig);
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
                }
            }
        }
        //is eth
        else if (symbol.endsWith(DictEnum.HB_MARKET_BASE_ETH.getCode())) {
            //compare to btc
            otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_BTC.getCode();
            rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), symbolTradeConfig);
                }
                //原对下降
                else {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), symbolTradeConfig);
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_USDT.getCode();
                rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), symbolTradeConfig);
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), symbolTradeConfig);
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
                }
            }
        }
        //is usdt
        else {
            //compare to btc
            otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_BTC.getCode();
            rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), symbolTradeConfig);
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), symbolTradeConfig);
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to eth
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_ETH.getCode();
                rateChangeVo = compareToOtherCurrency(symbol, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), symbolTradeConfig);
                    }
                    //原对下降
                    else {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), symbolTradeConfig);
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
                }
            }
        }
        return tranResult;
    }

    private boolean checkTransResult(RateChangeVo rateChangeVo, String quoteCurrency, BigDecimal salePrice, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("checkTransResult,rateChangeVo={},quoteCurrency={},salePrice={}", rateChangeVo, quoteCurrency, salePrice);
        boolean tranResult = false;
        try {
            rateChangeVo.setQuoteCurrency(quoteCurrency);
            //set hbToSale price
            rateChangeVo.setSalePrice(salePrice);
            //要购买的交易对主对
            String baseCurrency = marketRuleBiz.getHbBaseCurrency(rateChangeVo.getBuyerSymbol());
            rateChangeVo.setBaseCurrency(baseCurrency);
            //交易
            tranResult = transBiz.hbToBuy(rateChangeVo, symbolTradeConfig);
        } catch (ApiException e) {
            log.warn("buy fail.errCode={},errMsg={}", e.getErrCode(), e.getMessage());
        }
        return tranResult;
    }

    /**
     * compare with other currency growth rate
     */
    private RateChangeVo compareToOtherCurrency(String originSymbol, String otherSymbol, BigDecimal increase, SymbolTradeConfigEntity symbolTradeConfigEntity) {
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
        if (DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode().equals(symbolTradeConfigEntity.getThresholdType())) {
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
            if (new BigDecimal(Math.abs(increase.subtract(otherMinIncrease).doubleValue())).compareTo(symbolTradeConfigEntity.getCurrencyAbsolute()) > 0) {
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
            if (new BigDecimal(Math.abs(otherMinIncrease.subtract(increase).doubleValue())).compareTo(symbolTradeConfigEntity.getCurrencyAbsolute()) > 0) {
                rateChangeVo.setBuyerSymbol(originSymbol);
                rateChangeVo.setSaleSymbol(otherSymbol);
                info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, originSymbol);
                MarketDetailVo detailVo = info.getData().get(0);
                rateChangeVo.setBuyPrice(detailVo.getClose());
                rateChangeVo.setNowMarketDetailVo(detailVo);
                rateChangeVo.setRateValue(otherMinIncrease.subtract(increase));
            }
        }
        log.info("compare to other currency. otherSymbol={},increase={},nowPrice={},otherMinPrice={},otherMinIncrease={},rateChangeVo={}", otherSymbol, increase, otherNowPrice, otherMinPrice, otherMinIncrease, rateChangeVo);
        return rateChangeVo;
    }

    private void sendSms(String content, String symbol, String phones) {
        if (!needSms) {
            log.info("do not need sms...");
            return;
        }
        if (StringUtils.isNotEmpty(symbol)) {
            Sms.smsSend(content, phones);
        }
    }

    private void sendNotifyEmail(String content, String email) {
        String subject = "market info notify";
        MailQQ.sendEmail(subject, content, email);
    }


    /**
     * 相对主对相乘汇率
     *
     * @param buyPrice 买价
     * @param base1    主对1
     * @param base2    主对2
     */
    private BigDecimal getMultiplySalePrice(BigDecimal buyPrice, String base1, String base2, SymbolTradeConfigEntity symbolTradeConfigEntity) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, baseCurrencyGroup);
        return buyPrice.multiply(info.getData().get(0).getClose()).multiply(new BigDecimal(1).add(symbolTradeConfigEntity.getCurrencyAbsolute()));
    }

    /**
     * 相对主对相除汇率
     */
    private BigDecimal getDivideSalePrice(BigDecimal buyPrice, String base1, String base2, SymbolTradeConfigEntity symbolTradeConfigEntity) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, baseCurrencyGroup);
        return buyPrice.divide(info.getData().get(0).getClose(), 8, BigDecimal.ROUND_FLOOR).multiply(new BigDecimal(1).add(symbolTradeConfigEntity.getCurrencyAbsolute()));
    }


    /**
     * 两个主对组合顺序
     */
    private String twoBaseCurrencyGroup(String base1, String base2) {
        String baseSymbol = null;
        //is usdt
        if (DictEnum.HB_MARKET_BASE_USDT.getCode().equals(base1)) {
            baseSymbol = base2 + base1;
        } else if (DictEnum.HB_MARKET_BASE_USDT.getCode().equals(base2)) {
            baseSymbol = base1 + base2;
        } else if (DictEnum.HB_MARKET_BASE_BTC.getCode().equals(base1)) {
            baseSymbol = base2 + base1;
        } else if (DictEnum.HB_MARKET_BASE_BTC.getCode().equals(base2)) {
            baseSymbol = base1 + base2;
        }
        log.info("baseSymbol={}", baseSymbol);
        return baseSymbol;
    }


}
