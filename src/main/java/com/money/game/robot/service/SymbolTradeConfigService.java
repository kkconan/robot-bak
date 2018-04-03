package com.money.game.robot.service;

import com.money.game.robot.entity.SymbolTradeConfigEntity;

import java.util.List;

/**
 * @author conan
 *         2018/3/21 17:23
 **/
public interface SymbolTradeConfigService {

    List<SymbolTradeConfigEntity> findByUserId(String userId);

    SymbolTradeConfigEntity findByUserIdAndThresholdType(String userId, String thresholdType);

    SymbolTradeConfigEntity findByUserIdAndThresholdTypeAndMarketType(String userId, String thresholdType, String marketType);

    SymbolTradeConfigEntity findById(String id);

    SymbolTradeConfigEntity save(SymbolTradeConfigEntity entity);
}
