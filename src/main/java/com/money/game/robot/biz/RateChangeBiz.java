package com.money.game.robot.biz;

import com.money.game.robot.entity.RateChangeEntity;
import com.money.game.robot.service.RateChangeService;
import com.money.game.robot.vo.huobi.RateChangeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author conan
 *         2018/3/20 13:35
 **/
@Component
public class RateChangeBiz {

    @Autowired
    private RateChangeService rateChangeService;

    public RateChangeEntity save(RateChangeVo vo) {
        RateChangeEntity rateChangeEntity = new RateChangeEntity();
        BeanUtils.copyProperties(vo, rateChangeEntity);
        return rateChangeService.save(rateChangeEntity);

    }
}
