package com.money.game.robot.zb.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author conan
 *         2018/3/30 10:32
 **/
@Data
public class ZbOrderDetailVo {
    /**
     * 交易类型
     */
    private String currency;

    /**
     * 委托挂单号
     */
    private String id;
    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 挂单状态(1：取消,2：交易完成,0/3：待成交/待成交未交易部份)
     */
    @com.google.gson.annotations.SerializedName("state")
    private String state;

    /**
     * 挂单总数量
     */
    @com.google.gson.annotations.SerializedName("total_amount")
    private BigDecimal totalAmount;

    /**
     * 已成交数量
     */
    @com.google.gson.annotations.SerializedName("trade_amount")
    private BigDecimal fieldAmount;

    /**
     * 委托时间
     */
    @com.google.gson.annotations.SerializedName("trade_date")
    private Long tradeDate;

    /**
     * 已成交总金额
     */
    @com.google.gson.annotations.SerializedName("trade_money")
    private BigDecimal fieldCashAmount;

    /**
     * 挂单类型 1/0[buy/sell]
     */
    private String type;
}
