package com.money.game.robot.biz;

import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.huobi.*;
import com.money.game.robot.huobi.api.ApiClient;
import com.money.game.robot.huobi.request.CreateOrderRequest;
import com.money.game.robot.huobi.response.*;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * create order
     */

    public String createOrder(CreateOrderDto dto) {
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        CreateOrderRequest createOrderReq = new CreateOrderRequest();
        createOrderReq.setAccountId(dto.getAccountId());
        createOrderReq.setAmount(dto.getAmount().setScale(2, BigDecimal.ROUND_DOWN).toString());
        createOrderReq.setPrice(dto.getPrice().setScale(4, BigDecimal.ROUND_DOWN).toString());
        if (dto.getSymbol().endsWith(DictEnum.MARKET_BASE_BTC.getCode()) || dto.getSymbol().endsWith(DictEnum.MARKET_BASE_ETH.getCode())) {
            createOrderReq.setPrice(dto.getPrice().setScale(8, BigDecimal.ROUND_DOWN).toString());
            createOrderReq.setPrice(dto.getPrice().setScale(8, BigDecimal.ROUND_DOWN).toString());
        }
        createOrderReq.setSymbol(dto.getSymbol());
        createOrderReq.setType(dto.getOrderType());
        createOrderReq.setSource("api");
        Long orderId = client.createOrder(createOrderReq);
        log.info("createOrder,dto={},createOrderReq={},orderId={}", dto, createOrderReq, orderId);
        return client.placeOrder(orderId);
    }

    /**
     * cancel order
     */
    public void submitCancel(HuobiBaseDto dto) {
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        SubmitcancelResponse response = client.submitcancel(dto.getOrderId());
        if (!"ok".equals(response.getStatus())) {
            log.error("cancel order fail,orderId={}", dto.getOrderId());
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
    public OrdersDetail orderDetail(HuobiBaseDto dto) {
        ApiClient client = new ApiClient(dto.getApiKey(), dto.getApiSecret());
        OrdersDetailResponse<OrdersDetail> response = client.ordersDetail(dto.getOrderId());
        return response.getData();
    }


    /**
     * 订单成交明细
     */
    public MatchresultsOrdersDetail matchresults(HuobiBaseDto dto) {
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
}
