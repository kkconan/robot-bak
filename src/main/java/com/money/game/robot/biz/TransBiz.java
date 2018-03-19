package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.CreateOrderDto;
import com.money.game.robot.dto.huobi.DepthDto;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.huobi.request.CreateOrderRequest;
import com.money.game.robot.huobi.response.Accounts;
import com.money.game.robot.huobi.response.BalanceBean;
import com.money.game.robot.huobi.response.Depth;
import com.money.game.robot.huobi.response.OrdersDetail;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.vo.huobi.MarketDetailVo;
import com.money.game.robot.vo.huobi.MarketInfoVo;
import com.money.game.robot.vo.huobi.RateChangeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author conan
 *         2018/3/15 10:27
 **/
@Slf4j
@Component
public class TransBiz {

    @Autowired
    private HuobiApi huobiApi;

    @Autowired
    private MarketBiz marketBiz;

    @Autowired
    private TradeBiz tradeBiz;

    @Autowired
    private AccountBiz accountBiz;

    /**
     * 主对之间允许的误差,例如eosbtc increase 10%,ethbtc increase 3%,则实际增长幅度为10%-3%+baseCurrencyBlunder > 购买阈值
     */
    @Value("${base.currency.blunder:0.01}")
    private BigDecimal baseCurrencyBlunder;

    /**
     * 卖一最大允许差异比率,例如eosbtc 准备已当前最新价购买,卖一最多比最新价高的比例不超过此值(1%),则直接以卖一价市价购买
     */
    @Value("${base.currency.blunder:0.01}")
    private BigDecimal asksBlunder;

    /**
     * usdt 最多下单多少amount
     */
    @Value("${max.buy.with.usdt:10}")
    private BigDecimal maxBuyWithUsdt;

    /**
     * btc 最多下单多少amount
     */
    @Value("${max.buy.with.btc:0.00125}")
    private BigDecimal maxBuyWithBtc;

    /**
     * eth 最多下单多少amount
     */
    @Value("${max.buy.with.btc:0.02}")
    private BigDecimal maxBuyWithEth;

    /**
     * 下单可浮价格
     */
    @Value("${increase.buy.price:0.00000001}")
    private BigDecimal increseBuyAndSalePrice;

    /**
     * to trans
     */
    public void trans(RateChangeVo rateChangeVo) {
        List<String> orderIds;
        List<String> saleOrdes = new ArrayList<>();
        if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
            //下单操作
            orderIds = checkDeptAndCreateBuyOrder(rateChangeVo.getBuyerSymbol(), rateChangeVo.getBuyPrice(), rateChangeVo.getBaseCurrency());
            //检查买入委托
            for (String orderId : orderIds) {
                saleOrdes.addAll(checkStateAndSale(orderId, rateChangeVo));
            }
            //检查卖出委托
            for (String saleOrderId : saleOrdes) {
                checkSaleStatus(saleOrderId);
            }
        }
    }

    private void checkSaleStatus(String orderId) {
        HuobiBaseDto dto = new HuobiBaseDto();
        dto.setOrderId(orderId);
        OrdersDetail ordersDetail = tradeBiz.orderDetail(dto);
        log.info("to sale order;ordersDetail={}", ordersDetail);
        writeToFile2("to sale order result=" + ordersDetail.toString());
        if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(ordersDetail.getState())) {
            writeToFile2("sale order result=" + ordersDetail.toString());
        }
    }

    /**
     * 下单委托成功则挂单售出
     */
    private List<String> checkStateAndSale(String orderId, RateChangeVo rateChangeVo) {
        List<String> saleOrdes = new ArrayList<>();
        HuobiBaseDto dto = new HuobiBaseDto();
        dto.setOrderId(orderId);
        OrdersDetail ordersDetail = tradeBiz.orderDetail(dto);
        //订单完成成交则售出
        if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(ordersDetail.getState())) {
            log.info("buy order success;ordersDetail={}", ordersDetail);
            saleOrdes = checkDeptAndCreateSaleOrder(rateChangeVo, rateChangeVo.getNowMarketDetailVo().getClose(), new BigDecimal(ordersDetail.getAmount()));
        }
        log.info("sale to orderIds={}", saleOrdes);
        //TODO 入库,买单超过时间为成交或者部分成交则撤销订单,售出成交部分
        writeToFile2("buy order result=" + ordersDetail.toString() + ";saleOrdes=" + saleOrdes);
        return saleOrdes;
    }

    /**
     * 检查交易深度是否满足创建买单条件
     */
    private List<String> checkDeptAndCreateBuyOrder(String symbol, BigDecimal buyPrice, String baseQuote) {
        log.info("checkDeptAndCreateBuyOrder,symbol={},buyPrice={},baseCurrency={}", symbol, buyPrice, baseQuote);
        DepthDto dto = new DepthDto();
        dto.setSymbol(symbol);
        //获取交易深度
        Depth depth = marketBiz.depth(dto);
        //最新价格
        //所有成交的订单id
        List<String> orderIds = new ArrayList<>();
        //剩余需要购买的数量
        BigDecimal remainAmount = getBuyAmount(symbol, buyPrice);
        //buy list
        List<List<BigDecimal>> bids = depth.getBids();
        //sale list
        List<List<BigDecimal>> asks = depth.getAsks();
        //判断卖单是否足够
        for (List<BigDecimal> ask : asks) {
            //卖单价格
            BigDecimal salePrice = ask.get(0);
            //(a(卖一)-b(最新成交价))/b <= c(阈值)
            if ((salePrice.subtract(buyPrice)).divide(buyPrice, 4, BigDecimal.ROUND_HALF_UP).compareTo(asksBlunder) <= 0) {
                //卖单足够
                if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
                    //卖一+上浮=购买价
                    buyPrice = salePrice.add(increseBuyAndSalePrice);
                    BigDecimal saleAmount = ask.get(1);
                    remainAmount = remainAmount.compareTo(saleAmount) > 0 ? saleAmount : remainAmount;
                    String orderId = createBuyOrder(symbol, buyPrice, remainAmount, baseQuote);
//                    String orderId = "2502535508";
                    orderIds.add(orderId);
                    log.info("卖单价购买,buyPrice={},salePrice={},remainAmount={},saleAmount={},orderId={}", buyPrice, salePrice, remainAmount, saleAmount, orderId);
                    remainAmount = remainAmount.subtract(saleAmount);
                } else {
                    log.info("卖单数量已满足购买需求,订单完成委托");
                    break;
                }
            } else {
                log.info("卖单价格比例已超过阈值,不再已卖单价购买");
                break;
            }
        }
        //未已卖单价成交所有,参考买一价购买
        if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
            //买一价
            BigDecimal buyOnePrice = bids.get(0).get(0);
            //买单价格高于买一
            if (buyPrice.compareTo(buyOnePrice) > 0) {
                buyPrice = buyOnePrice.add(increseBuyAndSalePrice);
            } else {
                buyPrice = buyPrice.add(increseBuyAndSalePrice);
            }
            String orderId = createBuyOrder(symbol, buyPrice, remainAmount, baseQuote);
