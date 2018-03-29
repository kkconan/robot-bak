package com.money.game.robot.biz;

import com.money.game.core.util.DateUtils;
import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.CreateOrderDto;
import com.money.game.robot.dto.huobi.DepthDto;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.entity.*;
import com.money.game.robot.huobi.request.CreateOrderRequest;
import com.money.game.robot.huobi.response.Accounts;
import com.money.game.robot.huobi.response.Depth;
import com.money.game.robot.huobi.response.OrdersDetail;
import com.money.game.robot.mail.MailQQ;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.service.RateChangeService;
import com.money.game.robot.vo.huobi.MarketDetailVo;
import com.money.game.robot.vo.huobi.RateChangeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
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

    @Autowired
    private SymbolTradeConfigBiz symbolTradeConfigBiz;

    @Autowired
    private HuobiApi huobiApi;

    @Autowired
    private UserBiz userBiz;

    @Autowired
    private LimitTradeConfgBiz limitTradeConfgBiz;

    /**
     * to buy
     */
    public void buy(RateChangeVo rateChangeVo, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("buy,rateChangeVo={}", rateChangeVo);
        List<String> orderIds;
        if (StringUtils.isNotEmpty(rateChangeVo.getBuyerSymbol())) {
            boolean checkResult = checkNeedToBuy(rateChangeVo);
            if (checkResult) {
                log.info("存在未完成的订单,不再继续购买,rateChangeVo={}", rateChangeVo);
                return;
            }
            //下单操作
            orderIds = checkDeptAndCreateBuyOrder(rateChangeVo.getBuyerSymbol(), rateChangeVo.getBuyPrice(), rateChangeVo.getBaseCurrency(), symbolTradeConfig);
            RateChangeEntity rateChangeEntity = rateChangeBiz.save(rateChangeVo);
            //保存下单结果
            for (String orderId : orderIds) {
                orderBiz.saveOrder(orderId, rateChangeEntity.getOid(), null, symbolTradeConfig.getOid(), symbolTradeConfig.getUserId(), DictEnum.ORDER_MODEL_REAL.getCode());
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
            buyOrderEntity = orderBiz.updateOrderState(buyOrderEntity);

            SymbolTradeConfigEntity symbolTradeConfig = symbolTradeConfigBiz.findById(buyOrderEntity.getSymbolTradeConfigId());
            //完全成交或者部分成交撤销,售出成交部分
            if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(buyOrderEntity.getState()) || DictEnum.ORDER_DETAIL_STATE_PARTIAL_CANCELED.getCode().equals(buyOrderEntity.getState())) {
                log.info("买单已成交,可以挂单售出.orderEntity={}", buyOrderEntity);
                RateChangeEntity rateChangeEntity = rateChangeService.findOne(buyOrderEntity.getRateChangeId());
                saleOrdes = checkDeptAndCreateSaleOrder(rateChangeEntity, buyOrderEntity.getFieldAmount(), symbolTradeConfig);
                for (String orderId : saleOrdes) {
                    //保存卖单
                    orderBiz.saveOrder(orderId, rateChangeEntity.getOid(), buyOrderEntity.getOrderId(), symbolTradeConfig.getOid(), symbolTradeConfig.getUserId(), DictEnum.ORDER_MODEL_REAL.getCode());
                }
                //更新原买单状态
                buyOrderEntity.setState(DictEnum.ORDER_DETAIL_STATE_SELL.getCode());
                orderBiz.saveOrder(buyOrderEntity);
                //发送成交邮件通知
                transToEmailNotify(buyOrderEntity);
            }
            //部分成交
            else if (DictEnum.ORDER_DETAIL_STATE_PARTIAL_FILLED.getCode().equals(buyOrderEntity.getState()) || DictEnum.ORDER_DETAIL_STATE_SUBMITTED.getCode().equals(buyOrderEntity.getState())) {
                //买单超时则撤销
                Integer buyOrderWaitTime = (symbolTradeConfig != null && symbolTradeConfig.getBuyWaitTime() != null) ? symbolTradeConfig.getBuyWaitTime() : 10;
                if (DateUtils.addMinute(buyOrderEntity.getCreateTime(), buyOrderWaitTime).before(DateUtils.getCurrDateMmss())) {
                    log.info("买单超时撤销,buyOrderEntity={}", buyOrderEntity);
                    HuobiBaseDto dto = new HuobiBaseDto();
                    dto.setOrderId(buyOrderEntity.getOrderId());
                    dto.setUserId(buyOrderEntity.getUserId());
                    tradeBiz.submitCancel(dto);
                    orderBiz.updateOrderState(buyOrderEntity);
                }
            }
        }
    }


    public void checkSaleFinish() {
        List<OrderEntity> saleOrderList = orderBiz.findNoFilledSaleOrder();
        for (OrderEntity saleOrder : saleOrderList) {
            HuobiBaseDto dto = new HuobiBaseDto();
            dto.setOrderId(saleOrder.getOrderId());
            dto.setUserId(saleOrder.getUserId());
            OrdersDetail ordersDetail = tradeBiz.orderDetail(dto);
            //卖单已成交或撤销成交
            if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(ordersDetail.getState()) || DictEnum.ORDER_DETAIL_STATE_PARTIAL_CANCELED.getCode().equals(ordersDetail.getState())) {
                log.info("卖单已成交,交易完成.saleOrderId={}", saleOrder.getOrderId());
                saleOrder.setState(ordersDetail.getState());
                orderBiz.saveOrder(saleOrder);
                //发送成交邮件通知
                transToEmailNotify(saleOrder);

            }

        }
    }

    public void transModelLimitOrder() {

        List<UserEntity> userList = userBiz.findAllByNormal();
        for (UserEntity userEntity : userList) {
            List<LimitTradeConfigEntity> configList = limitTradeConfgBiz.findAllByUserId(userEntity.getOid());
            for (LimitTradeConfigEntity config : configList) {
                createModelLimitOrder(config, userEntity);
            }
        }

    }

    private void createModelLimitOrder(LimitTradeConfigEntity config, UserEntity userEntity) {
        //买单状态更新
        List<OrderEntity> buyList = orderBiz.findByUserIdAndModel(userEntity.getOid(), DictEnum.ORDER_MODEL_LIMIT.getCode(), DictEnum.ORDER_TYPE_BUY_LIMIT.getCode(), config.getSymbol(), config.getOid());
        Iterator<OrderEntity> buyIt = buyList.iterator();
        while (buyIt.hasNext()) {
            OrderEntity buyOrder = buyIt.next();
            buyOrder = orderBiz.updateOrderState(buyOrder);
            //买单已完成
            if (DictEnum.filledOrderStates.contains(buyOrder.getState())) {
                log.info("买单已完结,buyOrder={}", buyOrder);
                buyIt.remove();
                if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(buyOrder.getState())) {
                    transToEmailNotify(buyOrder);
                }
            }
        }
        //卖单状态更新
        List<OrderEntity> saleList = orderBiz.findByUserIdAndModel(userEntity.getOid(), DictEnum.ORDER_MODEL_LIMIT.getCode(), DictEnum.ORDER_TYPE_SELL_LIMIT.getCode(), config.getSymbol(), config.getOid());
        Iterator<OrderEntity> saleIt = saleList.iterator();
        while (saleIt.hasNext()) {
            OrderEntity saleOrder = saleIt.next();
            saleOrder = orderBiz.updateOrderState(saleOrder);
            //卖单已完成
            if (DictEnum.filledOrderStates.contains(saleOrder.getState())) {
                log.info("卖单已完结,saleOrder={}", saleOrder);
                saleIt.remove();
                if (DictEnum.ORDER_DETAIL_STATE_FILLED.getCode().equals(saleOrder.getState())) {
                    transToEmailNotify(saleOrder);
                }
            }
        }
        //买单大于最大数量 || 买单不为空且买单等于卖单 ||买单有成交,卖单未成交
        if (buyList.size() >= config.getMaxTradeCount() || (!buyList.isEmpty() && buyList.size() == buyList.size()) || (buyList.size() == (config.getMaxTradeCount() - 1) && saleList.size() > buyList.size())) {
            log.info("挂单已满,symbol={},userId={},configId={}", config.getSymbol(), userEntity.getOid(), config.getOid());
            return;
        } else if (buyList.isEmpty() && saleList.size() >= config.getMaxTradeCount()) {
            log.info("买单成交,卖单未成交,symbol={},userId={}", config.getSymbol(), userEntity.getOid());
            return;
        }
        MarketDetailVo marketDetailVo = huobiApi.getOneMarketDetail(config.getSymbol());
        if (marketDetailVo == null) {
            log.info("获取行情失败");
            return;
        }
        log.info("创建限价单开始,symbols={},userId={}", config.getSymbol(), userEntity.getOid());

        BigDecimal buyPrice = (new BigDecimal(1).subtract(config.getDecrease())).multiply(marketDetailVo.getClose());

        String baseCurrency = getBaseCurrency(config.getSymbol());
        //基对余额
        BigDecimal balanceMax = accountBiz.getQuoteBalance(userEntity.getOid(), baseCurrency);
        //总成交额
        BigDecimal totalAmount = config.getTotalAmount().compareTo(balanceMax) <= 0 ? config.getTotalAmount() : balanceMax;
        //购买数量
        BigDecimal amount = totalAmount.divide(buyPrice, 4, BigDecimal.ROUND_DOWN);
        BigDecimal salePrice = (new BigDecimal(1).add(config.getIncrease())).multiply(marketDetailVo.getClose());
        CreateOrderDto buyOrderDto = new CreateOrderDto();
        buyOrderDto.setSymbol(config.getSymbol());
        buyOrderDto.setPrice(buyPrice);
        buyOrderDto.setOrderType(DictEnum.ORDER_TYPE_BUY_LIMIT.getCode());
        buyOrderDto.setAccountId(userEntity.getAccountId());
        buyOrderDto.setUserId(userEntity.getOid());

        buyOrderDto.setAmount(amount);
        //创建限价买单
        String buyOrderId = tradeBiz.createOrder(buyOrderDto);
        orderBiz.saveOrder(buyOrderId, null, null, config.getOid(), userEntity.getOid(), DictEnum.ORDER_MODEL_LIMIT.getCode());
        CreateOrderDto saleOrderDto = new CreateOrderDto();
        BeanUtils.copyProperties(buyOrderDto, saleOrderDto);
        saleOrderDto.setOrderType(DictEnum.ORDER_TYPE_SELL_LIMIT.getCode());
        saleOrderDto.setPrice(salePrice);
        saleOrderDto.setUserId(userEntity.getOid());
        String quoteCurrency = getQuoteCurrency(config.getSymbol());
        //业务对余额
        balanceMax = accountBiz.getQuoteBalance(userEntity.getOid(), quoteCurrency);
        //验证余额
        amount = amount.compareTo(balanceMax) <= 0 ? amount : balanceMax;
        saleOrderDto.setAmount(amount);
        //创建限价卖单
        String saleOrderId = tradeBiz.createOrder(saleOrderDto);
        orderBiz.saveOrder(saleOrderId, null, buyOrderId, config.getOid(), userEntity.getOid(), DictEnum.ORDER_MODEL_LIMIT.getCode());
        log.info("创建限价单结束,symbols={},userId={},buyOrderId={},saleOrderId={}", config.getSymbol(), userEntity.getOid(), buyOrderId, saleOrderId);
    }


    /**
     * 限价单交易成功邮件通知
     */
    private void transToEmailNotify(OrderEntity orderEntity) {
        UserEntity userEntity = userBiz.findById(orderEntity.getUserId());
        if (StringUtil.isEmpty(userEntity.getNotifyEmail())) {
            log.info("email address is empty...");
            return;
        }
        String subject = orderEntity.getSymbol() + " " + orderEntity.getType() + " success notify";
        String content = orderEntity.getSymbol() + " " + orderEntity.getType() + " success. price is " + orderEntity.getPrice().setScale(8, BigDecimal.ROUND_DOWN)
                + ",amount is " + orderEntity.getAmount().setScale(8, BigDecimal.ROUND_DOWN) + " and totalToUsdt is " + orderEntity.getTotalToUsdt() + ".";
        MailQQ.sendEmail(subject, content, userEntity.getNotifyEmail());
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
    private List<String> checkDeptAndCreateBuyOrder(String symbol, BigDecimal buyPrice, String baseQuote, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("checkDeptAndCreateBuyOrder,symbols={},buyPrice={},baseCurrency={}", symbol, buyPrice, baseQuote);
        DepthDto dto = new DepthDto();
        dto.setSymbol(symbol);
        dto.setUserId(symbolTradeConfig.getUserId());
        //获取交易深度
        Depth depth = marketBiz.depth(dto);
        //最新价格
        //所有成交的订单id
        List<String> orderIds = new ArrayList<>();
        //剩余需要购买的数量
        BigDecimal remainAmount = getBuyAmount(symbol, buyPrice, symbolTradeConfig, baseQuote);
        //buy list
        List<List<BigDecimal>> bids = depth.getBids();
        //sale list
        List<List<BigDecimal>> asks = depth.getAsks();
        //判断卖单是否足够
        for (List<BigDecimal> ask : asks) {
            //卖单价格
            BigDecimal salePrice = ask.get(0);
            //(a(卖一)-b(最新成交价))/b <= c(阈值)
            if ((salePrice.subtract(buyPrice)).divide(buyPrice, 4, BigDecimal.ROUND_HALF_UP).compareTo(symbolTradeConfig.getAsksBlunder()) <= 0) {
                //卖单足够
                if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
                    //卖一+上浮=购买价
                    buyPrice = salePrice.add(symbolTradeConfig.getBuyIncreasePrice());
                    BigDecimal saleAmount = ask.get(1);
                    remainAmount = remainAmount.compareTo(saleAmount) > 0 ? saleAmount : remainAmount;
                    String orderId = createBuyOrder(symbol, buyPrice, remainAmount, baseQuote, symbolTradeConfig);
                    orderIds.add(orderId);
                    log.info("卖单价购买,buyPrice={},saleOnePrice={},remainAmount={},saleOneAmount={},orderId={}", buyPrice, salePrice, remainAmount, saleAmount, orderId);
                    remainAmount = remainAmount.subtract(saleAmount);
                } else {
                    log.info("卖单数量已满足购买需求,订单完成委托");
                    break;
                }
            } else {
                log.info("卖单价格比例已超过阈值,不再已卖单价购买,salePrice={},buyPrice={},asksBlunder={}", salePrice, buyPrice, symbolTradeConfig.getBuyIncreasePrice());
                break;
            }
        }
        //未已卖单价成交所有,参考买一价购买
        if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
            //买一价
            BigDecimal buyOnePrice = bids.get(0).get(0);
            //买单价格高于买一
            if (buyPrice.compareTo(buyOnePrice) > 0) {
                buyPrice = buyOnePrice.add(symbolTradeConfig.getBuyIncreasePrice());
            } else {
                buyPrice = buyPrice.add(symbolTradeConfig.getBuyIncreasePrice());
            }
            String orderId = createBuyOrder(symbol, buyPrice, remainAmount, baseQuote, symbolTradeConfig);
            orderIds.add(orderId);
            log.info("买单价购买,remainAmount={},buyPrice={},nowPrice={},orderId={}", remainAmount, buyPrice, buyPrice, orderId);
        }
        log.info("orderIds={}", orderIds);
        return orderIds;
    }

    /**
     * 限价买
     */
    private String createBuyOrder(String symbol, BigDecimal price, BigDecimal amount, String baseQuote, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("create createBuyOrder,symbols={},price={},amount={},baseCurrency={}", symbol, price, amount, baseQuote);
        CreateOrderDto dto = new CreateOrderDto();
        dto.setSymbol(symbol);
        dto.setOrderType(CreateOrderRequest.OrderType.BUY_LIMIT);

        HuobiBaseDto baseDto = new HuobiBaseDto();
        baseDto.setUserId(symbolTradeConfig.getUserId());
        Accounts accounts = accountBiz.getSpotAccounts(baseDto);
        dto.setAmount(amount);
        dto.setAccountId(String.valueOf(accounts.getId()));
        dto.setPrice(price);
        dto.setUserId(symbolTradeConfig.getUserId());
        return tradeBiz.createOrder(dto);
    }

    /**
     * 检查交易深度是否满足创建卖单条件
     */
    private List<String> checkDeptAndCreateSaleOrder(RateChangeEntity rateChangeEntity, BigDecimal amount, SymbolTradeConfigEntity symbolTradeConfig) {
        log.info("checkDeptAndCreateSaleOrder,rateChangeVo={},amount={}", rateChangeEntity, amount);
        DepthDto dto = new DepthDto();
        dto.setSymbol(rateChangeEntity.getSaleSymbol());
        dto.setUserId(symbolTradeConfig.getUserId());
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
            if ((salePrice.subtract(buyPrice)).divide(buyPrice, 4, BigDecimal.ROUND_HALF_UP).compareTo(symbolTradeConfig.getAsksBlunder()) <= 0) {
                //买单足够
                if (remainAmount.compareTo(BigDecimal.ZERO) > 0) {
                    //卖一+上浮=购买价
                    salePrice = salePrice.subtract(symbolTradeConfig.getBuyIncreasePrice());
                    BigDecimal saleAmount = bid.get(1);
                    String orderId = createSaleOrder(rateChangeEntity.getSaleSymbol(), salePrice, saleAmount, rateChangeEntity.getQuoteCurrency(), symbolTradeConfig.getUserId());
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
                salePrice = salePrice.subtract(symbolTradeConfig.getBuyIncreasePrice());
            } else {
                salePrice = saleOnePrice.subtract(symbolTradeConfig.getBuyIncreasePrice());
            }
            String orderId = createSaleOrder(rateChangeEntity.getSaleSymbol(), salePrice, remainAmount, rateChangeEntity.getQuoteCurrency(), symbolTradeConfig.getUserId());
            orderIds.add(orderId);
            log.info("买单价卖出,remainAmount={},salePrice={},orderId={}", remainAmount, salePrice, orderId);
        }
        log.info("orderIds={}", orderIds);
        return orderIds;
    }

    /**
     * 限价卖
     */
    private String createSaleOrder(String symbol, BigDecimal price, BigDecimal amount, String quoteCurrency, String userId) {
        log.info("create order,symbols={},price={},amount={},quoteCurrency={},userId={}", symbol, price, amount, quoteCurrency, userId);
        //判断是否超过可使用的上限
        CreateOrderDto dto = new CreateOrderDto();
        dto.setSymbol(symbol);
        dto.setOrderType(CreateOrderRequest.OrderType.SELL_LIMIT);
        dto.setPrice(price);

        HuobiBaseDto baseDto = new HuobiBaseDto();
        baseDto.setUserId(userId);
        Accounts accounts = accountBiz.getSpotAccounts(baseDto);
        BigDecimal balanceMax = accountBiz.getQuoteBalance(userId, quoteCurrency);
        amount = balanceMax.compareTo(amount) < 0 ? balanceMax : amount;
        dto.setAccountId(String.valueOf(accounts.getId()));
        dto.setAmount(amount);
        dto.setUserId(userId);
        return tradeBiz.createOrder(dto);
    }

    /**
     * 购买数量
     * newPrice 最新价格
     */
    private BigDecimal getBuyAmount(String symbol, BigDecimal newPrice, SymbolTradeConfigEntity symbolTradeConfig, String baseQuote) {
        BigDecimal amount;
        BigDecimal maxBalance = accountBiz.getQuoteBalance(symbolTradeConfig.getUserId(), baseQuote);
        if (symbol.endsWith(DictEnum.MARKET_BASE_USDT.getCode())) {
            maxBalance = maxBalance.compareTo(symbolTradeConfig.getUsdtMaxUse()) < 0 ? maxBalance : symbolTradeConfig.getUsdtMaxUse();
            amount = maxBalance.divide(newPrice, 4, BigDecimal.ROUND_FLOOR);
        } else if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode())) {
            maxBalance = maxBalance.compareTo(symbolTradeConfig.getBtcMaxUse()) < 0 ? maxBalance : symbolTradeConfig.getUsdtMaxUse();
            amount = maxBalance.divide(newPrice, 8, BigDecimal.ROUND_FLOOR);
        } else {
            maxBalance = maxBalance.compareTo(symbolTradeConfig.getEthMxzUse()) < 0 ? maxBalance : symbolTradeConfig.getUsdtMaxUse();
            amount = maxBalance.divide(newPrice, 8, BigDecimal.ROUND_FLOOR);
        }
        log.info("getBuyAmount,symbols={},newPrice={},amount={},symbolTradeConfig={}", symbol, newPrice, amount, symbolTradeConfig);
        return amount;
    }


    /**
     * 获取主对
     */
    private String getBaseCurrency(String symbol) {
        String baseCurrency;
        if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode()) || symbol.endsWith(DictEnum.MARKET_BASE_ETH.getCode())) {
            baseCurrency = symbol.substring(symbol.length() - 3, symbol.length());
        } else {
            baseCurrency = symbol.substring(symbol.length() - 4, symbol.length());
        }
        return baseCurrency;
    }

    /**
     * 获取业务对
     */
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
