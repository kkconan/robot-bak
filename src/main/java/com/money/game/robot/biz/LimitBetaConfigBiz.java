package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.constant.ErrorEnum;
import com.money.game.robot.dto.client.LimitBetaConfigDto;
import com.money.game.robot.entity.LimitBetaConfigEntity;
import com.money.game.robot.exception.BizException;
import com.money.game.robot.service.LimitBetaConfigService;
import com.money.game.robot.vo.LimitBetaConfigVo;
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

    public LimitBetaConfigEntity save(LimitBetaConfigEntity entity) {
        return limitBetaConfigService.save(entity);
    }

    public List<LimitBetaConfigVo> findAllList(String userId) {
        List<LimitBetaConfigEntity> list = limitBetaConfigService.findByUserId(userId);
        List<LimitBetaConfigVo> resultList = new ArrayList<>();
        for (LimitBetaConfigEntity entity : list) {
            LimitBetaConfigVo vo = new LimitBetaConfigVo();
            if (!DictEnum.STATUS_STOP.getCode().equals(entity.getStatus())) {
                BeanUtils.copyProperties(entity, vo);
                resultList.add(vo);
            }
        }
        return resultList;
    }

    public LimitBetaConfigVo info(String oid) {
        LimitBetaConfigEntity entity = limitBetaConfigService.findById(oid);
        if (entity == null) {
            throw new BizException(ErrorEnum.RECOLD_NOT_FOUND);
        }
        LimitBetaConfigVo vo = new LimitBetaConfigVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public void save(LimitBetaConfigDto dto, String userId) {
        LimitBetaConfigEntity entity = new LimitBetaConfigEntity();
        entity.setUserId(userId);
        if (StringUtils.isNotEmpty(dto.getOid())) {
            entity = limitBetaConfigService.findById(dto.getOid());
        }
        BeanUtils.copyProperties(dto, entity);
        this.save(entity);
    }


    public void delete(String oid) {

        LimitBetaConfigEntity entity = limitBetaConfigService.findById(oid);
        if (entity == null) {
            throw new BizException(ErrorEnum.RECOLD_NOT_FOUND);
        }
        entity.setIsDelete(DictEnum.IS_DELETE_YES.getCode());
        this.save(entity);
    }


    public void updateStatus(LimitBetaConfigDto dto) {
        LimitBetaConfigEntity entity = limitBetaConfigService.findById(dto.getOid());
        if (entity == null) {
            throw new BizException(ErrorEnum.RECOLD_NOT_FOUND);
        }
        entity.setStatus(dto.getStatus());
        this.save(entity);
    }
}
