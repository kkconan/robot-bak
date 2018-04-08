package com.money.game.robot.biz;

import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.dto.zb.BaseZbDto;
import com.money.game.robot.entity.AccountEntity;
import com.money.game.robot.huobi.api.ApiClient;
import com.money.game.robot.huobi.response.*;
import com.money.game.robot.service.AccountService;
import com.money.game.robot.zb.api.ZbApi;
import com.money.game.robot.zb.vo.ZbAccountDetailVo;
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
    private AccountService accountService;

    @Autowired
    private ZbApi zbApi;


    /**
     * 根据用户id和账号类型获取账号信息
     */
    public AccountEntity getByUserIdAndType(String userId, String type) {
        return accountService.findByUserIdAndType(userId, type);
    }

    public List<AccountEntity> findByType(String type) {
        List<AccountEntity> list = accountService.findByTypeAndStatus(type, DictEnum.USER_STATUS_NORMAL.getCode());
        for (AccountEntity accountEntity : list) {
            if (StringUtil.isEmpty(accountEntity.getAccountId()) && DictEnum.MARKET_TYPE_HB.getCode().equals(type)) {
                HuobiBaseDto dto = new HuobiBaseDto();
                dto.setApiKey(accountEntity.getApiKey());
                dto.setApiSecret(accountEntity.getApiSecret());
                Accounts accounts = this.getHuobiSpotAccounts(dto);
                if (accounts != null) {
                    accountEntity.setAccountId(String.valueOf(accounts.getId()));
                    accountService.save(accountEntity);
                }
            }
        }
        return list;
    }

    /**
     * 获取火币账号信息
     */
    public Accounts getHuobiSpotAccounts(HuobiBaseDto dto) {

        setHuobiApiKey(dto);

        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        AccountsResponse<List<Accounts>> accounts = client.accounts();
        List<Accounts> list = accounts.getData();
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 获取火币账号所有余额
     */
    public List<BalanceBean> getHuobiAccountBalance(HuobiBaseDto dto, Accounts accounts) {
        setHuobiApiKey(dto);
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        BalanceResponse<Balance<List<BalanceBean>>> response = client.balance(String.valueOf(accounts.getId()));
        Balance<List<BalanceBean>> balance = response.getData();
        return balance.getList();
    }


    /**
     * 获取hb当前币种最大余额
     */
    public BigDecimal getHuobiQuoteBalance(String userId, String quote) {
        BigDecimal maxBalance = BigDecimal.ZERO;
        HuobiBaseDto dto = new HuobiBaseDto();
        dto.setUserId(userId);
        setHuobiApiKey(dto);
        Accounts accounts = this.getHuobiSpotAccounts(dto);
        List<BalanceBean> balanceBeanList = this.getHuobiAccountBalance(dto, accounts);
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


    /**
     * 获取zb当前币种最大余额
     */
    public BigDecimal getZbBalance(String userId, String quote) {
        BigDecimal maxBalance = BigDecimal.ZERO;
        BaseZbDto baseZbDto = new BaseZbDto();
        this.setZbApiKey(baseZbDto, userId);
        List<ZbAccountDetailVo> zbAccountDetailVoList = zbApi.getAccountInfo(baseZbDto);
        for (ZbAccountDetailVo vo : zbAccountDetailVoList) {
            //获取当前quote余额
            if (quote.equals(vo.getKey())) {
                maxBalance = vo.getAvailable();
                break;
            }
        }
        log.info("maxBalance={}", maxBalance);
        return maxBalance;
    }

    /**
     * 设置火币账号api信息
     */
    public HuobiBaseDto setHuobiApiKey(HuobiBaseDto dto) {
        if (StringUtil.isEmpty(dto.getApiKey())) {
            AccountEntity accountEntity = accountService.findByUserIdAndType(dto.getUserId(), DictEnum.MARKET_TYPE_HB.getCode());
            dto.setApiKey(accountEntity.getApiKey());
            dto.setApiSecret(accountEntity.getApiSecret());
        }
        return dto;
    }

    public BaseZbDto setZbApiKey(BaseZbDto dto, String userId) {
        if (StringUtil.isEmpty(dto.getAccessKey())) {
            AccountEntity accountEntity = accountService.findByUserIdAndType(userId, DictEnum.MARKET_TYPE_ZB.getCode());
            dto.setAccessKey(accountEntity.getApiKey());
            dto.setSecretKey(accountEntity.getApiSecret());
        }
        return dto;
    }
}

