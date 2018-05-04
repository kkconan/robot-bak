package com.money.game.robot.dto.huobi;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ma趋势数据
 * @author conan
 *         2018/5/3 18:08
 **/

@Data
public class MaInfoDto {

    /**
     * 最新时间的ma平均数
     */
    BigDecimal oneMiddle = BigDecimal.ZERO;

    BigDecimal twoMiddle = BigDecimal.ZERO;

    BigDecimal threeMiddle = BigDecimal.ZERO;

    BigDecimal fourMiddle = BigDecimal.ZERO;
}
