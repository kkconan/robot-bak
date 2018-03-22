package com.money.game.robot.biz;

import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author conan
 *         2018/3/22 10:15
 **/
@Component
public class UserBiz {

    @Autowired
    private UserService userService;

    public List<UserEntity> findAll() {
        return userService.findAll();
    }
}
