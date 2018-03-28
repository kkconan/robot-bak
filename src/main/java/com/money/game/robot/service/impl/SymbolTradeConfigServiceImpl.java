package com.money.game.robot.service.impl;

import com.money.game.robot.dao.SymbolTradeConfigDao;
import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.service.SymbolTradeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author conan
 *         2018/3/21 17:27
 **/
@Service
public class SymbolTradeConfigServiceImpl implements SymbolTradeConfigService {

    @Autowired
    private SymbolTradeConfigDao symbolTradeConfigDao;

    @Override
    public List<SymbolTradeConfigEntity> findByUserId(String userId) {
        return symbolTradeConfigDao.findByUserId(userId);
    }

    @Override
    public SymbolTradeConfigEntity findByUserIdAndThresholdType(String userId, String thresholdType) {
        return symbolTradeConfigDao.findByUserIdAndThresholdType(userId, thresholdType);
    }

    @Override
    public SymbolTradeConfigEntity findById(String id) {
        return symbolTradeConfigDao.findOne(id);
    }

    @Override
    public SymbolTradeConfigEntity save(SymbolTradeConfigEntity entity) {
        return symbolTradeConfigDao.save(entity);
    }
}
