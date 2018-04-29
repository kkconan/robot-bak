package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.constant.ErrorEnum;
import com.money.game.robot.dto.client.LimitBetaConfigDto;
import com.money.game.robot.dto.client.LimitGammaConfigDto;
import com.money.game.robot.entity.LimitGammaConfigEntity;
import com.money.game.robot.exception.BizException;
import com.money.game.robot.service.LimitGammaConfigService;
import com.money.game.robot.vo.LimitBetaConfigVo;
import com.money.game.robot.vo.huobi.LimitGammaConfigVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author conan
 *         2018/4/16 10:50
 **/
@Component
public class LimitGammaConfigBiz {

    @Autowired
    private LimitGammaConfigService limitGammaConfigService;

    public List<LimitGammaConfigEntity> findByUserIdAndMarketType(String userId, String marketType) {
        List<LimitGammaConfigEntity> list = limitGammaConfigService.findByUserIdAndMarketType(userId, marketType);
        Iterator<LimitGammaConfigEntity> configIt = list.iterator();
        while (configIt.hasNext()) {
            LimitGammaConfigEntity entity = configIt.next();
            if (DictEnum.STATUS_STOP.getCode().equals(entity.getStatus()) || DictEnum.IS_DELETE_YES.getCode().equals(entity.getIsDelete())) {
                configIt.remove();
            }
        }
        return list;
    }

    public LimitGammaConfigEntity save(LimitGammaConfigEntity entity) {
        return limitGammaConfigService.save(entity);
    }

    public List<LimitGammaConfigVo> findAllList(String userId) {
        List<LimitGammaConfigEntity> list = limitGammaConfigService.findByUserId(userId);
        List<LimitGammaConfigVo> resultList = new ArrayList<>();
        for (LimitGammaConfigEntity entity : list) {
            LimitGammaConfigVo vo = new LimitGammaConfigVo();
            if (DictEnum.IS_DELETE_NO.getCode().equals(entity.getIsDelete())) {
                BeanUtils.copyProperties(entity, vo);
                resultList.add(vo);
            }
        }
        return resultList;
    }

    public LimitBetaConfigVo info(String oid) {
        LimitGammaConfigEntity entity = limitGammaConfigService.findById(oid);
        if (entity == null) {
            throw new BizException(ErrorEnum.RECOLD_NOT_FOUND);
        }
        LimitBetaConfigVo vo = new LimitBetaConfigVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public void save(LimitGammaConfigDto dto, String userId) {
        LimitGammaConfigEntity entity = new LimitGammaConfigEntity();
        entity.setUserId(userId);
        if (StringUtils.isNotEmpty(dto.getOid())) {
            entity = limitGammaConfigService.findById(dto.getOid());
        }
        BeanUtils.copyProperties(dto, entity);
        this.save(entity);
    }

    public void delete(String oid) {

        LimitGammaConfigEntity entity = limitGammaConfigService.findById(oid);
        if (entity == null) {
            throw new BizException(ErrorEnum.RECOLD_NOT_FOUND);
        }
        entity.setIsDelete(DictEnum.IS_DELETE_YES.getCode());
        this.save(entity);
    }

    public void updateStatus(LimitBetaConfigDto dto) {
        LimitGammaConfigEntity entity = limitGammaConfigService.findById(dto.getOid());
        if (entity == null) {
            throw new BizException(ErrorEnum.RECOLD_NOT_FOUND);
        }
        entity.setStatus(dto.getStatus());
        this.save(entity);
    }
}
