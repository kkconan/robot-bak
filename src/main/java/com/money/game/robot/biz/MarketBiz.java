package com.money.game.robot.biz;

import com.money.game.core.util.StringUtil;
import com.money.game.robot.dto.huobi.DepthDto;
import com.money.game.robot.entity.UserEntity;
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
    private UserBiz userBiz;

    /**
     * 获取交易对限价单信息
     */
    public Depth depth(DepthDto dto) {
        if (StringUtil.isEmpty(dto.getApiKey())) {
            UserEntity userEntity = userBiz.findById(dto.getUserId());
            dto.setApiKey(userEntity.getApiKey());
            dto.setApiSecret(userEntity.getApiSecret());
        }
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        DepthRequest depthRequest = new DepthRequest();
        depthRequest.setSymbol(dto.getSymbol());
        depthRequest.setType(dto.getType());
        DepthResponse<Depth> response = client.depth(depthRequest);
        return response.getTick();
    }
}
