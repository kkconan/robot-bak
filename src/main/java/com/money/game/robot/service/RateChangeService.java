package com.money.game.robot.service;

import com.money.game.robot.entity.RateChangeEntity;

/**
 * @author conan
 *         2018/3/20 11:14
 **/
public interface RateChangeService {

    RateChangeEntity findOne(String id);

    RateChangeEntity save(RateChangeEntity entity);

}
