package com.money.game.robot.biz;

import com.money.game.robot.dto.client.LimitTradeConfigDto;
import com.money.game.robot.entity.LimitTradeConfigEntity;
import com.money.game.robot.service.LimitTradeConfigService;
import com.money.game.robot.vo.LimitTradeConfigVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public List<LimitTradeConfigEntity> findAllByUserIdAndMarketType(String userId,String marketType) {
        return limitTradeConfigService.findAllByUserIdAndMarketType(userId,marketType);
    }

    public List<LimitTradeConfigVo> findAllList(String userId) {
        List<LimitTradeConfigVo> voList = new ArrayList<>();
        List<LimitTradeConfigEntity> list = limitTradeConfigService.findAllByUserId(userId);
        for (LimitTradeConfigEntity entity : list) {
            LimitTradeConfigVo vo = new LimitTradeConfigVo();
            BeanUtils.copyProperties(entity, vo);
            voList.add(vo);
        }
        return voList;
    }

    public LimitTradeConfigVo info(String oid) {
        LimitTradeConfigEntity entity = limitTradeConfigService.findById(oid);
        LimitTradeConfigVo vo = new LimitTradeConfigVo();
        if (entity != null) {
            BeanUtils.copyProperties(entity, vo);
        }
        return vo;
    }

    public void save(LimitTradeConfigDto dto, String userId) {
        LimitTradeConfigEntity entity = new LimitTradeConfigEntity();
        entity.setUserId(userId);
        if (StringUtils.isNotEmpty(dto.getOid())) {
            entity = limitTradeConfigService.findById(dto.getOid());
        }
        BeanUtils.copyProperties(dto, entity);
        limitTradeConfigService.save(entity);
    }
}
