package com.money.game.robot.biz;

import com.money.game.core.util.StringUtil;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.huobi.api.ApiClient;
import com.money.game.robot.huobi.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author conan
 *         2018/3/14 10:54
 **/
@Slf4j
@Component
public class AccountBiz {

    @Autowired
    private UserBiz userBiz;

    public Accounts getSpotAccounts(HuobiBaseDto dto) {

        setApiKey(dto);

        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        AccountsResponse<List<Accounts>> accounts = client.accounts();
        List<Accounts> list = accounts.getData();
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public List<BalanceBean> getAccountBalance(HuobiBaseDto dto, Accounts accounts) {
        setApiKey(dto);
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        BalanceResponse<Balance<List<BalanceBean>>> response = client.balance(String.valueOf(accounts.getId()));
        Balance<List<BalanceBean>> balance = response.getData();
        return balance.getList();
    }


    public BigDecimal getQuoteBalance(String userId, String quote) {
        BigDecimal maxBalance = BigDecimal.ZERO;
        HuobiBaseDto dto = new HuobiBaseDto();
        dto.setUserId(userId);
        setApiKey(dto);
        Accounts accounts = this.getSpotAccounts(dto);
        List<BalanceBean> balanceBeanList = this.getAccountBalance(dto, accounts);
        for (BalanceBean balanceBean : balanceBeanList) {
            //获取当前quote余额
            if (quote.equals(balanceBean.getCurrency()) && "trade".equals(balanceBean.getType())) {
                maxBalance = new BigDecimal(balanceBean.getBalance());
                break;
            }
        }
        log.info("maxBalance={}", maxBalance);
        return maxBalance;
    }

    public HuobiBaseDto setApiKey(HuobiBaseDto dto) {
        if (StringUtil.isEmpty(dto.getApiKey())) {
            UserEntity userEntity = userBiz.findById(dto.getUserId());
            dto.setApiKey(userEntity.getApiKey());
            dto.setApiSecret(userEntity.getApiSecret());
        }
        return dto;
    }
}

