package com.money.game.robot.service;

import com.money.game.robot.entity.OrderEntity;

import java.util.List;

/**
 * @author conan
 *         2018/1/5 11:23
 **/
public interface OrderService {


    OrderEntity findByOrderId(String orderId);

    OrderEntity save(OrderEntity entity);

    List<OrderEntity> findByState(List<String> states, String type);

    List<OrderEntity> findBySymbolAndType(String symbol, String orderType, List<String> states);
}
