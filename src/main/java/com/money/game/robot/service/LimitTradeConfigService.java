package com.money.game.robot.service;

import com.money.game.robot.entity.LimitTradeConfigEntity;

import java.util.List;

/**
 * @author conan
 *         2018/3/26 13:49
 **/
public interface LimitTradeConfigService {

    List<LimitTradeConfigEntity> findAllByUserId(String userId);
}
