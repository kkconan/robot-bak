package com.money.game.robot.dao;

import com.money.game.robot.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author conan
 *         2017/10/26 13:41
 **/
public interface OrderDao extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {

    OrderEntity findByOrderId(String orderId);

    @Query(value="select * from T_ORDER  where state in (?1) and type = ?2 ",nativeQuery = true)
    List<OrderEntity> findByState(List<String> states, String orderType);

    @Query(value = "select * from T_ORDER  where symbol =?1  and type = ?2 and state in (?3)", nativeQuery = true)
    List<OrderEntity> findBySymbolAndType(String symbol, String orderType, List<String> states);


}


