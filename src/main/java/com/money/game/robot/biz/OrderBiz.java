package com.money.game.robot.biz;

import com.money.game.core.util.DateUtils;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.entity.OrderEntity;
import com.money.game.robot.huobi.response.OrdersDetail;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.service.OrderService;
import com.money.game.robot.vo.huobi.MarketInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author conan
 *         2018/3/20 10:38
 **/
@Component
@Slf4j
public class OrderBiz {

    @Autowired
    private TradeBiz tradeBiz;

    @Autowired
    private OrderService orderService;

    @Autowired
    private HuobiApi huobiApi;


    /**
     * 该交易对是否存在未完成的买单/卖单
     */
    public boolean existNotFinishOrder(String symbol, String type) {
        boolean result = false;
        List<String> states = new ArrayList<>();
        states.add(DictEnum.ORDER_DETAIL_STATE_PRE_SUBMITTED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_SUBMITTING.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_SUBMITTED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_PARTIAL_FILLED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_PARTIAL_CANCELED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_FILLED.getCode());
        List<OrderEntity> orderEntityList = orderService.findBySymbolAndType(symbol, type, states);
        for (OrderEntity orderEntity : orderEntityList) {
            if (DateUtils.addDay(orderEntity.getCreateTime(), 1).after(DateUtils.getCurrDateMmss())) {
                log.info("一天之内已有未完成的订单,orderEntity={}", orderEntity);
                result = true;
                break;
            }
        }
        return result;
    }

    public OrderEntity saveOrder(String orderId, String rateChangeId, String buyOrderId) {
        HuobiBaseDto dto = new HuobiBaseDto();
        dto.setOrderId(orderId);
        OrdersDetail ordersDetail = tradeBiz.orderDetail(dto);
        OrderEntity orderEntity = orderService.findByOrderId(orderId);
        if (orderEntity == null) {
            orderEntity = new OrderEntity();
        }
        BeanUtils.copyProperties(ordersDetail, orderEntity);
        orderEntity.setRateChangeId(rateChangeId);
        orderEntity.setOrderId(ordersDetail.getId());
        orderEntity.setBuyOrderId(buyOrderId);
        BigDecimal totalToUsdt = getTotalToUsdt(orderEntity.getSymbol(),orderEntity.getPrice(),orderEntity.getAmount());
        orderEntity.setTotalToUsdt(totalToUsdt);
        return this.saveOrder(orderEntity);
    }

    /**
     * 同步订单最新状态
     */
    public OrderEntity updateOrderState(OrderEntity orderEntity) {
        HuobiBaseDto dto = new HuobiBaseDto();
        dto.setOrderId(orderEntity.getOrderId());
        OrdersDetail ordersDetail = tradeBiz.orderDetail(dto);
        //订单状态或者成交数量有变动
        if (!ordersDetail.getState().equals(orderEntity.getState()) || !ordersDetail.getFieldAmount().equals(orderEntity.getFieldAmount())) {
            BeanUtils.copyProperties(ordersDetail, orderEntity);
            orderEntity = this.saveOrder(orderEntity);
        }
        return orderEntity;
    }

    public OrderEntity saveOrder(OrderEntity entity) {
        return orderService.save(entity);
    }

    /**
     * 获取未完成的买单(部分成交,部分成交撤销，完全成交)
     */
    public List<OrderEntity> findNoFilledBuyOrder() {
        List<String> states = new ArrayList<>();
        states.add(DictEnum.ORDER_DETAIL_STATE_SUBMITTED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_PARTIAL_FILLED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_PARTIAL_CANCELED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_FILLED.getCode());
        return orderService.findByState(states, DictEnum.ORDER_TYPE_BUY_LIMIT.getCode());

    }

    /**
     * 获取未完成的卖单(已提交,部分成交,部分成交撤销)
     */
    public List<OrderEntity> findNoFilledSaleOrder() {
        List<String> states = new ArrayList<>();
        states.add(DictEnum.ORDER_DETAIL_STATE_SUBMITTING.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_SUBMITTED.getCode());
        states.add(DictEnum.ORDER_DETAIL_STATE_PARTIAL_FILLED.getCode());
        return orderService.findByState(states, DictEnum.ORDER_TYPE_SELL_LIMIT.getCode());

    }

    /**
     * 下单总额转换成usdt值
     */
    private BigDecimal getTotalToUsdt(String symbol, BigDecimal price, BigDecimal amount) {
        BigDecimal totalToUsdt;
        MarketInfoVo marketInfoVo;
        if (symbol.endsWith(DictEnum.MARKET_BASE_USDT.getCode())) {
            totalToUsdt = price.multiply(amount);
        } else if (symbol.endsWith(DictEnum.MARKET_BASE_BTC.getCode())) {
            marketInfoVo = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, DictEnum.MARKET_HUOBI_SYMBOL_BTC_USDT.getCode());
            totalToUsdt = price.multiply(amount).multiply(marketInfoVo.getData().get(0).getClose());
        } else {
            marketInfoVo = huobiApi.getMarketInfo(DictEnum.MARKET_PERIOD_1MIN.getCode(), 1, DictEnum.MARKET_HUOBI_SYMBOL_ETH_USDT.getCode());
            totalToUsdt = price.multiply(amount).multiply(marketInfoVo.getData().get(0).getClose());
        }
        return totalToUsdt;
    }
}
