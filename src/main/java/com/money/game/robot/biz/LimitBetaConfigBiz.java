package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.entity.LimitBetaConfigEntity;
import com.money.game.robot.service.LimitBetaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * @author conan
 *         2018/4/16 10:50
 **/
@Component
public class LimitBetaConfigBiz {

    @Autowired
    private LimitBetaConfigService limitBetaConfigService;

    public List<LimitBetaConfigEntity> findByUserIdAndMarketType(String userId, String marketType) {
        List<LimitBetaConfigEntity> list = limitBetaConfigService.findByUserIdAndMarketType(userId, marketType);
        Iterator<LimitBetaConfigEntity> configIt = list.iterator();
        while (configIt.hasNext()) {
            LimitBetaConfigEntity entity = configIt.next();
            if (DictEnum.STATUS_STOP.getCode().equals(entity.getStatus()) || DictEnum.IS_DELETE_YES.getCode().equals(entity.getIsDelete())) {
                configIt.remove();
            }
        }
        return list;
    }

    public LimitBetaConfigEntity save(LimitBetaConfigEntity entity){
        return limitBetaConfigService.save(entity);
    }
}
