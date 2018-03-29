package com.money.game.robot.service;

import com.money.game.robot.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * @author conan
 *         2018/1/5 11:23
 **/
public interface OrderService {


    OrderEntity findByOrderId(String orderId);

    OrderEntity findOne(String oid);

    OrderEntity save(OrderEntity entity);

    List<OrderEntity> findByState(List<String> states, String type);

    List<OrderEntity> findBySymbolAndType(String symbol, String orderType, List<String> states);

    List<OrderEntity> findByParam(String userId, String model, String orderType, String symbol,String symbolTradeConfigId, List<String> states);

    List<OrderEntity> findByUserIdAndModel(String userId, String model);

    Page<OrderEntity> findAll(Specification<OrderEntity> spec, Pageable pageable);
}
