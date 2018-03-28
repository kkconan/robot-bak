package com.money.game.robot.dto.client;

import lombok.Data;

/**
 * @author conan
 *         2018/3/28 13:27
 **/
@Data
public class ModifyUserInfoDto {

    private String apiKey;

    private String apiSecret;

    private String phone;

    private String password;

    /**
     * 通知手机号,可多个,通过逗号分隔
     */
    private String notifyPhone;

    /**
     * 通知邮箱,可多个,通过逗号分隔
     */
    private String notifyEmail;
}
