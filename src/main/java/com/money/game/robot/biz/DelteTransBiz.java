package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.CreateOrderDto;
import com.money.game.robot.dto.huobi.DepthDto;
import com.money.game.robot.entity.LimitDelteConfigEntity;
import com.money.game.robot.entity.OrderEntity;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.huobi.response.Depth;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.vo.huobi.MarketDetailVo;
import com.money.game.robot.vo.huobi.MarketInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author conan
 *         2018/5/3 10:27
 **/
@Component
@Slf4j
public class DelteTransBiz {

    @Autowired
    private HuobiApi huobiApi;

    @Autowired
    private UserBiz userBiz;

    @Autowired
    private LimitDelteConfigBiz limitDelteConfigBiz;

    @Autowired
    private OrderBiz orderBiz;

    @Autowired
    private MarketRuleBiz marketRuleBiz;

    @Autowired
    private AccountBiz accountBiz;

    @Autowired
    private TradeBiz tradeBiz;

    @Autowired
    private MailBiz mailBiz;

    @Autowired
    private MarketBiz marketBiz;

    /**
     * 更新delte订单状态
     */
    public void checkDelteStatus() {
        List<OrderEntity> orderEntityList = orderBiz.hbDetleNotFinishOrder();
        for (OrderEntity orderEntity : orderEntityList) {
            orderBiz.updateHbOrderState(orderEntity);
            if (orderEntity.getState().equals(DictEnum.ORDER_DETAIL_STATE_FILLED.getCode())) {
                mailBiz.transToEmailNotify(orderEntity, orderEntity.getUserId());
            }
        }
    }


    /**
     * 检查MA趋势delte单
     */
    public void checkByMa() {
        List<UserEntity> userList = userBiz.findAllHbByNormal();
        for (UserEntity user : userList) {
            List<LimitDelteConfigEntity> delteList = limitDelteConfigBiz.findByUserIdAndMarketType(user.getOid(), DictEnum.MARKET_TYPE_HB.getCode());
            for (LimitDelteConfigEntity delte : delteList) {
                try {
                    Boolean maUp = calcMa5min(delte.getSymbol(), DictEnum.MARKET_PERIOD_5MIN.getCode());
                    //上升
                    if (maUp != null && maUp) {
                        OrderEntity buyOrder = orderBiz.findHbDelteBuyOrder(delte.getUserId(), delte.getSymbol(), delte.getOid(), DictEnum.ORDER_MODEL_LIMIT_DELTE.getCode());
                        //不存在买单
                        if (buyOrder == null) {
                            //创建买单
                            hbCreateDelteLimitBuyOrderWithDelte(delte);
                            //处理前一笔卖单
                            OrderEntity saleOrder = orderBiz.findHbDelteSellOrder(delte.getUserId(), delte.getSymbol(), delte.getOid(), DictEnum.ORDER_MODEL_LIMIT_DELTE.getCode());
                            if (saleOrder != null) {
                                OrderEntity oldBuyOrder = hbCreateDelteLimitBuyOrderWithSaleOrder(saleOrder);
                                //更新买单为已完成
                                oldBuyOrder.setIsFinish(DictEnum.IS_FINISH_YES.getCode());
                                orderBiz.saveOrder(oldBuyOrder);
                                //创建买单并更新原卖单为已卖出
                                saleOrder.setBuyOrderId(oldBuyOrder.getOrderId());
                                saleOrder.setState(DictEnum.ORDER_DETAIL_STATE_BUY.getCode());
                                saleOrder.setIsFinish(DictEnum.IS_FINISH_YES.getCode());
                                orderBiz.saveOrder(saleOrder);
                            }
                        }

                    }
                    //下降
                    else if (maUp != null) {
                        OrderEntity saleOrder = orderBiz.findHbDelteSellOrder(delte.getUserId(), delte.getSymbol(), delte.getOid(), DictEnum.ORDER_MODEL_LIMIT_DELTE.getCode());
                        //不存在卖单
                        if (saleOrder == null) {
                            //创建卖单
                            hbCreateLimitSellOrderWithDelte(delte);
                            //处理前一笔买单
                            OrderEntity buyOrder = orderBiz.findHbDelteBuyOrder(delte.getUserId(), delte.getSymbol(), delte.getOid(), DictEnum.ORDER_MODEL_LIMIT_DELTE.getCode());
                            if (buyOrder != null) {
                                //创建历史买单对应的卖单
                                OrderEntity oldSaleOrder = hbCreateLimitSellOrderWithBuyOrder(buyOrder);
                                oldSaleOrder.setBuyOrderId(buyOrder.getOrderId());
                                oldSaleOrder.setIsFinish(DictEnum.IS_FINISH_YES.getCode());
                                orderBiz.saveOrder(oldSaleOrder);
                                //更新原买单为已卖出
                                buyOrder.setState(DictEnum.ORDER_DETAIL_STATE_SELL.getCode());
                                buyOrder.setIsFinish(DictEnum.IS_FINISH_YES.getCode());
                                orderBiz.saveOrder(buyOrder);

                            }
                        }
                    }
                    //hb 行情有访问频率控制
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("处理delte订单失败,delte={},e={}", delte, e);
                }
            }
        }
    }


