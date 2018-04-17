package com.money.game.robot.service.impl;

import com.money.game.robot.dao.LimitBetaConfigDao;
import com.money.game.robot.entity.LimitBetaConfigEntity;
import com.money.game.robot.service.LimitBetaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author conan
 *         2018/4/16 10:43
 **/
@Service
public class LimitBetaConfigServiceImpl implements LimitBetaConfigService {

    @Autowired
    private LimitBetaConfigDao limitBetaConfigDao;

    @Override
    public List<LimitBetaConfigEntity> findByUserIdAndMarketType(String userId, String marketType) {
        return limitBetaConfigDao.findByUserIdAndMarketType(userId, marketType);
    }

    @Override
    public LimitBetaConfigEntity save(LimitBetaConfigEntity entity) {
        return limitBetaConfigDao.save(entity);
    }
}
