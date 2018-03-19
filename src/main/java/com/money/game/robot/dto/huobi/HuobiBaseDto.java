package com.money.game.robot.dto.huobi;

import com.money.game.robot.dto.BaseDto;
import lombok.Data;

import java.io.Serializable;

/**
 * @author conan
 *         2018/3/14 16:43
 **/
@Data
public class HuobiBaseDto extends BaseDto implements Serializable{

    private String apiKey;

    private String apiSecret;
    ;
    private String orderId;

}
