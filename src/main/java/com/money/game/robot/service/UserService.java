package com.money.game.robot.service;

import com.money.game.robot.entity.UserEntity;

import java.util.List;

/**
 * @author conan
 *         2018/3/21 17:23
 **/
public interface UserService {

    List<UserEntity> findAllByStatus(String status);

    UserEntity save(UserEntity userEntity);

    UserEntity findOne(String id);

    UserEntity findByPhone(String phone);

}
