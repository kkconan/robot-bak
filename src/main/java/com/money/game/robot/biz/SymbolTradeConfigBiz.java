package com.money.game.robot.biz;

import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.service.SymbolTradeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author conan
 *         2018/3/22 10:31
 **/
@Component
public class SymbolTradeConfigBiz {


    @Autowired
    private SymbolTradeConfigService symbolTradeConfigService;

    public SymbolTradeConfigEntity findByUserIdAndThresholdType(String userId, String thresholdType) {

        return symbolTradeConfigService.findByUserIdAndThresholdType(userId, thresholdType);
    }

    public SymbolTradeConfigEntity findById(String id) {
        return symbolTradeConfigService.findById(id);
    }
}
