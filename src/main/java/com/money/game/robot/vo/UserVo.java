package com.money.game.robot.vo;

import lombok.Data;

/**
 * @author conan
 *         2018/3/28 13:27
 **/
@Data
public class UserVo {

    private String apiKey;

    private String apiSecret;
    /**
     * normal freeze
     */
    private String status;

    private String phone;

    /**
     * 通知手机号,可多个,通过逗号分隔
     */
    private String notifyPhone;

    /**
     * 通知邮箱,可多个,通过逗号分隔
     */
    private String notifyEmail;
}
