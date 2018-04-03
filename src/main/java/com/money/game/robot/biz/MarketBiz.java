package com.money.game.robot.biz;

import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.DepthDto;
import com.money.game.robot.entity.AccountEntity;
import com.money.game.robot.huobi.api.ApiClient;
import com.money.game.robot.huobi.request.DepthRequest;
import com.money.game.robot.huobi.response.Depth;
import com.money.game.robot.huobi.response.DepthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author conan
 *         2018/3/15 15:44
 **/
@Slf4j
@Component
public class MarketBiz {

    @Autowired
    private AccountBiz accountBiz;

    /**
     * 获取HB交易对限价单信息
     */
    public Depth HbDepth(DepthDto dto) {
        if (StringUtil.isEmpty(dto.getApiKey())) {
            AccountEntity accountEntity = accountBiz.getByUserIdAndType(dto.getUserId(), DictEnum.MARKET_TYPE_HB.getCode());
            dto.setApiKey(accountEntity.getApiKey());
            dto.setApiSecret(accountEntity.getApiSecret());
        }
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        DepthRequest depthRequest = new DepthRequest();
        depthRequest.setSymbol(dto.getSymbol());
        depthRequest.setType(dto.getType());
        DepthResponse<Depth> response = client.depth(depthRequest);
        return response.getTick();
    }
}
