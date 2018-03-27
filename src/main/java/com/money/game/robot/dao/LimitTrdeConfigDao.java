package com.money.game.robot.dao;

import com.money.game.robot.entity.LimitTradeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author conan
 *         2018/3/26 13:37
 **/
public interface LimitTrdeConfigDao  extends JpaRepository<LimitTradeConfigEntity, String>, JpaSpecificationExecutor<LimitTradeConfigEntity>{

    List<LimitTradeConfigEntity> findAllByUserId(String userId);

}
