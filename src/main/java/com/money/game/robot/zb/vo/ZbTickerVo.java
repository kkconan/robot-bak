package com.money.game.robot.zb.vo;

import lombok.Data;

/**
 * @author conan
 *         2018/4/3 17:57
 **/
@Data
public class ZbTickerVo {
    /**
     *  交易时间(时间戳)
     */
    private long date;
//    price : 交易价格
//    amount : 交易数量
//    tid : 交易生成ID
//    type : 交易类型，buy(买)/sell(卖)
//    trade_type : 委托类型，ask(卖)/bid(买)
}