//            String orderId = "2502535508";
            orderIds.add(orderId);
            log.info("买单价购买,remainAmount={},buyPrice={},nowPrice={},orderId={}", remainAmount, buyPrice, buyPrice, orderId);
        }
        log.info("orderIds={}", orderIds);
        return orderIds;
    }

    /**
     * 限价买
     */
    private String createBuyOrder(String symbol, BigDecimal price, BigDecimal amount, String baseQuote) {
        log.info("create createBuyOrder,symbol={},price={},amount={},baseCurrency={}", symbol, price, amount, baseQuote);
        BigDecimal maxAmount = getBuyAmount(symbol, price);
        CreateOrderDto dto = new CreateOrderDto();
        dto.setSymbol(symbol);
        dto.setOrderType(CreateOrderRequest.OrderType.BUY_LIMIT);

        HuobiBaseDto baseDto = new HuobiBaseDto();
        Accounts accounts = accountBiz.getSpotAccounts(baseDto);
        List<BalanceBean> balanceBeanList = accountBiz.getAccountBalance(dto, accounts);
        for (BalanceBean balanceBean : balanceBeanList) {
            //获取当前币种可交易余额
            if (baseQuote.equals(balanceBean.getCurrency()) && "trade".equals(balanceBean.getType())) {
                BigDecimal maxBalance = new BigDecimal(balanceBean.getBalance());
                //判断是否超过可使用上限
                maxAmount = maxBalance.compareTo(maxAmount) < 0 ? maxBalance : maxAmount;
                break;
            }
        }
        //判断是否超过上限
        amount = maxAmount.compareTo(amount) < 0 ? maxAmount : amount;

        dto.setAmount(amount);
        dto.setAccountId(String.valueOf(accounts.getId()));
        dto.setPrice(price);
        return tradeBiz.createOrder(dto);
    }


    /**
     * 检查交易深度是否满足创建卖单条件
     */
    private List<String> checkDeptAndCreateSaleOrder(RateChangeVo rateChangeVo, BigDecimal nowPrice, BigDecimal amount) {
        log.info("checkDeptAndCreateSaleOrder,rateChangeVo={},nowPrice={},amount={}", rateChangeVo, nowPrice, amount);
        DepthDto dto = new DepthDto();
        dto.setSymbol(rateChangeVo.getSaleSymbol());
        //获取交易深度
        Depth depth = marketBiz.depth(dto);
        //所有成交的订单id
        List<String> orderIds = new ArrayList<>();
        //剩余需要售卖的数量
        BigDecimal remainAmount = amount;
        //卖的价格
        BigDecimal salePrice = rateChangeVo.getSalePrice();
        //buy list
        List<List<BigDecimal>> bids = depth.getBids();
        //sale list
        List<List<BigDecimal>> asks = depth.getAsks();
        //判断买单是否足够
        for (List<BigDecimal> bid : bids) {
            //买单价格
            BigDecimal buyPrice = bid.get(0);

            //(a(卖一)-b(最新成交价))/b <= c(阈值)
            if ((salePrice.subtract(buyPrice)).divide(buyPrice, 4, BigDecimal.ROUND_HALF_UP).compareTo(asksBlunder) <= 0) {
                //买单足够
                if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
                    //卖一+上浮=购买价
                    salePrice = salePrice.subtract(increseBuyAndSalePrice);
                    BigDecimal saleAmount = bid.get(1);
                    String orderId = createSaleOrder(rateChangeVo.getSaleSymbol(), salePrice, saleAmount, rateChangeVo.getQuoteCurrency());
                    orderIds.add(orderId);
                    log.info("买单价售出,salePrice={},remainAmount={},saleAmount={},orderId={}", salePrice, remainAmount, saleAmount, orderId);
                    remainAmount = remainAmount.subtract(saleAmount);
                } else {
                    log.info("买单数量已满足出售需求,订单完成委托");
                    break;
                }
            }
            break;
        }
        //未已买单价成交所有,参考卖一价售出
        if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
            //卖一价
            BigDecimal saleOnePrice = asks.get(0).get(0);
            //卖价高于卖一
            if (salePrice.compareTo(saleOnePrice) > 0) {
                salePrice = salePrice.subtract(increseBuyAndSalePrice);
            } else {
                salePrice = saleOnePrice.subtract(increseBuyAndSalePrice);
            }
            String orderId = createSaleOrder(rateChangeVo.getSaleSymbol(), salePrice, remainAmount, rateChangeVo.getQuoteCurrency());
            orderIds.add(orderId);
            log.info("买单价卖出,remainAmount={},salePrice={},nowPrice={},orderId={}", remainAmount, salePrice, nowPrice, orderId);
        }
        log.info("orderIds={}", orderIds);
        return orderIds;
    }

    /**
     * 限价卖
     */
    private String createSaleOrder(String symbol, BigDecimal price, BigDecimal amount, String quoteCurrency) {
        log.info("create order,symbol={},price={},amount={},quoteCurrency={}", symbol, price, amount, quoteCurrency);
        //判断是否超过可使用的上限
        BigDecimal maxAmount = BigDecimal.ZERO;
        CreateOrderDto dto = new CreateOrderDto();
        dto.setSymbol(symbol);
        dto.setOrderType(CreateOrderRequest.OrderType.SELL_LIMIT);
        dto.setPrice(price);

        HuobiBaseDto baseDto = new HuobiBaseDto();
        Accounts accounts = accountBiz.getSpotAccounts(baseDto);
        List<BalanceBean> balanceBeanList = accountBiz.getAccountBalance(dto, accounts);
        for (BalanceBean balanceBean : balanceBeanList) {
            //获取当前币种可交易余额
            if (quoteCurrency.equals(balanceBean.getCurrency()) && "trade".equals(balanceBean.getType())) {
                maxAmount = new BigDecimal(balanceBean.getBalance());
                break;
            }
        }
        amount = maxAmount.compareTo(amount) < 0 ? maxAmount : amount;
        dto.setAccountId(String.valueOf(accounts.getId()));
        dto.setAmount(amount);
        return tradeBiz.createOrder(dto);
    }


    /**
     * eth to usdt rate validate
     *
     * @param quoteCurrency        业务对
     * @param increase             增长率
     * @param period               行情类型 1min ex.
     * @param size                 行情条数
     * @param baseCurrencyAbsolute 差异基准值
     * @return 要操作的新交易对
     */
    private RateChangeVo ethusdtRateChange(String quoteCurrency, BigDecimal increase, String period, Integer size, BigDecimal baseCurrencyAbsolute) {
        String baseQuote = DictEnum.MARKET_BASE_ETH.getCode();//主对
        MarketInfoVo info = huobiApi.getMarketInfo(period, size, DictEnum.MARKET_HUOBI_SYMBOL_ETH_USDT.getCode());
        return initRateChange(quoteCurrency, increase, period, size, baseCurrencyAbsolute, baseQuote, info);

    }


    /**
     * * btc to usdt rate validate
     * example ethusdtRateChange params
     */
    private RateChangeVo btcusdtRateChange(String quoteCurrency, BigDecimal increase, String period, Integer size, BigDecimal baseCurrencyAbsolute) {
        String baseQuote = DictEnum.MARKET_BASE_BTC.getCode();//主对
        MarketInfoVo info = huobiApi.getMarketInfo(period, size, DictEnum.MARKET_HUOBI_SYMBOL_BTC_USDT.getCode());
        return initRateChange(quoteCurrency, increase, period, size, baseCurrencyAbsolute, baseQuote, info);

    }

    private RateChangeVo initRateChange(String quoteCurrency, BigDecimal increase, String period, Integer size, BigDecimal baseCurrencyAbsolute, String baseQuote, MarketInfoVo info) {
        log.info("initRateChange start...quoteCurrency={},increase={},period={},size={},baseCurrencyAbsolute={},baseCurrency={}",
                quoteCurrency, increase, period, size, baseCurrencyAbsolute, baseQuote);
        RateChangeVo vo = new RateChangeVo();
        String otherBaseQuote = DictEnum.MARKET_BASE_BTC.getCode();
        if (DictEnum.MARKET_BASE_BTC.getCode().equals(baseQuote)) {
            otherBaseQuote = DictEnum.MARKET_BASE_ETH.getCode();
        }
        String buySymbol = null;
        String saleSymbol = null;
        MarketDetailVo nowVo = info.getData().get(0);
        BigDecimal nowPrice = nowVo.getClose();
        BigDecimal otherMinPrice;
        BigDecimal otherMinIncrease;
        BigDecimal rateValue;//可操作的最大差异比率
        if (DictEnum.MARKET_PERIOD_1MIN.getCode().equals(period)) {
            MarketDetailVo oneMinVo = info.getData().get(1);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (nowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        } else {
            MarketDetailVo oneMinVo = info.getData().get(5);
            otherMinPrice = oneMinVo.getClose();
            otherMinIncrease = (nowPrice.subtract(otherMinPrice)).divide(otherMinPrice, 9, BigDecimal.ROUND_HALF_UP);
        }
        // original symbol increase; example eoseth 10%
        if (increase.compareTo(BigDecimal.ZERO) >= 0) {
            //相对主对之间增幅超过阈值baseCurrencyAbsolute,example  10%-3%+baseCurrencyBlunder > 购买阈值，则购买另一主对交易对
            rateValue = increase.subtract(otherMinIncrease);
            if (rateValue.add(baseCurrencyBlunder).compareTo(baseCurrencyAbsolute) > 0) {
                buySymbol = quoteCurrency + otherBaseQuote;
                saleSymbol = quoteCurrency + baseQuote;
            }
        }
        //original symbol decrease; example eosbtc -10%
        else {
            //相对主对之间增幅超过阈值baseCurrencyAbsolute,example  -3%-(-10%)+baseCurrencyBlunder > 购买阈值，则购买当前交易对
            rateValue = otherMinIncrease.subtract(increase);
            if (rateValue.add(baseCurrencyBlunder).compareTo(baseCurrencyAbsolute) > 0) {
                buySymbol = quoteCurrency + baseQuote;
                saleSymbol = quoteCurrency + otherBaseQuote;
            }
        }
        //波动交易对的最新行情信息
        MarketInfoVo marketInfoVo = huobiApi.getMarketInfo(period, size, quoteCurrency + baseQuote);
        vo.setNowMarketDetailVo(marketInfoVo.getData().get(0));
        vo.setBuyerSymbol(buySymbol);
        vo.setRateValue(rateValue);
        vo.setBaseCurrency(baseQuote);
        vo.setQuoteCurrency(quoteCurrency);
        vo.setSaleSymbol(saleSymbol);
        log.info("ethbtcRateChange end...otherMinPrice={},otherMinIncrease={},increase={},baseCurrencyBlunder={},buySymbol={},vo={}",
                otherMinPrice, otherMinIncrease, increase, baseCurrencyBlunder, buySymbol, vo);
        return vo;

    }

    /**
     * 购买数量
     * newPrice 最新价格
     */
    private BigDecimal getBuyAmount(String symbol, BigDecimal newPrice) {
        BigDecimal amount;
        if (symbol.endsWith(DictEnum.MARKET_BASE_USDT.getCode())) {
            amount = maxBuyWithUsdt.divide(newPrice, 4, BigDecimal.ROUND_FLOOR);
        } else if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode())) {
            amount = maxBuyWithBtc.divide(newPrice, 8, BigDecimal.ROUND_FLOOR);
        } else {
            amount = maxBuyWithEth.divide(newPrice, 8, BigDecimal.ROUND_FLOOR);
        }
        return amount;
    }

    public static void writeToFile2(String content) {
        try {
            File file = new File("E:\\tmp\\trade.txt");
            //文件不存在时候，主动穿件文件。
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.newLine();
            bw.write(content);
            bw.close();
            fw.close();
            log.info("content={} write done!", content);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
