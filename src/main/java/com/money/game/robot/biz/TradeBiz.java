package com.money.game.robot.biz;

import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.constant.ErrorEnum;
import com.money.game.robot.dto.huobi.BatchCancelDto;
import com.money.game.robot.dto.huobi.CreateOrderDto;
import com.money.game.robot.dto.huobi.HuobiBaseDto;
import com.money.game.robot.dto.huobi.IntrustOrderDto;
import com.money.game.robot.dto.zb.ZbCreateOrderDto;
import com.money.game.robot.entity.AccountEntity;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.exception.BizException;
import com.money.game.robot.huobi.api.ApiClient;
import com.money.game.robot.huobi.api.ApiException;
import com.money.game.robot.huobi.request.CreateOrderRequest;
import com.money.game.robot.huobi.response.*;
import com.money.game.robot.zb.api.ZbApi;
import com.money.game.robot.zb.vo.ZbCreateOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author conan
 *         2018/3/14 11:25
 **/
@Slf4j
@Component
public class TradeBiz {

    @Autowired
    private UserBiz userBiz;

    @Autowired
    private AccountBiz accountBiz;

    @Autowired
    private ZbApi zbApi;

    /**
     * create hb order
     */

    public String createHbOrder(CreateOrderDto dto) {
        if (StringUtil.isEmpty(dto.getApiKey())) {
            UserEntity userEntity = userBiz.findById(dto.getUserId());
            AccountEntity accountEntity = accountBiz.getByUserIdAndType(dto.getUserId(), DictEnum.MARKET_TYPE_HB.getCode());
            dto.setApiKey(accountEntity.getApiKey());
            dto.setApiSecret(accountEntity.getApiSecret());
        }
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        CreateOrderRequest createOrderReq = new CreateOrderRequest();
        createOrderReq.setAccountId(dto.getAccountId());
        createOrderReq.setAmount(dto.getAmount().setScale(2, BigDecimal.ROUND_DOWN).toString());
        createOrderReq.setPrice(dto.getPrice().setScale(4, BigDecimal.ROUND_DOWN).toString());
        if (dto.getSymbol().endsWith(DictEnum.HB_MARKET_BASE_BTC.getCode()) || dto.getSymbol().endsWith(DictEnum.HB_MARKET_BASE_ETH.getCode())) {
            createOrderReq.setPrice(dto.getPrice().setScale(8, BigDecimal.ROUND_DOWN).toString());
            createOrderReq.setPrice(dto.getPrice().setScale(8, BigDecimal.ROUND_DOWN).toString());
        }
        createOrderReq.setSymbol(dto.getSymbol());
        createOrderReq.setType(dto.getOrderType());
        createOrderReq.setSource("api");
        Long orderId;
        try {
            orderId = client.createOrder(createOrderReq);
        } catch (ApiException e) {
            log.info("dto={},errMsg={}", dto, e.getMessage());
            Integer scale = 8;
            //order price precision error, scale: `2`
            String[] message = e.getMessage().split("scale:");
            if (message.length >= 2) {
                scale = Integer.valueOf(message[1].replaceAll("`", "").trim());
            }
            if (e.getMessage().contains("price")) {
                createOrderReq.setPrice(dto.getPrice().setScale(scale, BigDecimal.ROUND_DOWN).toString());
            }
            if (e.getMessage().contains("amount")) {
                createOrderReq.setAmount(dto.getAmount().setScale(scale, BigDecimal.ROUND_DOWN).toString());
            }
            orderId = client.createOrder(createOrderReq);
        }
        log.info("createHbOrder,dto={},createOrderReq={},orderId={}", dto, createOrderReq, orderId);
        return client.placeOrder(orderId);
    }

    /**
     * cancel order
     */
    public void submitCancel(HuobiBaseDto dto) {
        accountBiz.setHuobiApiKey(dto);
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        SubmitcancelResponse response = client.submitcancel(dto.getOrderId());
        if (!"ok".equals(response.getStatus())) {
            log.info("撤销订单失败,orderId={},errCode={},errmsg={},state={}", dto.getOrderId(), response.getErrCode(), response.getErrMsg(), response.getStatus());
            throw new BizException(response.getErrCode(), response.getErrMsg());
        }
    }

    /**
     * batch cancel order
     */
    public Batchcancel<List, BatchcancelBean> batchCancel(BatchCancelDto dto) {
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        BatchcancelResponse<Batchcancel<List, BatchcancelBean>> response = client.submitcancels(dto.getOrderIds());
        return response.getData();
    }

    /**
     * order detail
     */
    public OrdersDetail getHbOrderDetail(HuobiBaseDto dto) {
        accountBiz.setHuobiApiKey(dto);
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        OrdersDetailResponse<OrdersDetail> response = client.ordersDetail(dto.getOrderId());
        if (!"ok".equals(response.getStatus())) {
            log.info("获取订单详情失败,orderId={},response={}", dto.getOrderId(), response);
            return null;
        }
        return response.getData();
    }


    /**
     * 订单成交明细
     */
    public MatchresultsOrdersDetail hbMatchresults(HuobiBaseDto dto) {
        if (StringUtil.isEmpty(dto.getApiKey())) {
            AccountEntity accountEntity = accountBiz.getByUserIdAndType(dto.getUserId(), DictEnum.MARKET_TYPE_HB.getCode());
            dto.setApiKey(accountEntity.getApiKey());
            dto.setApiSecret(accountEntity.getApiSecret());
        }
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        MatchresultsOrdersDetailResponse<MatchresultsOrdersDetail> response = client.matchresults(dto.getOrderId());
        return response.getData();
    }


    /**
     * 当前委托、历史委托
     */
    public List<IntrustDetail> intrustOrdersDetail(IntrustOrderDto dto) {
        log.info("intrustOrdersDetail,dto={}", dto);
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        Map<String, String> map = new HashMap<>();
        map.put("symbol", dto.getSymbol());
        map.put("states", dto.getStates());
        map.put("types", dto.getTypes());
        map.put("direct", dto.getDirect());
        map.put("startDate", dto.getStartDate());
        map.put("endDate", dto.getEndDate());
        map.put("from", dto.getFrom());
        map.put("size", dto.getSize());
        IntrustDetailResponse<List<IntrustDetail>> response = client.intrustOrdersDetail(map);
        return response.getData();
    }

    /**
     * 创建zb订单
     */
    public String zbCreateOrder(String symbol, BigDecimal price, BigDecimal amount, String tradeType, String userId) {
        AccountEntity account = accountBiz.getByUserIdAndType(userId, DictEnum.MARKET_TYPE_ZB.getCode());
        //todo 精度放到redis里面，下单获取
        if (StringUtil.isEmpty(account.getApiKey()) || StringUtil.isEmpty(account.getApiSecret())) {
            throw new BizException(ErrorEnum.USER_API_NOT_FOUND);
        }
        ZbCreateOrderDto dto = new ZbCreateOrderDto();
        dto.setAccessKey(account.getApiKey());
        dto.setSecretKey(account.getApiSecret());
        dto.setCurrency(symbol);
        dto.setPrice(price.toString());
        dto.setAmount(amount.toString());
        dto.setTradeType(tradeType);
        ZbCreateOrderVo vo = zbApi.createOrder(dto);
        if (!"1000".equals(vo.getCode())) {
            log.error("订单创建失败,vo={}", vo);
            throw new BizException(ErrorEnum.CREATE_ORDER_FAIL);
        }
        return vo.getId();
    }


}
