package com.money.game.robot.biz;

import com.money.game.core.util.StringUtil;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.huobi.response.Accounts;
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

    @Autowired
    private AccountBiz accountBiz;

    public List<UserEntity> findAll() {
        List<UserEntity> list = userService.findAll();
        for (UserEntity userEntity : list) {
            if (StringUtil.isEmpty(userEntity.getAccountId())) {
                HuobiBaseDto dto = new HuobiBaseDto();
                dto.setApiKey(userEntity.getApiKey());
                dto.setApiSecret(userEntity.getApiSecret());
                Accounts accounts = accountBiz.getSpotAccounts(dto);
                if (accounts != null) {
                    userEntity.setAccountId(String.valueOf(accounts.getId()));
                    userService.save(userEntity);
                }
            }
        }
        return list;
    }

    public UserEntity findById(String userId) {
        return userService.findOne(userId);
    }
}