    public Boolean calcMa5min(String symbol, String period) {
        Boolean maUp = null;
        MarketInfoVo marketInfoVo = huobiApi.getMarketInfo(period, 8, symbol);
        if (marketInfoVo == null) {
            log.info("获取详情失败");
            return null;
        }
        List<MarketDetailVo> detailVoList = marketInfoVo.getData();
        BigDecimal oneTotal = BigDecimal.ZERO;
        BigDecimal twoTotal = BigDecimal.ZERO;
        BigDecimal threeTotal = BigDecimal.ZERO;
        BigDecimal fourTotal = BigDecimal.ZERO;
        BigDecimal oneMiddle;
        BigDecimal twoMiddle;
        BigDecimal threeMiddle;
        BigDecimal fourMiddle;
        for (int i = 0; i < detailVoList.size(); i++) {
            if (i >= 0 && i < 5) {
                oneTotal = oneTotal.add(detailVoList.get(i).getClose());
            }
            if (i >= 1 && i < 6) {
                twoTotal = twoTotal.add(detailVoList.get(i).getClose());
            }
            if (i >= 2 && i < 7) {
                threeTotal = threeTotal.add(detailVoList.get(i).getClose());
            }
            if (i >= 3 && i < 8) {
                fourTotal = fourTotal.add(detailVoList.get(i).getClose());
            }
        }
        oneMiddle = oneTotal.divide(new BigDecimal(5), 8, BigDecimal.ROUND_HALF_UP);
        twoMiddle = twoTotal.divide(new BigDecimal(5), 8, BigDecimal.ROUND_HALF_UP);
        threeMiddle = threeTotal.divide(new BigDecimal(5), 8, BigDecimal.ROUND_HALF_UP);
        fourMiddle = fourTotal.divide(new BigDecimal(5), 8, BigDecimal.ROUND_HALF_UP);
        log.info("oneMiddle={},twoMiddle={},threeMiddle={},fourMiddle={}", oneMiddle, twoMiddle, threeMiddle, fourMiddle);
        //up
        if (oneMiddle.compareTo(twoMiddle) > 0) {
            //连续增长
            if (twoMiddle.compareTo(threeMiddle) > 0 && threeMiddle.compareTo(fourMiddle) > 0) {
                //增长幅度增加
                if (oneMiddle.subtract(twoMiddle).compareTo(twoMiddle.subtract(threeMiddle)) > 0) {
                    BigDecimal maMin15 = getNewMa15min(symbol);
                    //向上离15min线越来越近或者穿过
                    if(oneMiddle.subtract(maMin15).compareTo(twoMiddle.subtract(maMin15)) > 0 || oneMiddle.compareTo(maMin15) > 0){
                        maUp = true;
                    }
                }
            }
        }
        //down
        if (oneMiddle.compareTo(twoMiddle) < 0) {
            //连续下降
            if (twoMiddle.compareTo(threeMiddle) < 0 && threeMiddle.compareTo(fourMiddle) < 0) {
                //下降幅度增加
                if (oneMiddle.subtract(twoMiddle).compareTo(twoMiddle.subtract(threeMiddle)) < 0) {

                    BigDecimal maMin15 = getNewMa15min(symbol);
                    //向下离15min线越来越近或者穿过
                    if(oneMiddle.subtract(maMin15).compareTo(twoMiddle.subtract(maMin15)) < 0 || oneMiddle.compareTo(maMin15) < 0){
                        maUp = false;
                    }
                }
            }
        }
        log.info("计算ma趋势结果,maUp={},symbol={}", maUp, symbol);
        return maUp;
    }


