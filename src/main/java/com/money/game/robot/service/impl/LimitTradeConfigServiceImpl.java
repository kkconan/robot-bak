package com.money.game.robot.service.impl;

import com.money.game.robot.dao.LimitTrdeConfigDao;
import com.money.game.robot.entity.LimitTradeConfigEntity;
import com.money.game.robot.service.LimitTradeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author conan
 *         2018/3/26 13:51
 **/
@Service
public class LimitTradeConfigServiceImpl implements LimitTradeConfigService {

    @Autowired
    private LimitTrdeConfigDao limitTrdeConfigDao;
    @Override
    public List<LimitTradeConfigEntity> findAllByUserId(String userId) {
        return limitTrdeConfigDao.findAllByUserId(userId);
    }
}