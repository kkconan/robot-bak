package com.money.game.robot.biz;

import com.money.game.robot.dto.client.SymbolTradeConfigDto;
import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.service.SymbolTradeConfigService;
import com.money.game.robot.vo.SymbolTradeConfigVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    public List<SymbolTradeConfigVo> findAllList(String userId) {
        List<SymbolTradeConfigVo> voList = new ArrayList<>();
        SymbolTradeConfigVo vo = new SymbolTradeConfigVo();
        List<SymbolTradeConfigEntity> list = symbolTradeConfigService.findByUserId(userId);
        for (SymbolTradeConfigEntity entity : list) {
            BeanUtils.copyProperties(entity, vo);
            voList.add(vo);
        }
        return voList;
    }

    public SymbolTradeConfigVo info(String oid) {
        SymbolTradeConfigEntity entity = symbolTradeConfigService.findById(oid);
        SymbolTradeConfigVo vo = new SymbolTradeConfigVo();
        if(entity != null) {
            BeanUtils.copyProperties(entity, vo);
        }
        return vo;
    }

    public void save(SymbolTradeConfigDto dto, String userId) {
        SymbolTradeConfigEntity entity = new SymbolTradeConfigEntity();
        entity.setUserId(userId);
        if (StringUtils.isNotEmpty(dto.getOid())) {
            entity = symbolTradeConfigService.findById(dto.getOid());
        }
        BeanUtils.copyProperties(dto, entity);
        symbolTradeConfigService.save(entity);
    }
}