    public BigDecimal getNewMa15min(String symbol) {
        MarketInfoVo marketInfoVo = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_15MIN.getCode(), 5, symbol);
        if (marketInfoVo == null) {
            log.info("获取详情失败");
            return null;
        }
        List<MarketDetailVo> detailVoList = marketInfoVo.getData();
        BigDecimal oneTotal = BigDecimal.ZERO;
        BigDecimal oneMiddle;
        for (MarketDetailVo detailVo : detailVoList) {
            oneTotal = oneTotal.add(detailVo.getClose());
        }
        oneMiddle = oneTotal.divide(new BigDecimal(5), 8, BigDecimal.ROUND_HALF_UP);
        log.info("min15Middle={}", oneMiddle);
        return oneMiddle;
    }


    /**
     * 单笔策略delte限价买单,主动买入时使用
     */
    private OrderEntity hbCreateDelteLimitBuyOrderWithDelte(LimitDelteConfigEntity delte) {
        log.info("创建detle买单,delte={}", delte);
        BigDecimal saleOnePrice = saleOnePrice(delte.getSymbol(), delte.getUserId());
        //略高买
        BigDecimal buyPrice = (new BigDecimal(1.0005)).multiply(saleOnePrice);
        String baseCurrency = marketRuleBiz.getHbBaseCurrency(delte.getSymbol());
        //基对余额
        BigDecimal balanceMax = accountBiz.getHuobiQuoteBalance(delte.getUserId(), baseCurrency);
        //总成交额
        BigDecimal totalAmount = delte.getTotalAmount().compareTo(balanceMax) <= 0 ? delte.getTotalAmount() : balanceMax;
        //购买数量
        BigDecimal amount = totalAmount.divide(buyPrice, 4, BigDecimal.ROUND_DOWN);
        //创建限价买单
        return hbCreateDelteLimitBuyOrder(delte.getSymbol(), delte.getUserId(), amount, delte.getOid());
    }

    /**
     * 单笔策略delte限价买单,被动买入时使用(对应卖单买入)
     */
    private OrderEntity hbCreateDelteLimitBuyOrderWithSaleOrder(OrderEntity saleOrder) {
        log.info("创建detle买单,saleOrderId={}", saleOrder.getOrderId());
        //购买数量
        BigDecimal amount = saleOrder.getAmount();

        String baseCurrency = marketRuleBiz.getHbBaseCurrency(saleOrder.getSymbol());
        //基对余额
        BigDecimal balanceMax = accountBiz.getHuobiQuoteBalance(saleOrder.getUserId(), baseCurrency);
        if (balanceMax.compareTo(saleOrder.getTotalToUsdt()) < 0) {
            amount = balanceMax.divide(saleOrder.getPrice(), 4, BigDecimal.ROUND_DOWN);
        }
        //创建限价买单
        return hbCreateDelteLimitBuyOrder(saleOrder.getSymbol(), saleOrder.getUserId(), amount, saleOrder.getOid());
    }


    /**
     * 单笔策略delte限价买单
     */
    private OrderEntity hbCreateDelteLimitBuyOrder(String symbol, String userId, BigDecimal amount, String configId) {
        log.info("创建detle买单,symbol={},amount={}", symbol, amount);
        CreateOrderDto buyOrderDto = new CreateOrderDto();
        buyOrderDto.setSymbol(symbol);
        BigDecimal saleOnePrice = saleOnePrice(symbol, userId);
        //略高买
        BigDecimal buyPrice = (new BigDecimal(1.0005)).multiply(saleOnePrice);
        buyOrderDto.setPrice(buyPrice);
        buyOrderDto.setOrderType(DictEnum.ORDER_TYPE_BUY_LIMIT.getCode());
        buyOrderDto.setUserId(userId);

        buyOrderDto.setAmount(amount);
        //创建限价买单
        String buyOrderId = tradeBiz.hbCreateOrder(buyOrderDto);
        return orderBiz.saveHbOrder(buyOrderId, null, null, configId, userId, DictEnum.ORDER_TYPE_BUY_LIMIT.getCode(), DictEnum.ORDER_MODEL_LIMIT_DELTE.getCode());
    }


    /**
     * 单笔策略限价卖单,主动卖出时使用
     */
    private OrderEntity hbCreateLimitSellOrderWithDelte(LimitDelteConfigEntity delte) {
        log.info("创建delte卖单,delte={}", delte);
        //买一价
        BigDecimal buyOnePrice = buyOnePrice(delte.getSymbol(), delte.getUserId());
        //略低卖
        BigDecimal salePrice = (new BigDecimal(0.9995)).multiply(buyOnePrice);
        String quoteCurrency = marketRuleBiz.getHbQuoteCurrency(delte.getSymbol());
        //业务对余额
        BigDecimal balanceMax = accountBiz.getHuobiQuoteBalance(delte.getUserId(), quoteCurrency);
        BigDecimal amount = delte.getTotalAmount().divide(salePrice, 2, BigDecimal.ROUND_DOWN);
        //验证余额
        if (amount.compareTo(balanceMax) > 0) {
            log.info("余额不足,售卖数量不足,amount={},balanceMax={}", amount, balanceMax);
            mailBiz.balanceToEmailNotify(delte.getUserId(), quoteCurrency, DictEnum.MARKET_TYPE_HB.getCode());
            amount = balanceMax;
        }
        return hbCreateLimitSellOrder(delte.getSymbol(), delte.getUserId(), amount, delte.getOid());
    }


    /**
     * 单笔策略限价卖单,对应买单使用
     */
    private OrderEntity hbCreateLimitSellOrderWithBuyOrder(OrderEntity orderEntity) {
        log.info("创建delte卖单,buyOrderId={}", orderEntity.getOrderId());
        //卖出数量
        BigDecimal amount = orderEntity.getAmount();
        String quoteCurrency = marketRuleBiz.getHbQuoteCurrency(orderEntity.getSymbol());
        //业务对余额
        BigDecimal balanceMax = accountBiz.getHuobiQuoteBalance(orderEntity.getUserId(), quoteCurrency);
        amount = balanceMax.compareTo(amount) < 0 ? balanceMax : amount;
        return hbCreateLimitSellOrder(orderEntity.getSymbol(), orderEntity.getUserId(), amount, orderEntity.getSymbolTradeConfigId());
    }


    /**
     * 单笔策略限价卖单
     */
    private OrderEntity hbCreateLimitSellOrder(String symbol, String userId, BigDecimal amount, String configId) {
        log.info("创建delte卖单,symbol={},amount={}", symbol, amount);
        CreateOrderDto saleOrderDto = new CreateOrderDto();
        //买一价
        BigDecimal buyOnePrice = buyOnePrice(symbol, userId);
        BigDecimal salePrice = (new BigDecimal(0.9995)).multiply(buyOnePrice);
        saleOrderDto.setSymbol(symbol);
        saleOrderDto.setOrderType(DictEnum.ORDER_TYPE_SELL_LIMIT.getCode());
        saleOrderDto.setPrice(salePrice);
        saleOrderDto.setUserId(userId);
        saleOrderDto.setAmount(amount);
        //创建限价卖单
        String saleOrderId = tradeBiz.hbCreateOrder(saleOrderDto);
        return orderBiz.saveHbOrder(saleOrderId, null, null, configId, userId, DictEnum.ORDER_TYPE_SELL_LIMIT.getCode(), DictEnum.ORDER_MODEL_LIMIT_DELTE.getCode());
    }


    /**
     * 买一价
     */
    private BigDecimal buyOnePrice(String symbol, String userId) {
        Depth depth = getDeth(symbol, userId);
        return depth.getBids().get(0).get(0);
    }

    /**
     * 卖一价
     */
    private BigDecimal saleOnePrice(String symbol, String userId) {
        Depth depth = getDeth(symbol, userId);
        return depth.getAsks().get(0).get(0);
    }

    private Depth getDeth(String symbol, String userId) {
        DepthDto dto = new DepthDto();
        dto.setSymbol(symbol);
        dto.setUserId(userId);
        return marketBiz.HbDepth(dto);
    }

}
