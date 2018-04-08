package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.huobi.api.ApiException;
import com.money.game.robot.market.HuobiApi;
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
 * hb行情监控
 *
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

    public void huoBiMonitor(String symbol) {
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, symbol);
        if (info != null && info.getData().size() > 0) {
            List<UserEntity> userList = userBiz.findAllByNormal();
            for (UserEntity user : userList) {
                MarketDetailVo nowVo = info.getData().get(0);
                // 1min monitor
                oneMinMonitor(symbol, nowVo, info.getData(), user);
                // 5min monitor
//            fiveMinMonitor(symbol, nowVo, info.getData(), userList);
            }
        }

    }

    private void oneMinMonitor(String symbol, MarketDetailVo nowVo, List<MarketDetailVo> detailVos, UserEntity user) {
        MarketDetailVo lastMinVo = detailVos.get(1);
        //查询用户一分钟hb交易配置
        SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findByUserIdAndThresholdTypeAndMarketType(user.getOid(), DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode(), DictEnum.MARKET_TYPE_HB.getCode());
        if (symbolTradeConfig != null) {
            checkMinMoitor(symbol, nowVo, lastMinVo, user, symbolTradeConfig);
        }

    }

    private void fiveMinMonitor(String symbol, MarketDetailVo nowVo, List<MarketDetailVo> detailVos, UserEntity user) {
        MarketDetailVo lastMinVo = detailVos.get(1);
        //查询用户五分钟交易配置
        SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findByUserIdAndThresholdTypeAndMarketType(user.getOid(), DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_FIVE_MIN.getCode(), DictEnum.MARKET_TYPE_HB.getCode());
        if (symbolTradeConfig != null) {
            checkMinMoitor(symbol, nowVo, lastMinVo, user, symbolTradeConfig);
        }
    }

    private void checkMinMoitor(String symbol, MarketDetailVo nowVo, MarketDetailVo lastMinVo, UserEntity user, SymbolTradeConfigEntity symbolTradeConfig) {
        RateChangeVo rateChangeVo = marketRuleBiz.initMonitor(symbol, nowVo.getClose(), lastMinVo.getClose(), symbolTradeConfig, user);
        if (rateChangeVo.isOperate()) {
            //check buy
            boolean transResult = checkToHbTrans(symbol, nowVo.getClose(), rateChangeVo.getRateValue(), symbolTradeConfig);
            //trans success to send sms
            if (transResult) {
                marketRuleBiz.sendSms(rateChangeVo.getContext(), symbol, user.getNotifyPhone());
            }
        }
    }

    /**
     * 检查是否可交易
     */
    private boolean checkToHbTrans(String symbol, BigDecimal nowPrice, BigDecimal increase, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("checkToHbTrans,symbol={},increase={}", symbol, increase);
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
            rateChangeVo = hbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), rateChangeVo.getRateValue());
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), rateChangeVo.getRateValue());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_USDT.getCode();
                rateChangeVo = hbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_BTC.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
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
            rateChangeVo = hbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //原对下降
                else {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_USDT.getCode();
                rateChangeVo = hbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_ETH.getCode(), DictEnum.HB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
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
            rateChangeVo = hbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to eth
            if (!tranResult) {
                otherSymbol = quoteCurrency + DictEnum.HB_MARKET_BASE_ETH.getCode();
                rateChangeVo = hbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), rateChangeVo.getRateValue());
                    }
                    //原对下降
                    else {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.HB_MARKET_BASE_USDT.getCode(), DictEnum.HB_MARKET_BASE_ETH.getCode(), rateChangeVo.getRateValue());
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
            //set sale price
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
     * HB 不同交易对之间涨跌幅比较
     */
    private RateChangeVo hbCompareToOtherCurrency(String originSymbol, BigDecimal nowPrice, String otherSymbol, BigDecimal increase, SymbolTradeConfigEntity symbolTradeConfigEntity) {
        RateChangeVo rateChangeVo = new RateChangeVo();
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 6, otherSymbol);
        if (info == null) {
            log.info(otherSymbol + " currency not found.");
            return rateChangeVo;
        }
        log.info("marketInfo={}", info);
        MarketDetailVo marketDetailVo = info.getData().get(0);
        BigDecimal otherNowPrice = marketDetailVo.getClose();
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
        rateChangeVo = marketRuleBiz.getRateChangeVo(originSymbol, nowPrice, otherSymbol, increase, symbolTradeConfigEntity, otherNowPrice, otherMinPrice);
        rateChangeVo.setMarketType(DictEnum.MARKET_TYPE_HB.getCode());
        log.info("compare to other currency. otherSymbol={},increase={},nowPrice={},otherMinPrice={},otherMinIncrease={},rateChangeVo={}", otherSymbol, increase, otherNowPrice, otherMinPrice, otherMinIncrease, rateChangeVo);
        return rateChangeVo;
    }


    /**
     * 相对主对相乘汇率
     *
     * @param buyPrice 买价
     * @param base1    主对1
     * @param base2    主对2
     */
    private BigDecimal getMultiplySalePrice(BigDecimal buyPrice, String base1, String base2, BigDecimal rateValue) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, baseCurrencyGroup);
        return marketRuleBiz.getMultiplySalePrice(buyPrice, info.getData().get(0).getClose(), rateValue);
    }

    /**
     * 相对主对相除汇率
     */
    private BigDecimal getDivideSalePrice(BigDecimal buyPrice, String base1, String base2, BigDecimal rateValue) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        MarketInfoVo info = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, baseCurrencyGroup);
        return marketRuleBiz.getDivideSalePrice(buyPrice, info.getData().get(0).getClose(), rateValue);
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
