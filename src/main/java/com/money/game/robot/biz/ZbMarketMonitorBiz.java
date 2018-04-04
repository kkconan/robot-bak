package com.money.game.robot.biz;

import com.money.game.core.util.StrRedisUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.entity.AccountEntity;
import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.huobi.api.ApiException;
import com.money.game.robot.vo.huobi.RateChangeVo;
import com.money.game.robot.zb.api.ZbApi;
import com.money.game.robot.zb.vo.ZbKineDetailVo;
import com.money.game.robot.zb.vo.ZbKineVo;
import com.money.game.robot.zb.vo.ZbSymbolInfoVo;
import com.money.game.robot.zb.vo.ZbTickerVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 中币行情监控
 *
 * @author conan
 *         2018/3/8 15:58
 **/
@Component
@Slf4j
public class ZbMarketMonitorBiz {


    @Value("${need.sms:false}")
    private boolean needSms;

    @Autowired
    private ZbApi zbApi;
    @Autowired
    private TransBiz transBiz;

    @Autowired
    private UserBiz userBiz;

    @Autowired
    private AccountBiz accountBiz;

    @Autowired
    private SymbolTradeConfigBiz symbolTradeConfigBiz;

    @Autowired
    private MarketRuleBiz marketRuleBiz;

    @Autowired
    private RedisTemplate<String, String> redis;


    @Async("zbMarketMonitor")
    public void asyncDoMonitor(List<ZbSymbolInfoVo> list) {
        for (ZbSymbolInfoVo zbSymbolInfoVo : list) {
            zbMonitor(zbSymbolInfoVo.getCurrency());
        }
    }

    public void zbMonitor(String symbol) {
        ZbKineVo info = zbApi.getKline(symbol, DictEnum.MARKET_PERIOD_1MIN.getCode(), 6);
        if (info.getData() != null && info.getData().size() > 0) {
            List<ZbKineDetailVo> kineDetailVoList = info.getData();
            // 倒序排列,zb最新数据在最后面
            Collections.reverse(kineDetailVoList);
            List<UserEntity> userList = userBiz.findAllByNormal();

            ZbKineDetailVo nowVo = kineDetailVoList.get(0);
            for (UserEntity user : userList) {
                AccountEntity account = accountBiz.getByUserIdAndType(user.getOid(), DictEnum.MARKET_TYPE_ZB.getCode());
                if (account != null && StringUtils.isNotEmpty(account.getApiKey())) {
                    // 1min monitor
                    oneMinMonitor(symbol, nowVo, kineDetailVoList, user);
                    // 5min monitor
                    fiveMinMonitor(symbol, nowVo, kineDetailVoList, user);
                }
            }
        }

    }

    /**
     * 初始化zb各交易对小数位
     */
    public void initScaleToRedis() {
        List<ZbSymbolInfoVo> list = zbApi.getSymbolInfo();
        for (ZbSymbolInfoVo vo : list) {
            StrRedisUtil.set(redis, DictEnum.ZB_CURRENCY_KEY_PRICE.getCode() + vo.getCurrency(), vo.getPriceScale());
            StrRedisUtil.set(redis, DictEnum.ZB_CURRENCY__KEY_AMOUNT.getCode() + vo.getCurrency(), vo.getAmountScale());

        }
    }

    private void oneMinMonitor(String symbol, ZbKineDetailVo nowVo, List<ZbKineDetailVo> detailVos, UserEntity user) {
        ZbKineDetailVo lastMinVo = detailVos.get(1);
        //查询用户一分钟zb交易配置
        SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findByUserIdAndThresholdTypeAndMarketType(user.getOid(), DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode(), DictEnum.MARKET_TYPE_ZB.getCode());
        if (symbolTradeConfig != null) {
            checkMinMoitor(symbol, nowVo, lastMinVo, user, symbolTradeConfig);
        }

    }

