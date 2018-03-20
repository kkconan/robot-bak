package com.money.game.robot.dao;

import com.money.game.robot.entity.RateChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author conan
 *         2017/10/26 13:41
 **/
public interface RateChangeDao extends JpaRepository<RateChangeEntity, String>, JpaSpecificationExecutor<RateChangeEntity> {

}


