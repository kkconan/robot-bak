package com.money.game.robot.huobi.response;

import lombok.Data;

/**
 * @Author ISME
 * @Date 2018/1/14
 * @Time 18:22
 */
@Data
public class OrdersDetail {

    /**
     * id : 59378
     * symbol : ethusdt
     * account-id : 100009
     * amount : 10.1000000000
     * price : 100.1000000000
     * created-at : 1494901162595
     * type : buy-limit
     * field-amount : 10.1000000000
     * field-cash-amount : 1011.0100000000
     * field-fees : 0.0202000000
     * finished-at : 1494901400468
     * user-id : 1000
     * source : api
     * state : filled
     * canceled-at : 0
     * exchange : huobi
     * batch :
     */

    private Long id;
    private String symbol;
    private int accountid;
    private String amount;
    private String price;
    private long createdat;
    private String type;
    private String fieldamount;
    private String fieldcashamount;
    private String fieldfees;
    private long finishedat;
    private int userid;
    private String source;
    private String state;
    private int canceledat;
    private String exchange;
    private String batch;
}