    private void fiveMinMonitor(String symbol, ZbKineDetailVo nowVo, List<ZbKineDetailVo> detailVos, UserEntity user) {
        ZbKineDetailVo lastMinVo = detailVos.get(5);
        //查询用户五分钟交易配置
        SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findByUserIdAndThresholdTypeAndMarketType(user.getOid(), DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_FIVE_MIN.getCode(), DictEnum.MARKET_TYPE_ZB.getCode());
        if (symbolTradeConfig != null) {
            checkMinMoitor(symbol, nowVo, lastMinVo, user, symbolTradeConfig);
        }
    }

    private void checkMinMoitor(String symbol, ZbKineDetailVo nowVo, ZbKineDetailVo lastMinVo, UserEntity user, SymbolTradeConfigEntity symbolTradeConfig) {
        RateChangeVo rateChangeVo = marketRuleBiz.initMonitor(symbol, nowVo.getClose(), lastMinVo.getClose(), symbolTradeConfig, user);
        if (rateChangeVo.isOperate()) {
            //check buy
            boolean transResult = checkToZbTrans(symbol, nowVo.getClose(), rateChangeVo.getRateValue(), symbolTradeConfig);
            //trans success to send sms
            if (transResult) {
                marketRuleBiz.sendSms(rateChangeVo.getContext(), symbol, user.getNotifyPhone());
            }
        }
    }

