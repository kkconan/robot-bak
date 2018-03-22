package com.money.game.robot.biz;

import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.huobi.api.ApiClient;
import com.money.game.robot.huobi.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author conan
 *         2018/3/14 10:54
 **/
@Slf4j
@Component
public class AccountBiz {

    public Accounts getSpotAccounts(HuobiBaseDto dto) {
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        AccountsResponse<List<Accounts>> accounts = client.accounts();
        List<Accounts> list = accounts.getData();
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public List<BalanceBean> getAccountBalance(HuobiBaseDto dto, Accounts accounts) {
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        BalanceResponse<Balance<List<BalanceBean>>> response = client.balance(String.valueOf(accounts.getId()));
        Balance<List<BalanceBean>> balance = response.getData();
        return balance.getList();
    }
}
