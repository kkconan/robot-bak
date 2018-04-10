package com.money.game.robot.entity;

import com.money.game.basic.component.ext.hibernate.UUID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * @author conan
 *         2018/4/10 15:20
 **/
@Entity
@Table(name = "T_SHUFFLE_CONFIG")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ShuffleConfigEntity extends UUID{

    private String userId;

    /**
     * 业务对
     */
    private String quote;

    private String marketOne;

    private String marketTwo;

    /**
     * 差异率
     */
    private BigDecimal rateValue;

    /**
     * 挂单总金额(usdt)
     */
    private BigDecimal totalAmount;


}
