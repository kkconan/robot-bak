package com.money.game.robot.service.impl;

import com.money.game.robot.dao.AccountDao;
import com.money.game.robot.entity.AccountEntity;
import com.money.game.robot.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author conan
 *         2018/3/30 15:14
 **/
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao accountDao;

    @Override
    public AccountEntity findByUserIdAndType(String userId, String type) {
        return accountDao.findByUserIdAndType(userId, type);
    }

    @Override
    public List<AccountEntity> findByTypeAndStatus(String type, String status) {
        return accountDao.findByTypeAndStatus(type,status);
    }
}
