package com.money.game.robot.biz;

import com.money.game.core.util.DateUtils;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.CreateOrderDto;
import com.money.game.robot.dto.huobi.DepthDto;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.entity.OrderEntity;
import com.money.game.robot.entity.RateChangeEntity;
import com.money.game.robot.huobi.request.CreateOrderRequest;
import com.money.game.robot.huobi.response.Accounts;
import com.money.game.robot.huobi.response.BalanceBean;
import com.money.game.robot.huobi.response.Depth;
import com.money.game.robot.huobi.response.OrdersDetail;
import com.money.game.robot.service.RateChangeService;
import com.money.game.robot.vo.huobi.RateChangeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private MarketBiz marketBiz;

    @Autowired
    private TradeBiz tradeBiz;

    @Autowired
    private AccountBiz accountBiz;

    @Autowired
    private OrderBiz orderBiz;

    @Autowired
    private RateChangeService rateChangeService;

    @Autowired
    private RateChangeBiz rateChangeBiz;

    /**
     * 主对之间允许的误差,例如eosbtc increase 10%,ethbtc increase 3%,则实际增长幅度为10%-3%+baseCurrencyBlunder > 购买阈值
     */
    @Value("${base.currency.blunder:0.01}")
    private BigDecimal baseCurrencyBlunder;

    /**
     * 卖一最大允许差异比率,例如eosbtc 准备已当前最新价购买,卖一最多比最新价高的比例不超过此值(1%),则直接以卖一价市价购买
     */
    @Value("${asks.blunder:0.01}")
    private BigDecimal asksBlunder;

    /**
     * usdt 最多下单多少amount
     */
    @Value("${max.buy.with.usdt:1}")
    private BigDecimal maxBuyWithUsdt;

    /**
     * btc 最多下单多少amount
     */
    @Value("${max.buy.with.btc:0.00125}")
    private BigDecimal maxBuyWithBtc;

    /**
     * eth 最多下单多少amount
     */
    @Value("${max.buy.with.eth:0.002}")
    private BigDecimal maxBuyWithEth;

    /**
     * 下单可浮价格
     */
    @Value("${increase.buy.price:0.00000001}")
    private BigDecimal increseBuyAndSalePrice;

    /**
     * 买单未成交最多等待多长时间,超时则撤销
     */
    @Value("${buy.order.wait.time:10}")
    private Integer buyOrderWaitTime;

    /**
     * to buy
     */
    public void buy(RateChangeVo rateChangeVo) {
        log.info("buy,rateChangeVo={}", rateChangeVo);
        List<String> orderIds;
        if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
            boolean checkResult = checkNeedToBuy(rateChangeVo);
            if (checkResult) {
                log.info("存在未完成的订单,不再继续购买,rateChangeVo={}", rateChangeVo);
                return;
            }
            //下单操作
            orderIds = checkDeptAndCreateBuyOrder(rateChangeVo.getBuyerSymbol(), rateChangeVo.getBuyPrice(), rateChangeVo.getBaseCurrency());
            RateChangeEntity rateChangeEntity = rateChangeBiz.save(rateChangeVo);
            //保存下单结果
            for (String orderId : orderIds) {
                orderBiz.saveOrder(orderId, rateChangeEntity.getOid(), null);
            }
        }
    }

    /**
     * 检查是否有需要卖出的订单
     */
    public void sale() {
        List<String> saleOrdes;
        List<OrderEntity> list = orderBiz.findNoFilledBuyOrder();
        for (OrderEntity buyOrderEntity : list) {
            //完全成交或者部分成交撤销,售出成交部分
            if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(buyOrderEntity.getState()) || DictEnum.ORDER_DETAIL_STATE_PARTIAL_CANCELED.getCode().equals(buyOrderEntity.getState())) {
                log.info("order can to sale.orderEntity={}", buyOrderEntity);
                RateChangeEntity rateChangeEntity = rateChangeService.findOne(buyOrderEntity.getRateChangeId());
                saleOrdes = checkDeptAndCreateSaleOrder(rateChangeEntity, buyOrderEntity.getFieldAmount());
                for (String orderId : saleOrdes) {
                    //保存卖单
                    orderBiz.saveOrder(orderId, rateChangeEntity.getOid(), buyOrderEntity.getOrderId());
                }
                //更新原买单状态
                buyOrderEntity.setState(DictEnum.ORDER_DETAIL_STATE_SELL.getCode());
                orderBiz.saveOrder(buyOrderEntity);
            }
            //部分成交
            else if (DictEnum.ORDER_DETAIL_STATE_PARTIAL_FILLED.getCode().equals(buyOrderEntity.getState())) {
                //买单超时则撤销
                if (DateUtils.addMinute(buyOrderEntity.getCreateTime(), buyOrderWaitTime).before(DateUtils.getCurrDateMmss())) {
                    HuobiBaseDto dto = new HuobiBaseDto();
                    dto.setOrderId(buyOrderEntity.getOrderId());
                    tradeBiz.submitCancel(dto);
                }
            }
        }
    }


    public void checkSaleFinish() {
        List<OrderEntity> saleOrderList = orderBiz.findNoFilledSaleOrder();
        for (OrderEntity saleOrder : saleOrderList) {
            HuobiBaseDto dto = new HuobiBaseDto();
            dto.setOrderId(saleOrder.getOrderId());
            OrdersDetail ordersDetail = tradeBiz.orderDetail(dto);
            //卖单已成交或撤销成交
            if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(ordersDetail.getState()) || DictEnum.ORDER_DETAIL_STATE_PARTIAL_CANCELED.getCode().equals(ordersDetail.getState())) {
                log.info("卖单已成交,交易完成.saleOrderId={}", saleOrder.getOrderId());
                saleOrder.setState(ordersDetail.getState());

            }

        }
    }


    private boolean checkNeedToBuy(RateChangeVo rateChangeVo) {
        boolean result = orderBiz.existNotFinishOrder(rateChangeVo.getBuyerSymbol(), DictEnum.ORDER_TYPE_BUY_LIMIT.getCode());
        if (!result) {
            result = orderBiz.existNotFinishOrder(rateChangeVo.getSaleSymbol(), DictEnum.ORDER_TYPE_SELL_LIMIT.getCode());
        }
        return result;

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
                    log.info("卖单价购买,buyPrice={},saleOnePrice={},remainAmount={},saleOneAmount={},orderId={}", buyPrice, salePrice, remainAmount, saleAmount, orderId);
                    remainAmount = remainAmount.subtract(saleAmount);
                } else {
                    log.info("卖单数量已满足购买需求,订单完成委托");
                    break;
                }
            } else {
                log.info("卖单价格比例已超过阈值,不再已卖单价购买,salePrice={},buyPrice={},asksBlunder={}", salePrice, buyPrice, asksBlunder);
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
    private List<String> checkDeptAndCreateSaleOrder(RateChangeEntity rateChangeEntity, BigDecimal amount) {
        log.info("checkDeptAndCreateSaleOrder,rateChangeVo={},amount={}", rateChangeEntity, amount);
        DepthDto dto = new DepthDto();
        dto.setSymbol(rateChangeEntity.getSaleSymbol());
        //获取交易深度
        Depth depth = marketBiz.depth(dto);
        //所有成交的订单id
        List<String> orderIds = new ArrayList<>();
        //剩余需要售卖的数量
        BigDecimal remainAmount = amount;
        //卖的价格
        BigDecimal salePrice = rateChangeEntity.getSalePrice();
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
                    String orderId = createSaleOrder(rateChangeEntity.getSaleSymbol(), salePrice, saleAmount, rateChangeEntity.getQuoteCurrency());
                    orderIds.add(orderId);
                    log.info("买单价售出,salePrice={},remainAmount={},saleAmount={},orderId={}", salePrice, remainAmount, saleAmount, orderId);
                    remainAmount = remainAmount.subtract(saleAmount);
                } else {
                    log.info("买单数量已满足出售需求,订单完成委托");
                    break;
                }
            } else {
                break;
            }
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
            String orderId = createSaleOrder(rateChangeEntity.getSaleSymbol(), salePrice, remainAmount, rateChangeEntity.getQuoteCurrency());
            orderIds.add(orderId);
            log.info("买单价卖出,remainAmount={},salePrice={},orderId={}", remainAmount, salePrice, orderId);
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
}
