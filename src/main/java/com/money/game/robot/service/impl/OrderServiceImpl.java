package com.money.game.robot.service.impl;

import com.money.game.robot.dao.OrderDao;
import com.money.game.robot.entity.OrderEntity;
import com.money.game.robot.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author conan
 *         2018/1/5 11:24
 **/
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDao orderDao;

    @Override
    public OrderEntity findByOrderId(String orderId) {
        return orderDao.findByOrderId(orderId);
    }

    @Override
    public OrderEntity findOne(String oid) {
        return orderDao.findOne(oid);
    }

    @Override
    public OrderEntity save(OrderEntity entity) {
        return orderDao.save(entity);
    }

    @Override
    public List<OrderEntity> findByState(List<String> states, String type) {
        return orderDao.findByState(states, type);
    }

    @Override
    public List<OrderEntity> findBySymbolAndType(String symbol, String orderType, String symbolConfigId, List<String> states) {
        return orderDao.findBySymbolAndType(symbol, orderType, symbolConfigId, states);
    }

    @Override
    public List<OrderEntity> findByParam(String userId, String model, String orderType, String symbol, String symbolTradeConfigId, String marketType, List<String> states) {
        return orderDao.findByParam(userId, model, orderType, symbol, symbolTradeConfigId, marketType, states);
    }

    @Override
    public List<OrderEntity> findShuffleByMarket(String model, String marketType, List<String> states) {
        return orderDao.findShuffleByMarket(model, marketType, states);
    }

    @Override
    public List<OrderEntity> findByUserIdAndModel(String userId, String model) {
        return orderDao.findByUserIdAndModel(userId, model);
    }

    @Override
    public Page<OrderEntity> findAll(Specification<OrderEntity> spec, Pageable pageable) {
        return orderDao.findAll(spec, pageable);
    }
}