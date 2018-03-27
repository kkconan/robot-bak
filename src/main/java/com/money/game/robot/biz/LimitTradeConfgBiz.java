package com.money.game.robot.biz;

import com.money.game.robot.entity.LimitTradeConfigEntity;
import com.money.game.robot.service.LimitTradeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author conan
 *         2018/3/26 13:55
 **/
@Component
@Slf4j
public class LimitTradeConfgBiz {

    @Autowired
    private LimitTradeConfigService limitTradeConfigService;

    public List<LimitTradeConfigEntity> findAllByUserId(String userId) {
        return limitTradeConfigService.findAllByUserId(userId);
    }
}
