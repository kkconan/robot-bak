package com.money.game.robot.service;

import com.money.game.robot.entity.LimitBetaConfigEntity;

import java.util.List;

/**
 * @author conan
 *         2018/4/16 10:43
 **/
public interface LimitBetaConfigService {

    List<LimitBetaConfigEntity> findByUserIdAndMarketType(String userId, String marketType);

    LimitBetaConfigEntity save(LimitBetaConfigEntity entity);
}