    /**
     * 检查是否可交易
     */
    private boolean checkToZbTrans(String symbol, BigDecimal nowPrice, BigDecimal increase, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("checkToZbTrans,symbol={},increase={}", symbol, increase);
        RateChangeVo rateChangeVo;
        BigDecimal salePrice;
        //应用对
        String quoteCurrency = marketRuleBiz.getZbQuoteCurrency(symbol);

        boolean tranResult = false;

        String otherSymbol;
        //is btc
        if (symbol.endsWith(DictEnum.ZB_MARKET_BASE_BTC.getCode())) {
            //compare to qc
            otherSymbol = zbGroupCurrency(quoteCurrency, DictEnum.ZB_MARKET_BASE_QC.getCode());
            rateChangeVo = zbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), DictEnum.ZB_MARKET_BASE_QC.getCode(), rateChangeVo.getRateValue());
                }
                //原对下降
                else {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), DictEnum.ZB_MARKET_BASE_QC.getCode(), rateChangeVo.getRateValue());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = zbGroupCurrency(quoteCurrency, DictEnum.ZB_MARKET_BASE_USDT.getCode());
                rateChangeVo = zbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
                }
            }
        }
        //is QC
        else if (symbol.endsWith(DictEnum.ZB_MARKET_BASE_QC.getCode())) {
            //compare to btc
            otherSymbol = zbGroupCurrency(quoteCurrency, DictEnum.ZB_MARKET_BASE_BTC.getCode());
            rateChangeVo = zbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_QC.getCode(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_QC.getCode(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to usdt
            if (!tranResult) {
                otherSymbol = zbGroupCurrency(quoteCurrency, DictEnum.HB_MARKET_BASE_USDT.getCode());
                rateChangeVo = zbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_QC.getCode(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
                    }
                    //原对下降
                    else {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_QC.getCode(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), rateChangeVo.getRateValue());
                    }
                    //验证是否成功创建订单
                    checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
                }
            }
        }
        //is usdt
        else {
            //compare to btc
            otherSymbol = zbGroupCurrency(quoteCurrency, DictEnum.ZB_MARKET_BASE_BTC.getCode());
            rateChangeVo = zbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
            if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                //原对增长
                if (increase.compareTo(BigDecimal.ZERO) > 0) {
                    salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //原对下降
                else {
                    salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), DictEnum.ZB_MARKET_BASE_BTC.getCode(), rateChangeVo.getRateValue());
                }
                //验证是否成功创建订单
                tranResult = checkTransResult(rateChangeVo, quoteCurrency, salePrice, symbolTradeConfig);
            }
            //compare to qc
            if (!tranResult) {
                otherSymbol = zbGroupCurrency(quoteCurrency, DictEnum.ZB_MARKET_BASE_QC.getCode());
                rateChangeVo = zbCompareToOtherCurrency(symbol, nowPrice, otherSymbol, increase, symbolTradeConfig);
                if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
                    //原对增长
                    if (increase.compareTo(BigDecimal.ZERO) > 0) {
                        salePrice = getDivideSalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), DictEnum.ZB_MARKET_BASE_QC.getCode(), rateChangeVo.getRateValue());
                    }
                    //原对下降
                    else {
                        salePrice = getMultiplySalePrice(rateChangeVo.getBuyPrice(), DictEnum.ZB_MARKET_BASE_USDT.getCode(), DictEnum.ZB_MARKET_BASE_QC.getCode(), rateChangeVo.getRateValue());
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
            String baseCurrency = marketRuleBiz.getZbBaseCurrency(rateChangeVo.getBuyerSymbol());
            rateChangeVo.setBaseCurrency(baseCurrency);
            //交易
            tranResult = transBiz.zbToBuy(rateChangeVo, symbolTradeConfig);
        } catch (ApiException e) {
            log.warn("buy fail.errCode={},errMsg={}", e.getErrCode(), e.getMessage());
        }
        return tranResult;
    }


    /**
     * ZB 不同交易对之间涨跌幅比较
     */
    private RateChangeVo zbCompareToOtherCurrency(String originSymbol, BigDecimal nowPrice, String otherSymbol, BigDecimal increase, SymbolTradeConfigEntity symbolTradeConfigEntity) {
        RateChangeVo rateChangeVo = new RateChangeVo();
        ZbKineVo info = zbApi.getKline(otherSymbol, DictEnum.MARKET_PERIOD_1MIN.getCode(), 6);
        if (info == null) {
            log.info(otherSymbol + " currency not found.");
            return rateChangeVo;
        }
        log.info("marketInfo={}", info);
        ZbKineDetailVo zbKineDetailVo = info.getData().get(0);
        BigDecimal otherNowPrice = zbKineDetailVo.getClose();
        BigDecimal otherMinPrice;
        BigDecimal otherMinIncrease;
        if (DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode().equals(symbolTradeConfigEntity.getThresholdType())) {
            ZbKineDetailVo oneMinVo = info.getData().get(1);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (otherNowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        } else {
            ZbKineDetailVo oneMinVo = info.getData().get(5);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (otherNowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        }
        rateChangeVo = marketRuleBiz.getRateChangeVo(originSymbol, nowPrice, otherSymbol, increase, symbolTradeConfigEntity, otherNowPrice, otherMinPrice);
        rateChangeVo.setMarketType(DictEnum.MARKET_TYPE_ZB.getCode());
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
        ZbTickerVo info = zbApi.getTicker(baseCurrencyGroup);
        return marketRuleBiz.getMultiplySalePrice(buyPrice, info.getLast(), rateValue);
    }

    /**
     * 相对主对相除汇率
     */
    private BigDecimal getDivideSalePrice(BigDecimal buyPrice, String base1, String base2, BigDecimal rateValue) {
        String baseCurrencyGroup = twoBaseCurrencyGroup(base1, base2);
        ZbTickerVo info = zbApi.getTicker(baseCurrencyGroup);
        return marketRuleBiz.getDivideSalePrice(buyPrice, info.getLast(), rateValue);
    }


    /**
     * 两个主对组合顺序
     */
    private String twoBaseCurrencyGroup(String base1, String base2) {
        String baseSymbol = null;
        //is qc
        if (DictEnum.ZB_MARKET_BASE_QC.getCode().equals(base1)) {
            baseSymbol = base2 + "_" + base1;
        } else if (DictEnum.ZB_MARKET_BASE_QC.getCode().equals(base2)) {
            baseSymbol = base1 + "_" + base2;
        } else if (DictEnum.ZB_MARKET_BASE_USDT.getCode().equals(base1)) {
            baseSymbol = base2 + "_" + base1;
        } else if (DictEnum.ZB_MARKET_BASE_USDT.getCode().equals(base2)) {
            baseSymbol = base1 + "_" + base2;
        }
        log.info("baseSymbol={}", baseSymbol);
        return baseSymbol;
    }


    private String zbGroupCurrency(String quote, String base) {
        return quote + "_" + base;
    }
}
